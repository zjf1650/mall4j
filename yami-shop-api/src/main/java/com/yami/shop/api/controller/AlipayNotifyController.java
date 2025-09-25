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

import com.alipay.api.AlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
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
    private final AlipayClient alipayClient;

    /**
     * 支付异步通知
     */
    @PostMapping("/notify")
    public String notify(HttpServletRequest request) {
        logger.info("支付宝支付回调通知");

        Map<String, String> params = getRequestParams(request);

        // 打印所有回调参数，便于回放测试
        logger.info("=== 支付宝异步通知参数 ===");
        for (Map.Entry<String, String> entry : params.entrySet()) {
            logger.info("参数: {} = {}", entry.getKey(), entry.getValue());
        }
        logger.info("=== 参数结束 ===");

        logger.info("支付宝配置 - PublicKey前50位: {}",
            alipayConfig.getPublicKey() != null ? alipayConfig.getPublicKey().substring(0, Math.min(50, alipayConfig.getPublicKey().length())) : "null");
        logger.info("支付宝配置 - Charset: {}", alipayConfig.getCharset());
        logger.info("支付宝配置 - SignType: {}", alipayConfig.getSignType());

        try {
            // 验证签名
            logger.info("开始验证签名...");
            boolean signVerified = AlipaySignature.rsaCheckV1(params,
                alipayConfig.getPublicKey(), alipayConfig.getCharset(), alipayConfig.getSignType());
            logger.info("签名验证结果: {}", signVerified);

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
                logger.warn("请检查: 1.支付宝公钥是否正确 2.参数是否完整 3.字符编码是否一致");
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
            logger.info("查询订单支付状态，订单号：{}", orderNumbers);
            boolean isPaid = payService.queryPayStatus(orderNumbers);
            result.put("code", "00000");
            result.put("data", Map.of("isPaid", isPaid));
            result.put("success", true);
        } catch (Exception e) {
            logger.error("查询支付状态失败", e);
            result.put("code", "A00001");
            result.put("msg", "查询支付状态失败: " + e.getMessage());
            result.put("data", Map.of("isPaid", false));
        }
        return result;
    }

    /**
     * 通过支付单号查询支付状态接口
     */
    @GetMapping("/payStatusByPayNo")
    public Map<String, Object> queryPayStatusByPayNo(@RequestParam String payNo) {
        Map<String, Object> result = new HashMap<>();
        try {
            logger.info("通过支付单号查询支付状态，payNo：{}", payNo);

            // 使用PayService的新方法，它会返回查询结果包含订单是否存在
            Map<String, Object> paymentResult = payService.queryPaymentResultByPayNo(payNo);

            result.put("code", "00000");
            result.put("data", paymentResult);

            logger.info("支付单号查询结果：payNo={}, 结果={}", payNo, paymentResult);

        } catch (Exception e) {
            logger.error("通过支付单号查询支付状态失败", e);
            result.put("code", "A00001");
            result.put("msg", "查询支付状态失败: " + e.getMessage());
            result.put("data", Map.of("isPaid", false, "orderExists", false));
        }
        return result;
    }

    /**
     * 查询支付宝支付状态接口
     */
    @GetMapping("/queryPayStatus")
    public Map<String, Object> queryAlipayPayStatus(@RequestParam String orderNumbers) {
        Map<String, Object> result = new HashMap<>();
        try {
            logger.info("查询支付宝支付状态，订单号：{}", orderNumbers);

            // 调用支付宝查询接口
            AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
            request.setBizContent("{\"out_trade_no\":\"" + orderNumbers + "\"}");

            AlipayTradeQueryResponse response = alipayClient.execute(request);
            logger.info("支付宝查询响应：{}", response.getBody());

            if (response.isSuccess()) {
                String tradeStatus = response.getTradeStatus();
                boolean isPaid = "TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus);

                result.put("code", "00000");
                result.put("data", Map.of(
                    "isPaid", isPaid,
                    "tradeStatus", tradeStatus,
                    "tradeNo", response.getTradeNo()
                ));

                logger.info("支付宝查询结果：订单号={}, 支付状态={}, 是否已支付={}",
                    orderNumbers, tradeStatus, isPaid);
            } else {
                logger.warn("支付宝查询失败：{}", response.getSubMsg());
                result.put("code", "A00001");
                result.put("msg", "支付宝查询失败: " + response.getSubMsg());
                result.put("data", Map.of("isPaid", false));
            }
        } catch (Exception e) {
            logger.error("查询支付宝支付状态失败", e);
            result.put("code", "A00001");
            result.put("msg", "查询支付状态失败: " + e.getMessage());
            result.put("data", Map.of("isPaid", false));
        }
        return result;
    }

    /**
     * 测试支付成功接口（仅用于测试）
     */
    @PostMapping("/testPaySuccess")
    public String testPaySuccess(@RequestParam String outTradeNo,
                                @RequestParam(required = false) String tradeNo) {
        try {
            logger.info("测试支付成功接口调用，订单号：{}", outTradeNo);
            String testTradeNo = tradeNo != null ? tradeNo : "TEST_" + System.currentTimeMillis();
            payService.paySuccess(outTradeNo, testTradeNo);
            logger.info("测试支付成功处理完成，订单号：{}", outTradeNo);
            return "success";
        } catch (Exception e) {
            logger.error("测试支付成功处理失败", e);
            return "failure";
        }
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