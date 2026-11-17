package com.ers.room;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * 场地切片工具（PRD 4.4 / IA §6.6）：每天 09:00-18:00，固定 18 个 30 分钟切片，可约未来 14 天。
 */
public final class SlotUtil {

    public static final int SLOTS_PER_DAY = 18;
    public static final int BOOKABLE_DAYS = 14;
    private static final LocalTime OPEN = LocalTime.of(9, 0);
    private static final DateTimeFormatter HM = DateTimeFormatter.ofPattern("HH:mm");

    private SlotUtil() {
    }

    public static LocalDateTime startAt(LocalDate date, int slot) {
        return date.atTime(OPEN.plusMinutes(30L * slot));
    }

    public static LocalDateTime endAt(LocalDate date, int slot) {
        return date.atTime(OPEN.plusMinutes(30L * slot + 30));
    }

    public static String label(int slot) {
        return startAt(LocalDate.now(), slot).toLocalTime().format(HM) + "-"
                + endAt(LocalDate.now(), slot).toLocalTime().format(HM);
    }

    public static boolean isValidSlot(Integer slot) {
        return slot != null && slot >= 0 && slot < SLOTS_PER_DAY;
    }

    /** 可约范围：今天起 14 天内。 */
    public static boolean inBookableRange(LocalDate date) {
        LocalDate today = LocalDate.now();
        return date != null && !date.isBefore(today) && date.isBefore(today.plusDays(BOOKABLE_DAYS));
    }
}
