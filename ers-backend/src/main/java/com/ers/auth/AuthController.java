package com.ers.auth;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ers.auth.dto.AuthResponse;
import com.ers.auth.dto.LoginRequest;
import com.ers.auth.dto.RegisterRequest;
import com.ers.auth.dto.UserVO;
import com.ers.common.ApiResponse;

import jakarta.validation.Valid;

/**
 * 认证接口（API 契约 §2）。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest req) {
        return ApiResponse.ok(authService.register(req));
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        return ApiResponse.ok(authService.login(req));
    }

    @GetMapping("/me")
    public ApiResponse<UserVO> me(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ApiResponse.ok(authService.me(userId));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        // 无状态 JWT：前端清除本地令牌即可；如需强制失效可引入 Redis 黑名单（PRD §7.1 可选）
        return ApiResponse.ok();
    }
}
