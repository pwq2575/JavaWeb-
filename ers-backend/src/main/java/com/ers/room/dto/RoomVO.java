package com.ers.room.dto;

/**
 * 场地（API 契约 §6 #22）。
 */
public record RoomVO(Long id, String name, String location, Integer capacity, Integer status) {
}
