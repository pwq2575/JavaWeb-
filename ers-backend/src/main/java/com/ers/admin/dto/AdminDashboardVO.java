package com.ers.admin.dto;

import java.util.List;

/**
 * 管理看板（API 契约 §9 #40）：全量数据、无筛选（Q10 决策）。
 */
public record AdminDashboardVO(
        long totalAssets,
        long inStock,
        long borrowed,
        long overdue,
        long disabled,
        long totalUsers,
        long lowCreditUsers,
        long pendingProcurements,
        long pendingTransfers,
        List<NameValue> valueByCategory,
        List<MonthCount> monthlyBorrowTrend,
        List<NameValue> statusDistribution,
        List<NameValue> roomUsage,
        String range) {

    public record NameValue(String name, long value) {
    }

    public record MonthCount(String month, long count) {
    }
}
