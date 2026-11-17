package com.ers.admin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 管理员新增 / 编辑场地（API 契约 §9 #46-49；D14 课程要求：管理员可对场地增删改查）。
 */
public record AdminRoomSaveRequest(
        @NotBlank(message = "请填写场地名称")
        @Size(max = 32, message = "场地名称不超过 32 字") String name,
        @Size(max = 64, message = "位置不超过 64 字") String location,
        @Min(value = 1, message = "容纳人数至少 1 人")
        @Max(value = 200, message = "容纳人数不超过 200 人") Integer capacity,
        @Min(value = 0, message = "状态取值为 0 或 1")
        @Max(value = 1, message = "状态取值为 0 或 1") Integer status) {
}
