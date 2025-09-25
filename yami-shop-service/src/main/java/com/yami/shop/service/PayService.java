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

import com.yami.shop.bean.app.param.PayParam;
import com.yami.shop.bean.pay.PayInfoDto;

import java.util.List;
import java.util.Map;

/**
 * @author lgh on 2018/09/15.
 */
public interface PayService {

    /**
     * 支付
     * @param userId
     * @param payParam
     * @return
     */
    PayInfoDto pay(String userId, PayParam payParam);

    /**
     * 支付成功
     * @param payNo
     * @param bizPayNo
     * @return
     */
    List<String> paySuccess(String payNo, String bizPayNo);

    /**
     * 查询支付状态
     * @param orderNumbers 订单号
     * @return 是否已支付
     */
    boolean queryPayStatus(String orderNumbers);

    /**
     * 直接通过支付单号查询支付状态
     * @param payNo 支付单号
     * @return 是否已支付
     */
    boolean queryPayStatusByPayNo(String payNo);

    /**
     * 通过支付单号查询支付结果（包含订单是否存在）
     * @param payNo 支付单号
     * @return 支付结果信息
     */
    Map<String, Object> queryPaymentResultByPayNo(String payNo);

}
