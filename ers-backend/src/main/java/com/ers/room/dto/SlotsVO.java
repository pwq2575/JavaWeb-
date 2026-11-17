package com.ers.room.dto;

import java.util.List;

/**
 * 场地单日切片（API 契约 §6 #23：18 个 30 分钟切片，可约未来 14 天）。
 */
public record SlotsVO(Long roomId, String date, List<SlotVO> slots, int bookableDays) {
}
