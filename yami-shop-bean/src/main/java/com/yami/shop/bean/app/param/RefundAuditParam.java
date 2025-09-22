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
 * 退款审核参数
 * @author yami
 */
@Data
@Schema(description = "退款审核参数")
public class RefundAuditParam {

    @Schema(description = "退款ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long refundId;

    @Schema(description = "审核结果 2:同意 3:拒绝", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer auditResult;

    @Schema(description = "商家备注")
    private String sellerMsg;
}