package com.ers.admin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * 管理员编辑用户（API 契约 §9 #43）：账号不可改。
 */
public record AdminUserUpdateRequest(
        @Size(max = 32, message = "姓名不超过 32 字") String realName,
        String role,
        @Min(value = 0, message = "状态取值为 0 或 1")
        @Max(value = 1, message = "状态取值为 0 或 1") Integer status) {
}
