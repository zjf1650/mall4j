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

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.domain.AlipayTradePrecreateModel;
import com.alipay.api.domain.AlipayTradeQueryModel;
import com.alipay.api.domain.AlipayTradeRefundModel;
import com.alipay.api.domain.AlipayTradeWapPayModel;
import com.alipay.api.request.AlipayTradePrecreateRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.request.AlipayTradeWapPayRequest;
import com.alipay.api.response.AlipayTradePrecreateResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.alipay.api.response.AlipayTradeWapPayResponse;
import com.yami.shop.bean.pay.PayInfoDto;
import com.yami.shop.common.exception.YamiShopBindException;
import com.yami.shop.service.AlipayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 支付宝支付服务实现
 * @author yami
 */
@Service
public class AlipayServiceImpl implements AlipayService {

    @Autowired
    private AlipayClient alipayClient;

    @Value("${alipay.notifyUrl}")
    private String notifyUrl;

    @Value("${alipay.returnUrl}")
    private String returnUrl;

    @Override
    public String h5Pay(PayInfoDto payInfo) {
        System.out.println("开始生成支付宝H5支付表单");
        System.out.println("支付单号: " + payInfo.getPayNo());
        System.out.println("支付金额: " + payInfo.getPayAmount());
        System.out.println("商品信息: " + payInfo.getBody());
        System.out.println("通知地址: " + notifyUrl);
        System.out.println("返回地址: " + returnUrl);

        AlipayTradeWapPayRequest request = new AlipayTradeWapPayRequest();
        request.setReturnUrl(returnUrl);
        request.setNotifyUrl(notifyUrl);

        AlipayTradeWapPayModel model = new AlipayTradeWapPayModel();
        model.setOutTradeNo(payInfo.getPayNo());
        model.setTotalAmount(String.valueOf(payInfo.getPayAmount()));
        model.setSubject("购物支付");
        model.setProductCode("QUICK_WAP_WAY");

        request.setBizModel(model);

        try {
            System.out.println("准备调用支付宝API...");
            AlipayTradeWapPayResponse response = alipayClient.pageExecute(request);
            System.out.println("支付宝API响应成功状态: " + response.isSuccess());
            if (!response.isSuccess()) {
                System.err.println("支付宝API错误码: " + response.getCode());
                System.err.println("支付宝API错误信息: " + response.getMsg());
                System.err.println("支付宝API详细错误: " + response.getSubCode() + " - " + response.getSubMsg());
            }
            if (response.isSuccess()) {
                String formHtml = response.getBody();
                System.out.println("生成的支付表单长度: " + (formHtml != null ? formHtml.length() : 0));
                System.out.println("=== 支付表单内容 ===");
                System.out.println(formHtml);
                System.out.println("=== 支付表单结束 ===");
                return formHtml;
            } else {
                System.err.println("支付宝H5支付失败: " + response.getSubMsg());
                throw new YamiShopBindException("支付宝H5支付失败: " + response.getSubMsg());
            }
        } catch (AlipayApiException e) {
            System.err.println("支付宝H5支付异常: " + e.getMessage());
            throw new YamiShopBindException("支付宝H5支付异常: " + e.getMessage());
        }
    }

    @Override
    public String faceToFacePay(PayInfoDto payInfo) {
        AlipayTradePrecreateRequest request = new AlipayTradePrecreateRequest();
        request.setNotifyUrl(notifyUrl);

        AlipayTradePrecreateModel model = new AlipayTradePrecreateModel();
        model.setOutTradeNo(payInfo.getPayNo());
        model.setTotalAmount(String.valueOf(payInfo.getPayAmount()));
        model.setSubject(payInfo.getBody());

        request.setBizModel(model);

        try {
            AlipayTradePrecreateResponse response = alipayClient.execute(request);
            if (response.isSuccess()) {
                return response.getQrCode();
            } else {
                throw new YamiShopBindException("支付宝当面付失败: " + response.getSubMsg());
            }
        } catch (AlipayApiException e) {
            throw new YamiShopBindException("支付宝当面付异常: " + e.getMessage());
        }
    }

    @Override
    public boolean queryPayStatus(String outTradeNo) {
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();

        AlipayTradeQueryModel model = new AlipayTradeQueryModel();
        model.setOutTradeNo(outTradeNo);
        request.setBizModel(model);

        try {
            AlipayTradeQueryResponse response = alipayClient.execute(request);
            if (response.isSuccess()) {
                String tradeStatus = response.getTradeStatus();
                return "TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus);
            }
            return false;
        } catch (AlipayApiException e) {
            throw new YamiShopBindException("查询支付状态异常: " + e.getMessage());
        }
    }

    @Override
    public boolean refund(String outTradeNo, Double refundAmount, String refundReason) {
        AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();

        AlipayTradeRefundModel model = new AlipayTradeRefundModel();
        model.setOutTradeNo(outTradeNo);
        model.setRefundAmount(String.valueOf(refundAmount));
        model.setRefundReason(refundReason);

        request.setBizModel(model);

        try {
            AlipayTradeRefundResponse response = alipayClient.execute(request);
            if (response.isSuccess()) {
                // 退款成功的判断条件
                return "Y".equals(response.getFundChange());
            }
            return false;
        } catch (AlipayApiException e) {
            throw new YamiShopBindException("支付宝退款异常: " + e.getMessage());
        }
    }
}