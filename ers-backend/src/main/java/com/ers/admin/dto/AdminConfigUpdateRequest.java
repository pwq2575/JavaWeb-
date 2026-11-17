package com.ers.admin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

/**
 * 更新策略配置（API 契约 §9 #51）：部分更新，字段为 null 表示不修改。
 * 信誉阈值只允许 50 / 60 / 70 / 80（PRD 4.3）。
 */
public record AdminConfigUpdateRequest(
        @Pattern(regexp = "^(50|60|70|80)$", message = "信誉阈值只能是 50 / 60 / 70 / 80")
        String creditThreshold,
        @Min(value = 0, message = "高价值阈值不能为负")
        @Max(value = 1000000, message = "高价值阈值过大")
        Integer highValueThreshold,
        @Min(value = 1, message = "借出天数至少 1 天")
        @Max(value = 30, message = "借出天数不超过 30 天")
        Integer borrowDays,
        @Min(value = 0, message = "续借上限不能为负")
        @Max(value = 10, message = "续借上限不超过 10 次")
        Integer renewMaxTimes) {
}
