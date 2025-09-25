/*
 * Copyright (c) 2018-2999 广州市蓝海创新科技有限公司 All rights reserved.
 *
 * https://www.mall4j.com/
 *
 * 未经允许，不可做商业用途！
 *
 * 版权所有，侵权必究！
 */

package com.yami.shop.admin.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yami.shop.bean.app.param.RefundAuditParam;
import com.yami.shop.bean.model.OrderRefund;
import com.yami.shop.common.response.ServerResponseEntity;
import com.yami.shop.security.admin.util.SecurityUtils;
import com.yami.shop.service.RefundService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * 商家退款管理接口
 * @author yami
 */
@RestController
@RequestMapping("/admin/refund")
@Tag(name = "退款管理", description = "商家退款管理接口")
public class AdminRefundController {

    @Autowired
    private RefundService refundService;

    @GetMapping("/list")
    @Operation(summary = "退款列表", description = "查询退款申请列表")
    @PreAuthorize("@pms.hasPermission('admin:refund:page')")
    public ServerResponseEntity<IPage<OrderRefund>> getRefundList(
            @RequestParam(value = "current", defaultValue = "1") Long current,
            @RequestParam(value = "size", defaultValue = "10") Long size,
            @RequestParam(value = "status", required = false) Integer status) {
        // 获取当前登录商户的shopId
        Long shopId = SecurityUtils.getSysUser().getShopId();

        System.out.println("=== 商家退款列表查询 ===");
        System.out.println("当前商户ID: " + shopId);
        System.out.println("查询参数 - current: " + current + ", size: " + size + ", status: " + status);

        IPage<OrderRefund> refundList = refundService.getMerchantRefundList(shopId, current, size, status);

        System.out.println("查询结果 - 总记录数: " + refundList.getTotal() + ", 当前页记录数: " + refundList.getRecords().size());

        return ServerResponseEntity.success(refundList);
    }

    @GetMapping("/detail/{refundId}")
    @Operation(summary = "退款详情", description = "查询退款申请详情")
    @PreAuthorize("@pms.hasPermission('admin:refund:info')")
    public ServerResponseEntity<OrderRefund> getRefundDetail(@PathVariable Long refundId) {
        OrderRefund refund = refundService.getRefundById(refundId);
        if (refund == null) {
            return ServerResponseEntity.showFailMsg("退款记录不存在");
        }

        // 验证退款记录是否属于当前商户
        Long currentShopId = SecurityUtils.getSysUser().getShopId();
        if (!refund.getShopId().equals(currentShopId)) {
            return ServerResponseEntity.showFailMsg("无权查看该退款记录");
        }

        return ServerResponseEntity.success(refund);
    }

    @PutMapping("/audit")
    @Operation(summary = "审核退款", description = "商家审核退款申请")
    @PreAuthorize("@pms.hasPermission('admin:refund:audit')")
    public ServerResponseEntity<Void> auditRefund(@Valid @RequestBody RefundAuditParam param) {
        refundService.auditRefund(param);
        return ServerResponseEntity.success();
    }
}