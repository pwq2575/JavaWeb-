package com.ers.room.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 预约一个切片（API 契约 §6 #24）。
 */
public record ReservationCreateRequest(
        @NotNull(message = "场地不能为空") Long roomId,
        @NotNull(message = "日期不能为空") LocalDate date,
        @NotNull(message = "时段不能为空")
        @Min(value = 0, message = "时段不合法")
        @Max(value = 17, message = "时段不合法") Integer slot) {
}
