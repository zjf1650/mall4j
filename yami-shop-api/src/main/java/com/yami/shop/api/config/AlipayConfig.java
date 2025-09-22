/*
 * Copyright (c) 2018-2999 广州市蓝海创新科技有限公司 All rights reserved.
 *
 * https://www.mall4j.com/
 *
 * 未经允许，不可做商业用途！
 *
 * 版权所有，侵权必究！
 */

package com.yami.shop.api.config;

import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 支付宝配置
 * @author yami
 */
@Configuration
@ConfigurationProperties(prefix = "alipay")
public class AlipayConfig {

    private String appId;
    private String privateKey;
    private String publicKey;
    private String serverUrl;
    private String format = "json";
    private String charset = "UTF-8";
    private String signType = "RSA2";
    private String notifyUrl;
    private String returnUrl;

    @Bean
    public AlipayClient alipayClient() {
        System.out.println("=== 支付宝配置信息 ===");
        System.out.println("AppId: " + appId);
        System.out.println("ServerUrl: " + serverUrl);
        System.out.println("SignType: " + signType);
        System.out.println("PrivateKey前50位: " + (privateKey != null ? privateKey.substring(0, Math.min(50, privateKey.length())) : "null"));
        System.out.println("PublicKey前50位: " + (publicKey != null ? publicKey.substring(0, Math.min(50, publicKey.length())) : "null"));
        return new DefaultAlipayClient(serverUrl, appId, privateKey, format, charset, publicKey, signType);
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getCharset() {
        return charset;
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }

    public String getSignType() {
        return signType;
    }

    public void setSignType(String signType) {
        this.signType = signType;
    }

    public String getNotifyUrl() {
        return notifyUrl;
    }

    public void setNotifyUrl(String notifyUrl) {
        this.notifyUrl = notifyUrl;
    }

    public String getReturnUrl() {
        return returnUrl;
    }

    public void setReturnUrl(String returnUrl) {
        this.returnUrl = returnUrl;
    }
}