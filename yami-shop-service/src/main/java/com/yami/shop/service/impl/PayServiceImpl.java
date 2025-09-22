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
import com.yami.shop.bean.enums.PayType;
import com.yami.shop.bean.event.PaySuccessOrderEvent;
import com.yami.shop.bean.model.Order;
import com.yami.shop.bean.model.OrderSettlement;
import com.yami.shop.bean.app.param.PayParam;
import com.yami.shop.bean.pay.PayInfoDto;
import com.yami.shop.common.exception.YamiShopBindException;
import com.yami.shop.common.util.Arith;
import com.yami.shop.dao.OrderMapper;
import com.yami.shop.dao.OrderSettlementMapper;
import com.yami.shop.service.AlipayService;
import com.yami.shop.service.PayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author lgh on 2018/09/15.
 */
@Service
public class PayServiceImpl implements PayService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderSettlementMapper orderSettlementMapper;

    @Autowired
    private AlipayService alipayService;


    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private Snowflake snowflake;

    /**
     * 不同的订单号，同一个支付流水号
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PayInfoDto pay(String userId, PayParam payParam) {


        // 不同的订单号的产品名称
        StringBuilder prodName = new StringBuilder();
        // 支付单号
        String payNo = String.valueOf(snowflake.nextId());
        String[] orderNumbers = payParam.getOrderNumbers().split(StrUtil.COMMA);
        // 修改订单信息
        for (String orderNumber : orderNumbers) {
            OrderSettlement orderSettlement = new OrderSettlement();
            orderSettlement.setPayNo(payNo);
            orderSettlement.setPayType(payParam.getPayType());
            orderSettlement.setUserId(userId);
            orderSettlement.setOrderNumber(orderNumber);
            orderSettlementMapper.updateByOrderNumberAndUserId(orderSettlement);

            Order order = orderMapper.getOrderByOrderNumber(orderNumber);
            prodName.append(order.getProdName()).append(StrUtil.COMMA);
        }
        // 除了ordernumber不一样，其他都一样
        List<OrderSettlement> settlements = orderSettlementMapper.getSettlementsByPayNo(payNo);
        // 应支付的总金额
        double payAmount = 0.0;
        for (OrderSettlement orderSettlement : settlements) {
            payAmount = Arith.add(payAmount, orderSettlement.getPayAmount());
        }

        prodName.substring(0, Math.min(100, prodName.length() - 1));

        PayInfoDto payInfoDto = new PayInfoDto();
        payInfoDto.setBody(prodName.toString());
        payInfoDto.setPayAmount(payAmount);
        payInfoDto.setPayNo(payNo);

        // 根据支付类型处理
        if (payParam.getPayType() == PayType.ALIPAY_H5.value()) {
            String form = alipayService.h5Pay(payInfoDto);
            payInfoDto.setPayForm(form);
        } else if (payParam.getPayType() == PayType.ALIPAY_FACE_TO_FACE.value()) {
            String qrCode = alipayService.faceToFacePay(payInfoDto);
            payInfoDto.setQrCode(qrCode);
        }

        return payInfoDto;
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<String> paySuccess(String payNo, String bizPayNo) {
        List<OrderSettlement> orderSettlements = orderSettlementMapper.selectList(new LambdaQueryWrapper<OrderSettlement>().eq(OrderSettlement::getPayNo, payNo));

        OrderSettlement settlement = orderSettlements.get(0);

        // 订单已支付
        if (settlement.getPayStatus() == 1) {
            throw new YamiShopBindException("订单已支付");
        }
        // 修改订单结算信息
        if (orderSettlementMapper.updateToPay(payNo, settlement.getVersion()) < 1) {
            throw new YamiShopBindException("结算信息已更改");
        }


        List<String> orderNumbers = orderSettlements.stream().map(OrderSettlement::getOrderNumber).collect(Collectors.toList());

        // 将订单改为已支付状态
        orderMapper.updateByToPaySuccess(orderNumbers, settlement.getPayType());

        List<Order> orders = orderNumbers.stream().map(orderNumber -> orderMapper.getOrderByOrderNumber(orderNumber)).collect(Collectors.toList());
        eventPublisher.publishEvent(new PaySuccessOrderEvent(orders));
        return orderNumbers;
    }

    @Override
    public boolean queryPayStatus(String orderNumbers) {
        // 根据订单号查询支付单号
        String[] orderNumberArray = orderNumbers.split(StrUtil.COMMA);
        if (orderNumberArray.length > 0) {
            OrderSettlement settlement = orderSettlementMapper.getSettlementByOrderNumber(orderNumberArray[0]);
            if (settlement != null) {
                // 如果已经支付，直接返回true
                if (settlement.getPayStatus() == 1) {
                    return true;
                }
                // 根据支付类型查询支付状态
                if (settlement.getPayType() == PayType.ALIPAY.value() ||
                    settlement.getPayType() == PayType.ALIPAY_H5.value() ||
                    settlement.getPayType() == PayType.ALIPAY_FACE_TO_FACE.value()) {
                    // 支付宝支付查询
                    return alipayService.queryPayStatus(settlement.getPayNo());
                } else if (settlement.getPayType() == PayType.WECHATPAY.value()) {
                    // 微信支付查询（可扩展）
                    // TODO: 实现微信支付状态查询
                    return false;
                }
            }
        }
        return false;
    }

}
