package com.ers.room.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 预约记录（我的预约 / 场地预约列表）。
 */
public record ReservationVO(
        Long id,
        Long roomId,
        String roomName,
        LocalDate reserveDate,
        Integer slot,
        String slotLabel,
        String status,
        LocalDateTime createdAt,
        boolean canCancel,
        boolean canUse) {
}
