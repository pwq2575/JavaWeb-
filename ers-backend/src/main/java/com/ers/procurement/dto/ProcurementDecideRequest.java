package com.ers.procurement.dto;

import jakarta.validation.constraints.Size;

/**
 * 审批采购申请（API 契约 §7 #29 / #30）：驳回时理由必填。
 * <p>审批自己提交的申请（自审）时，必须显式勾选 {@code selfReview=true}，
 * 否则 42200「审批自己提交的申请需勾选「自审确认」」（权限矩阵 §6、PRD Q8/D8）。</p>
 */
public record ProcurementDecideRequest(
        @Size(max = 200, message = "审批备注不超过 200 字") String approveNote,
        Boolean selfReview) {
}
