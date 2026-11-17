package com.ers.admin.dto;

import java.time.LocalDateTime;

/**
 * 策略配置视图（API 契约 §9 #50）。
 */
public record AdminConfigVO(
        int creditThreshold,
        int highValueThreshold,
        int borrowDays,
        int renewMaxTimes,
        LocalDateTime updatedAt) {
}
