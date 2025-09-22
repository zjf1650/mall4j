/*
 * Copyright (c) 2018-2999 广州市蓝海创新科技有限公司 All rights reserved.
 *
 * https://www.mall4j.com/
 *
 * 未经允许，不可做商业用途！
 *
 * 版权所有，侵权必究！
 */

package com.yami.shop.bean.app.param;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 退款申请参数
 * @author yami
 */
@Data
@Schema(description = "退款申请参数")
public class RefundApplyParam {

    @Schema(description = "订单号", requiredMode = Schema.RequiredMode.REQUIRED)
    private String orderNumber;

    @Schema(description = "申请类型 1:仅退款 2:退货退款", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer applyType;

    @Schema(description = "退款金额", requiredMode = Schema.RequiredMode.REQUIRED)
    private Double refundAmount;

    @Schema(description = "申请原因", requiredMode = Schema.RequiredMode.REQUIRED)
    private String buyerMsg;

    @Schema(description = "凭证图片，多张用逗号分隔")
    private String photoFiles;
}