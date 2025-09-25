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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yami.shop.bean.app.param.OrderRefundExpressParam;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Date;
import java.util.Objects;

/**
 * 退款服务实现
 * @author yami
 */
@Service
public class RefundServiceImpl implements RefundService {

    private static final Logger logger = LoggerFactory.getLogger(RefundServiceImpl.class);

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

        // 2. 验证订单状态（确认收货后或已完成的订单可以申请退款）
        if (!OrderStatus.CONFIRM.value().equals(order.getStatus()) &&
            !OrderStatus.SUCCESS.value().equals(order.getStatus())) {
            throw new YamiShopBindException("订单状态不允许申请退款，请先确认收货");
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

        // 5. 查询支付信息获取支付流水号
        OrderSettlement settlement = orderSettlementMapper.getSettlementByOrderNumber(param.getOrderNumber());
        String flowTradeNo = "";
        Integer payType = 1; // 默认微信支付
        if (settlement != null) {
            flowTradeNo = settlement.getPayNo();
            payType = settlement.getPayType();
        }

        // 6. 创建退款记录
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
        refund.setFlowTradeNo(flowTradeNo); // 设置支付流水号
        refund.setPayType(payType); // 设置支付方式

        orderRefundMapper.insert(refund);
        return refund.getRefundId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditRefund(RefundAuditParam param) {
        logger.info("开始审核退款申请，退款ID: {}, 审核结果: {}", param.getRefundId(), param.getAuditResult());

        OrderRefund refund = orderRefundMapper.selectById(param.getRefundId());
        if (refund == null) {
            logger.error("退款申请不存在，退款ID: {}", param.getRefundId());
            throw new YamiShopBindException("退款申请不存在");
        }

        if (!refund.getRefundSts().equals(1)) {
            logger.error("退款申请状态不正确，当前状态: {}, 期望状态: 1", refund.getRefundSts());
            throw new YamiShopBindException("退款申请状态不正确");
        }

        // 更新审核结果
        refund.setRefundSts(param.getAuditResult());
        refund.setSellerMsg(param.getSellerMsg());
        refund.setHandelTime(new Date());

        orderRefundMapper.updateById(refund);

        if (param.getAuditResult().equals(2)) {
            if (Objects.equals(refund.getApplyType(), 2)) {
                logger.info("退款审核通过，等待用户填写退货物流信息，退款ID: {}", refund.getRefundId());
            } else {
                logger.info("退款审核通过，准备在事务提交后执行退款流程，订单号: {}, 退款金额: {}", refund.getOrderNumber(), refund.getRefundAmount());
                final Long refundId = refund.getRefundId();
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        OrderRefund latest = orderRefundMapper.selectById(refundId);
                        if (latest != null) {
                            executeRefund(latest);
                        }
                    }
                });
            }
        } else {
            logger.info("退款审核被拒绝，订单号: {}, 拒绝原因: {}", refund.getOrderNumber(), param.getSellerMsg());
        }

        logger.info("退款审核完成，退款ID: {}", param.getRefundId());
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
    public IPage<OrderRefund> getMerchantRefundList(Long shopId, Long current, Long size, Integer status) {
        Page<OrderRefund> page = new Page<>(current, size);

        LambdaQueryWrapper<OrderRefund> wrapper = new LambdaQueryWrapper<OrderRefund>()
            .eq(shopId != null, OrderRefund::getShopId, shopId)
            .eq(status != null, OrderRefund::getRefundSts, status)
            .orderByDesc(OrderRefund::getApplyTime);

        return orderRefundMapper.selectPage(page, wrapper);
    }

    @Override
    public OrderRefund getRefundById(Long refundId) {
        return orderRefundMapper.selectById(refundId);
    }

    @Override
    public void submitRefundExpress(String userId, OrderRefundExpressParam param) {
        logger.info("用户提交退货退款物流信息，refundSn: {}", param.getRefundSn());

        OrderRefund refund = orderRefundMapper.selectOne(
            new LambdaQueryWrapper<OrderRefund>().eq(OrderRefund::getRefundSn, param.getRefundSn())
        );

        if (refund == null || !Objects.equals(refund.getUserId(), userId)) {
            throw new YamiShopBindException("退款记录不存在");
        }

        if (!Objects.equals(refund.getApplyType(), 2)) {
            throw new YamiShopBindException("当前退款类型无需填写物流信息");
        }

        if (!Objects.equals(refund.getRefundSts(), 2)) {
            throw new YamiShopBindException("当前退款状态不支持填写物流信息");
        }

        if (StrUtil.isNotBlank(refund.getExpressNo())) {
            throw new YamiShopBindException("已提交物流信息，无需重复提交");
        }

        if (!Objects.equals(refund.getReturnMoneySts(), 0)) {
            throw new YamiShopBindException("退款状态已更新，无需填写物流信息");
        }

        refund.setExpressName(param.getExpressName());
        refund.setExpressNo(param.getExpressNo());
        refund.setShipTime(new Date());

        orderRefundMapper.updateById(refund);

        logger.info("退货退款物流信息提交完成，准备执行退款流程，退款ID: {}", refund.getRefundId());
        executeRefund(refund);
    }

    /**
     * 执行退款
     */
    public void executeRefund(OrderRefund refund) {
        logger.info("=== 开始执行退款流程 ===");
        logger.info("退款ID: {}, 订单号: {}, 退款金额: {}", refund.getRefundId(), refund.getOrderNumber(), refund.getRefundAmount());

        if (Objects.equals(refund.getReturnMoneySts(), 1)) {
            logger.info("退款已成功，无需重复执行，退款ID: {}", refund.getRefundId());
            return;
        }
        if (Objects.equals(refund.getReturnMoneySts(), -1)) {
            logger.warn("退款标记为失败，准备重新执行退款流程，退款ID: {}", refund.getRefundId());
        }

        try {
            // 查询支付信息
            OrderSettlement settlement = orderSettlementMapper.getSettlementByOrderNumber(refund.getOrderNumber());
            if (settlement == null) {
                logger.error("未找到支付记录，订单号: {}", refund.getOrderNumber());
                updateRefundStatus(refund.getRefundId(), -1, "未找到支付记录");
                return;
            }

            logger.info("找到支付记录，支付单号: {}, 支付类型: {}", settlement.getPayNo(), settlement.getPayType());

            // 设置支付信息
            refund.setFlowTradeNo(settlement.getPayNo());
            refund.setPayType(settlement.getPayType());

            // 调用支付宝退款
            if (settlement.getPayType() == 2 || settlement.getPayType() == 6 || settlement.getPayType() == 7) { // 支付宝、支付宝H5或当面付
                logger.info("调用支付宝退款接口，支付单号: {}, 退款金额: {}, 退款原因: {}",
                    settlement.getPayNo(), refund.getRefundAmount(), refund.getBuyerMsg());

                // 查询订单总金额，判断是否为全额退款
                Order order = orderMapper.getOrderByOrderNumber(refund.getOrderNumber());
                boolean isFullRefund = order != null && refund.getRefundAmount().equals(order.getActualTotal());

                String outRequestNo = null;
                if (!isFullRefund) {
                    // 部分退款必须传退款请求号
                    outRequestNo = refund.getRefundSn(); // 使用退款单号作为退款请求号
                    logger.info("部分退款，使用退款请求号: {}", outRequestNo);
                } else {
                    logger.info("全额退款，不传退款请求号");
                }

                boolean success = alipayService.refund(settlement.getPayNo(), refund.getRefundAmount(), refund.getBuyerMsg(), outRequestNo);

                if (success) {
                    logger.info("支付宝退款成功，退款ID: {}", refund.getRefundId());
                    updateRefundStatus(refund.getRefundId(), 1, "退款成功");
                    refund.setReturnMoneySts(1);
                } else {
                    logger.error("支付宝退款失败，退款ID: {}", refund.getRefundId());
                    updateRefundStatus(refund.getRefundId(), -1, "支付宝退款失败");
                    refund.setReturnMoneySts(-1);
                }
            } else {
                // 其他支付方式模拟退款成功（如微信支付）
                logger.info("其他支付方式模拟退款成功，支付类型: {}", settlement.getPayType());
                updateRefundStatus(refund.getRefundId(), 1, "退款成功");
                refund.setReturnMoneySts(1);
            }

        } catch (Exception e) {
            logger.error("退款流程异常，退款ID: {}, 异常信息: {}", refund.getRefundId(), e.getMessage(), e);
            updateRefundStatus(refund.getRefundId(), -1, "退款异常: " + e.getMessage());
            refund.setReturnMoneySts(-1);
        }

        logger.info("=== 退款流程结束 ===");
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

    @Override
    public Integer getRefundStatusByOrderNumber(String orderNumber) {
        try {
            logger.info("查询订单退款状态, orderNumber: {}", orderNumber);

            if (orderNumber == null || orderNumber.trim().isEmpty()) {
                logger.warn("订单号为空");
                return 0;
            }

            LambdaQueryWrapper<OrderRefund> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(OrderRefund::getOrderNumber, orderNumber);
            wrapper.orderByDesc(OrderRefund::getApplyTime);
            wrapper.last("LIMIT 1");

            OrderRefund refund = orderRefundMapper.selectOne(wrapper);
            if (refund == null) {
                logger.info("订单无退款记录, orderNumber: {}", orderNumber);
                return 0; // 未退款
            }

            // 正确的状态判断逻辑
            Integer refundSts = refund.getRefundSts();
            Integer returnMoneySts = refund.getReturnMoneySts();
            logger.info("订单退款状态, orderNumber: {}, refundSts: {}, returnMoneySts: {}",
                       orderNumber, refundSts, returnMoneySts);

            if (refundSts == null) {
                return 0; // 未退款
            }

            // 正确的状态映射逻辑
            switch (refundSts) {
                case 1: // 待审核
                    return 1; // 申请退款中
                case 2: // 已同意
                    if (returnMoneySts == null || returnMoneySts == 0) {
                        return 1; // 退款处理中，显示为申请中
                    } else if (returnMoneySts == 1) {
                        return 2; // 退款成功
                    } else if (returnMoneySts == -1) {
                        return 3; // 退款失败
                    } else {
                        return 1; // 默认处理中
                    }
                case 3: // 已拒绝
                    return 3; // 退款失败
                default:
                    return 1; // 默认申请退款中
            }

        } catch (Exception e) {
            logger.error("查询订单退款状态异常, orderNumber: {}", orderNumber, e);
            return 0; // 异常时返回未退款
        }
    }
}
