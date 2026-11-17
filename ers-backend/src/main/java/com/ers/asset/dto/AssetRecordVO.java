package com.ers.asset.dto;

import java.time.LocalDateTime;

/**
 * 流转记录（时间线）视图，对应 IA §6.4 审计事件流。
 */
public record AssetRecordVO(
        Long id,
        String action,
        Long operatorId,
        String operatorName,
        Long targetUserId,
        String targetName,
        String fromStatus,
        String toStatus,
        String remark,
        LocalDateTime happenedAt) {
}
