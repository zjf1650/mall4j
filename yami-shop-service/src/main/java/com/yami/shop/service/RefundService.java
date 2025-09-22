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

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yami.shop.bean.app.param.RefundApplyParam;
import com.yami.shop.bean.app.param.RefundAuditParam;
import com.yami.shop.bean.model.OrderRefund;

/**
 * 退款服务
 * @author yami
 */
public interface RefundService {

    /**
     * 用户申请退款
     * @param userId 用户ID
     * @param param 申请参数
     * @return 退款ID
     */
    Long applyRefund(String userId, RefundApplyParam param);

    /**
     * 商家审核退款
     * @param param 审核参数
     */
    void auditRefund(RefundAuditParam param);

    /**
     * 用户查询退款列表
     * @param userId 用户ID
     * @param current 当前页
     * @param size 页大小
     * @return 退款列表
     */
    IPage<OrderRefund> getUserRefundList(String userId, Long current, Long size);

    /**
     * 商家查询退款列表
     * @param current 当前页
     * @param size 页大小
     * @param status 状态筛选
     * @return 退款列表
     */
    IPage<OrderRefund> getMerchantRefundList(Long current, Long size, Integer status);

    /**
     * 根据ID查询退款详情
     * @param refundId 退款ID
     * @return 退款详情
     */
    OrderRefund getRefundById(Long refundId);
}