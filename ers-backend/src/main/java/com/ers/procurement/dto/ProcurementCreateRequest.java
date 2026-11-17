package com.ers.procurement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 提交采购申请（API 契约 §7 #28：名称 + 理由，Q6 决策：只有这两项）。
 */
public record ProcurementCreateRequest(
        @NotBlank(message = "请填写采购名称")
        @Size(max = 64, message = "采购名称不超过 64 字") String title,
        @NotBlank(message = "请填写采购理由")
        @Size(max = 200, message = "采购理由不超过 200 字") String reason) {
}
