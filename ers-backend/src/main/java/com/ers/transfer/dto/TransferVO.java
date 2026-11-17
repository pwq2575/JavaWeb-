package com.ers.transfer.dto;

import java.time.LocalDateTime;

/**
 * 转交记录（列表 / 详情共用，API 契约 §4 #15）。
 */
public record TransferVO(
        Long id,
        Long assetId,
        String assetCode,
        String assetName,
        Long fromUserId,
        String fromUserName,
        Long toUserId,
        String toUserName,
        String status,
        String applyReason,
        String rejectReason,
        LocalDateTime createdAt,
        LocalDateTime decidedAt) {
}
