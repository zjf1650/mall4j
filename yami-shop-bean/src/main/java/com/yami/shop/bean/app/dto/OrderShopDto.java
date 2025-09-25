/*
 * Copyright (c) 2018-2999 广州市蓝海创新科技有限公司 All rights reserved.
 *
 * https://www.mall4j.com/
 *
 * 未经允许，不可做商业用途！
 *
 * 版权所有，侵权必究！
 */

package com.yami.shop.bean.app.dto;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 订单下的每个店铺
 *
 * @author YaMi
 */
@Data
public class OrderShopDto implements Serializable {

    /**
     * 店铺ID
     **/
    @Schema(description = "店铺id" ,requiredMode = Schema.RequiredMode.REQUIRED)
    private Long shopId;

    /**
     * 店铺名称
     **/
    @Schema(description = "店铺名称" ,requiredMode = Schema.RequiredMode.REQUIRED)
    private String shopName;

    @Schema(description = "实际总值" ,requiredMode = Schema.RequiredMode.REQUIRED)
    private Double actualTotal;

    @Schema(description = "商品总值" ,requiredMode = Schema.RequiredMode.REQUIRED)
    private Double total;

    @Schema(description = "商品总数" ,requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer totalNum;

    @Schema(description = "地址Dto" ,requiredMode = Schema.RequiredMode.REQUIRED)
    private UserAddrDto userAddrDto;

    @Schema(description = "产品信息" ,requiredMode = Schema.RequiredMode.REQUIRED)
    private List<OrderItemDto> orderItemDtos;

    @Schema(description = "运费" ,requiredMode = Schema.RequiredMode.REQUIRED)
    private Double transfee;

    @Schema(description = "优惠总额" ,requiredMode = Schema.RequiredMode.REQUIRED)
    private Double reduceAmount;

    @Schema(description = "促销活动优惠金额" ,requiredMode = Schema.RequiredMode.REQUIRED)
    private Double discountMoney;

    @Schema(description = "优惠券优惠金额" ,requiredMode = Schema.RequiredMode.REQUIRED)
    private Double couponMoney;

    /**
     * 创建时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "订单创建时间" ,requiredMode = Schema.RequiredMode.REQUIRED)
    private Date createTime;

    /**
     * 订单备注信息
     */
    @Schema(description = "订单备注信息" ,requiredMode = Schema.RequiredMode.REQUIRED)
    private String remarks;

    /**
     * 订单状态
     */
    @Schema(description = "订单状态" ,requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer status;

    /**
     * 支付方式
     */
    @Schema(description = "支付方式 1:微信支付 2:支付宝 6:支付宝H5支付 7:支付宝当面付")
    private Integer payType;

    /**
     * 退款状态
     */
    @Schema(description = "退款状态 0:未退款 1:申请退款中 2:退款成功 3:退款失败")
    private Integer refundStatus;
}
