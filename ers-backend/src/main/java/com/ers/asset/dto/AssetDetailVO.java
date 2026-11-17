package com.ers.asset.dto;

import java.time.LocalDateTime;

/**
 * 资产详情：资产本体 + 流转时间线 + 待审批转交（如有）。
 */
public record AssetDetailVO(
        AssetVO asset,
        java.util.List<AssetRecordVO> records,
        TransferBrief pendingTransfer) {

    /** 详情页上的转交摘要（IA §6.5）。 */
    public record TransferBrief(
            Long id,
            Long fromUserId,
            String fromUserName,
            Long toUserId,
            String toUserName,
            String applyReason,
            LocalDateTime createdAt) {
    }
}
