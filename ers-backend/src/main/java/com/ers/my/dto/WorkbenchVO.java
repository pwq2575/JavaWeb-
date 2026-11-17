package com.ers.my.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 工作台聚合（API 契约 §5 #21，IA §6.10 S7）：一次返回首屏全部区块数据。
 */
public record WorkbenchVO(
        int creditScore,
        int creditThreshold,
        long myBorrows,
        long dueSoon,
        long overdue,
        long pendingApprovals,
        long myReservations,
        List<BorrowItem> todayBorrowList,
        List<TransferItem> todoTransferList) {

    public record BorrowItem(Long assetId, String assetName, LocalDate dueDate, long overdueDays) {
    }

    public record TransferItem(Long transferId, String assetName, String toUserName, LocalDateTime createdAt) {
    }
}
