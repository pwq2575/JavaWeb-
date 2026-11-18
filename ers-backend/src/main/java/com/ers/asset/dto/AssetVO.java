package com.ers.asset.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 资产列表/详情通用视图。字段与 IA §4 P-ASSET-01 / P-ASSET-02 对齐。
 */
public record AssetVO(
        Long id,
        String code,
        String name,
        String category,
        BigDecimal value,
        String status,
        Long holderId,
        String holderName,
        LocalDateTime borrowAt,
        LocalDate dueDate,
        Integer renewCount,
        Long daysLeft,
        boolean overdue,
        String description,
        boolean highValue,
        boolean canBorrow,
        boolean canRenew,
        boolean canReturn,
        /** 资产被他人借出且无待审批申请时，当前用户可「申请接手」（B 向持有者 A 发起转交）。 */
        boolean canApplyTransfer,
        Long pendingTransferId,
        LocalDateTime createdAt) {
}
