package com.ers.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 管理员新建用户（API 契约 §9 #42；Q9 决策：管理员账号由管理员创建，不开放注册）。
 */
public record AdminUserCreateRequest(
        @NotBlank(message = "请填写账号")
        @Pattern(regexp = "^[A-Za-z0-9_]{3,20}$", message = "账号为 3-20 位字母、数字或下划线") String username,
        @NotBlank(message = "请填写姓名")
        @Size(max = 32, message = "姓名不超过 32 字") String realName,
        @Pattern(regexp = "^(USER|ADMIN)$", message = "角色只能是 USER 或 ADMIN") String role,
        @NotBlank(message = "请填写初始密码")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)[\\S]{8,32}$", message = "密码需 8-32 位且同时包含字母与数字")
        String password) {
}
