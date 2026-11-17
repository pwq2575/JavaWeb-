package com.ers.room.dto;

/**
 * 单日切片状态（API 契约 §6 #23）：status = FREE | BOOKED | PAST。
 * <p>IA §6.6：只返回状态与本人标记，不返回他人姓名（隐私）。</p>
 */
public record SlotVO(int slot, String label, String status, boolean reservedByMe) {
}
