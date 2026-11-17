package com.ers.procurement.dto;

import jakarta.validation.constraints.Size;

/**
 * 审批采购申请（API 契约 §7 #29 / #30）：驳回时理由必填。
 */
public record ProcurementDecideRequest(
        @Size(max = 200, message = "审批备注不超过 200 字") String approveNote) {
}
