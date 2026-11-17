package com.ers.procurement.dto;

import java.time.LocalDateTime;

/**
 * 采购申请（API 契约 §7）。
 */
public record ProcurementVO(
        Long id,
        String title,
        String reason,
        String status,
        Long applicantId,
        String applicantName,
        String approverName,
        String approveNote,
        LocalDateTime createdAt,
        LocalDateTime decidedAt,
        boolean canDelete) {
}
