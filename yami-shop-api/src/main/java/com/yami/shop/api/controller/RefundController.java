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

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yami.shop.bean.app.param.OrderRefundExpressParam;
import com.yami.shop.bean.app.param.RefundApplyParam;
import com.yami.shop.bean.model.OrderRefund;
import com.yami.shop.common.response.ServerResponseEntity;
import com.yami.shop.security.api.util.SecurityUtils;
import com.yami.shop.service.RefundService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * 用户退款相关接口
 * @author yami
 */
@RestController
@RequestMapping("/p/refund")
@Tag(name = "退款接口", description = "用户退款相关接口")
public class RefundController {

    @Autowired
    private RefundService refundService;

    @PostMapping("/apply")
    @Operation(summary = "申请退款", description = "用户申请订单退款")
    public ServerResponseEntity<Long> applyRefund(@Valid @RequestBody RefundApplyParam param) {
        String userId = SecurityUtils.getUser().getUserId();
        Long refundId = refundService.applyRefund(userId, param);
        return ServerResponseEntity.success(refundId);
    }

    @PostMapping("/express")
    @Operation(summary = "提交退货物流信息", description = "退货退款用户填写物流单号")
    public ServerResponseEntity<Void> submitRefundExpress(@Valid @RequestBody OrderRefundExpressParam param) {
        String userId = SecurityUtils.getUser().getUserId();
        refundService.submitRefundExpress(userId, param);
        return ServerResponseEntity.success();
    }

    @GetMapping("/list")
    @Operation(summary = "退款列表", description = "查询用户的退款申请列表")
    public ServerResponseEntity<IPage<OrderRefund>> getRefundList(
            @RequestParam(value = "current", defaultValue = "1") Long current,
            @RequestParam(value = "size", defaultValue = "10") Long size) {
        String userId = SecurityUtils.getUser().getUserId();
        IPage<OrderRefund> refundList = refundService.getUserRefundList(userId, current, size);
        return ServerResponseEntity.success(refundList);
    }

    @GetMapping("/detail/{refundId}")
    @Operation(summary = "退款详情", description = "查询退款申请详情")
    public ServerResponseEntity<OrderRefund> getRefundDetail(@PathVariable Long refundId) {
        String userId = SecurityUtils.getUser().getUserId();
        OrderRefund refund = refundService.getRefundById(refundId);

        if (refund == null || !refund.getUserId().equals(userId)) {
            return ServerResponseEntity.showFailMsg("退款记录不存在");
        }

        return ServerResponseEntity.success(refund);
    }
}
