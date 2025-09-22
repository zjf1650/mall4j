/*
 * Copyright (c) 2018-2999 广州市蓝海创新科技有限公司 All rights reserved.
 *
 * https://www.mall4j.com/
 *
 * 未经允许，不可做商业用途！
 *
 * 版权所有，侵权必究！
 */

package com.yami.shop.api.controller;

import com.alipay.api.internal.util.AlipaySignature;
import com.yami.shop.api.config.AlipayConfig;
import com.yami.shop.service.PayService;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 支付宝支付回调控制器
 * @author yami
 */
@Hidden
@RestController
@RequestMapping("/alipay")
@AllArgsConstructor
public class AlipayNotifyController {

    private static final Logger logger = LoggerFactory.getLogger(AlipayNotifyController.class);

    private final PayService payService;
    private final AlipayConfig alipayConfig;

    /**
     * 支付异步通知
     */
    @PostMapping("/notify")
    public String notify(HttpServletRequest request) {
        logger.info("支付宝支付回调通知");

        Map<String, String> params = getRequestParams(request);

        try {
            // 验证签名
            boolean signVerified = AlipaySignature.rsaCheckV1(params,
                alipayConfig.getPublicKey(), alipayConfig.getCharset(), alipayConfig.getSignType());

            if (signVerified) {
                String tradeStatus = params.get("trade_status");
                String outTradeNo = params.get("out_trade_no");
                String tradeNo = params.get("trade_no");

                logger.info("支付宝回调验证成功，订单号：{}，交易状态：{}", outTradeNo, tradeStatus);

                if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
                    // 支付成功，更新订单状态
                    payService.paySuccess(outTradeNo, tradeNo);
                    logger.info("订单支付成功处理完成，订单号：{}", outTradeNo);
                }
                return "success";
            } else {
                logger.warn("支付宝回调签名验证失败");
                return "failure";
            }
        } catch (Exception e) {
            logger.error("支付宝回调处理失败", e);
            return "failure";
        }
    }

    /**
     * 支付同步回调（H5支付返回页面）
     */
    @GetMapping("/return")
    public String returnUrl(HttpServletRequest request) {
        logger.info("支付宝H5支付同步回调");

        Map<String, String> params = getRequestParams(request);

        try {
            // 验证签名
            boolean signVerified = AlipaySignature.rsaCheckV1(params,
                alipayConfig.getPublicKey(), alipayConfig.getCharset(), alipayConfig.getSignType());

            if (signVerified) {
                String outTradeNo = params.get("out_trade_no");
                logger.info("支付宝H5支付同步回调验证成功，订单号：{}", outTradeNo);
                // 跳转到支付结果页
                return "redirect:/pages/pay-result/pay-result?sts=1&payNo=" + outTradeNo;
            } else {
                logger.warn("支付宝H5支付同步回调签名验证失败");
                return "redirect:/pages/pay-result/pay-result?sts=0";
            }
        } catch (Exception e) {
            logger.error("支付宝H5支付同步回调处理失败", e);
            return "redirect:/pages/pay-result/pay-result?sts=0";
        }
    }

    /**
     * 查询支付状态接口
     */
    @GetMapping("/payStatus")
    public Map<String, Object> queryPayStatus(@RequestParam String orderNumbers) {
        Map<String, Object> result = new HashMap<>();
        try {
            boolean isPaid = payService.queryPayStatus(orderNumbers);
            result.put("isPaid", isPaid);
            result.put("success", true);
        } catch (Exception e) {
            logger.error("查询支付状态失败", e);
            result.put("isPaid", false);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 获取请求参数
     */
    private Map<String, String> getRequestParams(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        Map<String, String[]> requestParams = request.getParameterMap();
        for (Map.Entry<String, String[]> entry : requestParams.entrySet()) {
            String name = entry.getKey();
            String[] values = entry.getValue();
            String valueStr = "";
            for (int i = 0; i < values.length; i++) {
                valueStr = (i == values.length - 1) ? valueStr + values[i] : valueStr + values[i] + ",";
            }
            params.put(name, valueStr);
        }
        return params;
    }
}