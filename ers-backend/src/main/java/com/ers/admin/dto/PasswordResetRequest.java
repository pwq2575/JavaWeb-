package com.ers.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 重置用户密码（API 契约 §9 #45）。
 */
public record PasswordResetRequest(
        @NotBlank(message = "请填写新密码")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)[\\S]{8,32}$", message = "密码需 8-32 位且同时包含字母与数字")
        String newPassword) {
}
