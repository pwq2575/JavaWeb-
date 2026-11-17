package com.ers.admin;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ers.admin.dto.AdminUserCreateRequest;
import com.ers.admin.dto.AdminUserUpdateRequest;
import com.ers.admin.dto.PasswordResetRequest;
import com.ers.auth.dto.UserVO;
import com.ers.common.ApiResponse;
import com.ers.common.PageResult;
import com.ers.security.UserContext;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * 管理端用户管理（IA §4 P-ADM-07；D14 课程要求）。
 */
@Tag(name = "管理端-用户管理")
@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final AdminUserService userService;

    public AdminUserController(AdminUserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "用户列表（账号/姓名/角色/状态筛选）")
    @GetMapping
    public ApiResponse<PageResult<UserVO>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Integer status) {
        return ApiResponse.ok(userService.list(page, size, keyword, role, status));
    }

    @Operation(summary = "新建用户")
    @PostMapping
    public ApiResponse<UserVO> create(@Valid @RequestBody AdminUserCreateRequest req) {
        return ApiResponse.ok(userService.create(req));
    }

    @Operation(summary = "编辑用户（姓名/角色/状态）")
    @PutMapping("/{id}")
    public ApiResponse<UserVO> update(@PathVariable Long id, @Valid @RequestBody AdminUserUpdateRequest req) {
        return ApiResponse.ok(userService.update(id, req, UserContext.requireUserId()));
    }

    @Operation(summary = "删除用户（有业务数据时拒绝）")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        userService.delete(id, UserContext.requireUserId());
        return ApiResponse.ok(null);
    }

    @Operation(summary = "重置密码")
    @PutMapping("/{id}/password")
    public ApiResponse<Void> resetPassword(@PathVariable Long id, @Valid @RequestBody PasswordResetRequest req) {
        userService.resetPassword(id, req.newPassword());
        return ApiResponse.ok(null);
    }
}
