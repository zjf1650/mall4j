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
import com.yami.shop.bean.app.param.OrderRefundExpressParam;
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
     * @param shopId 商户ID
     * @param current 当前页
     * @param size 页大小
     * @param status 状态筛选
     * @return 退款列表
     */
    IPage<OrderRefund> getMerchantRefundList(Long shopId, Long current, Long size, Integer status);

    /**
     * 根据ID查询退款详情
     * @param refundId 退款ID
     * @return 退款详情
     */
    OrderRefund getRefundById(Long refundId);

    /**
     * 根据订单号查询退款状态
     * @param orderNumber 订单号
     * @return 退款状态 0:未退款 1:申请退款中 2:退款成功 3:退款失败
     */
    Integer getRefundStatusByOrderNumber(String orderNumber);

    /**
     * 用户提交退货退款的物流信息
     * @param userId 用户ID
     * @param param 物流参数
     */
    void submitRefundExpress(String userId, OrderRefundExpressParam param);
}
