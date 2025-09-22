/*
 * Copyright (c) 2018-2999 广州市蓝海创新科技有限公司 All rights reserved.
 *
 * https://www.mall4j.com/
 *
 * 未经允许，不可做商业用途！
 *
 * 版权所有，侵权必究！
 */

package com.yami.shop.service.impl;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yami.shop.bean.app.param.RefundApplyParam;
import com.yami.shop.bean.app.param.RefundAuditParam;
import com.yami.shop.bean.enums.OrderStatus;
import com.yami.shop.bean.model.Order;
import com.yami.shop.bean.model.OrderRefund;
import com.yami.shop.bean.model.OrderSettlement;
import com.yami.shop.common.exception.YamiShopBindException;
import com.yami.shop.dao.OrderMapper;
import com.yami.shop.dao.OrderRefundMapper;
import com.yami.shop.dao.OrderSettlementMapper;
import com.yami.shop.service.AlipayService;
import com.yami.shop.service.RefundService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/**
 * 退款服务实现
 * @author yami
 */
@Service
public class RefundServiceImpl implements RefundService {

    @Autowired
    private OrderRefundMapper orderRefundMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderSettlementMapper orderSettlementMapper;

    @Autowired
    private AlipayService alipayService;

    @Autowired
    private Snowflake snowflake;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long applyRefund(String userId, RefundApplyParam param) {
        // 1. 验证订单
        Order order = orderMapper.getOrderByOrderNumber(param.getOrderNumber());
        if (order == null || !order.getUserId().equals(userId)) {
            throw new YamiShopBindException("订单不存在");
        }

        // 2. 验证订单状态（只有已完成的订单可以申请退款）
        if (!OrderStatus.SUCCESS.value().equals(order.getStatus())) {
            throw new YamiShopBindException("订单状态不允许申请退款");
        }

        // 3. 检查是否已有未完成的退款申请
        Long existRefundCount = orderRefundMapper.selectCount(
            new LambdaQueryWrapper<OrderRefund>()
                .eq(OrderRefund::getOrderNumber, param.getOrderNumber())
                .in(OrderRefund::getRefundSts, 1, 2) // 待审核或已同意
        );
        if (existRefundCount > 0) {
            throw new YamiShopBindException("该订单已有进行中的退款申请");
        }

        // 4. 验证退款金额
        if (param.getRefundAmount() <= 0 || param.getRefundAmount() > order.getActualTotal()) {
            throw new YamiShopBindException("退款金额不正确");
        }

        // 5. 创建退款记录
        OrderRefund refund = new OrderRefund();
        refund.setShopId(order.getShopId());
        refund.setOrderId(order.getOrderId());
        refund.setOrderNumber(param.getOrderNumber());
        refund.setOrderAmount(order.getActualTotal());
        refund.setOrderItemId(0L); // 全部退款
        refund.setRefundSn(generateRefundSn());
        refund.setUserId(userId);
        refund.setRefundAmount(param.getRefundAmount());
        refund.setApplyType(param.getApplyType());
        refund.setBuyerMsg(param.getBuyerMsg());
        refund.setPhotoFiles(param.getPhotoFiles());
        refund.setRefundSts(1); // 待审核
        refund.setReturnMoneySts(0); // 退款处理中
        refund.setApplyTime(new Date());

        orderRefundMapper.insert(refund);
        return refund.getRefundId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditRefund(RefundAuditParam param) {
        OrderRefund refund = orderRefundMapper.selectById(param.getRefundId());
        if (refund == null) {
            throw new YamiShopBindException("退款申请不存在");
        }

        if (!refund.getRefundSts().equals(1)) {
            throw new YamiShopBindException("退款申请状态不正确");
        }

        // 更新审核结果
        refund.setRefundSts(param.getAuditResult());
        refund.setSellerMsg(param.getSellerMsg());
        refund.setHandelTime(new Date());

        if (param.getAuditResult().equals(2)) {
            // 同意退款，执行退款流程
            executeRefundAsync(refund);
        }

        orderRefundMapper.updateById(refund);
    }

    @Override
    public IPage<OrderRefund> getUserRefundList(String userId, Long current, Long size) {
        Page<OrderRefund> page = new Page<>(current, size);

        LambdaQueryWrapper<OrderRefund> wrapper = new LambdaQueryWrapper<OrderRefund>()
            .eq(OrderRefund::getUserId, userId)
            .orderByDesc(OrderRefund::getApplyTime);

        return orderRefundMapper.selectPage(page, wrapper);
    }

    @Override
    public IPage<OrderRefund> getMerchantRefundList(Long current, Long size, Integer status) {
        Page<OrderRefund> page = new Page<>(current, size);

        LambdaQueryWrapper<OrderRefund> wrapper = new LambdaQueryWrapper<OrderRefund>()
            .eq(status != null, OrderRefund::getRefundSts, status)
            .orderByDesc(OrderRefund::getApplyTime);

        return orderRefundMapper.selectPage(page, wrapper);
    }

    @Override
    public OrderRefund getRefundById(Long refundId) {
        return orderRefundMapper.selectById(refundId);
    }

    /**
     * 异步执行退款
     */
    @Async
    public void executeRefundAsync(OrderRefund refund) {
        try {
            // 查询支付信息
            OrderSettlement settlement = orderSettlementMapper.getSettlementByOrderNumber(refund.getOrderNumber());
            if (settlement == null) {
                updateRefundStatus(refund.getRefundId(), -1, "未找到支付记录");
                return;
            }

            // 设置支付信息
            refund.setFlowTradeNo(settlement.getPayNo());
            refund.setPayType(settlement.getPayType());

            // 调用支付宝退款
            if (settlement.getPayType() == 2 || settlement.getPayType() == 6 || settlement.getPayType() == 7) { // 支付宝、支付宝H5或当面付
                boolean success = alipayService.refund(settlement.getPayNo(), refund.getRefundAmount(), refund.getBuyerMsg());
                if (success) {
                    updateRefundStatus(refund.getRefundId(), 1, "退款成功");
                } else {
                    updateRefundStatus(refund.getRefundId(), -1, "支付宝退款失败");
                }
            } else {
                // 其他支付方式模拟退款成功（如微信支付）
                updateRefundStatus(refund.getRefundId(), 1, "退款成功");
            }

        } catch (Exception e) {
            updateRefundStatus(refund.getRefundId(), -1, "退款异常: " + e.getMessage());
        }
    }

    /**
     * 更新退款状态
     */
    private void updateRefundStatus(Long refundId, Integer status, String message) {
        OrderRefund refund = new OrderRefund();
        refund.setRefundId(refundId);
        refund.setReturnMoneySts(status);
        if (status == 1) {
            refund.setRefundTime(new Date());
        }
        orderRefundMapper.updateById(refund);
    }

    /**
     * 生成退款单号
     */
    private String generateRefundSn() {
        return "RF" + snowflake.nextId();
    }
}