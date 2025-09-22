/*
 * Copyright (c) 2018-2999 广州市蓝海创新科技有限公司 All rights reserved.
 *
 * https://www.mall4j.com/
 *
 * 未经允许，不可做商业用途！
 *
 * 版权所有，侵权必究！
 */

package com.yami.shop.service;

import com.yami.shop.bean.pay.PayInfoDto;

/**
 * 支付宝支付服务
 * @author yami
 */
public interface AlipayService {

    /**
     * 支付宝H5支付
     * @param payInfo 支付信息
     * @return 支付表单HTML
     */
    String h5Pay(PayInfoDto payInfo);

    /**
     * 支付宝当面付（生成二维码）
     * @param payInfo 支付信息
     * @return 二维码内容
     */
    String faceToFacePay(PayInfoDto payInfo);

    /**
     * 查询支付状态
     * @param outTradeNo 商户订单号
     * @return 是否已支付
     */
    boolean queryPayStatus(String outTradeNo);

    /**
     * 支付宝退款
     * @param outTradeNo 商户订单号
     * @param refundAmount 退款金额
     * @param refundReason 退款原因
     * @return 是否退款成功
     */
    boolean refund(String outTradeNo, Double refundAmount, String refundReason);
}