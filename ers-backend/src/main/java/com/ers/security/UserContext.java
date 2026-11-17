package com.ers.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 从 SecurityContext 读取当前登录用户（JWT 过滤器把 userId 作为 principal）。
 */
public final class UserContext {

    private UserContext() {
    }

    /** 当前用户 id；未登录返回 null。 */
    public static Long userId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Long id)) {
            return null;
        }
        return id;
    }

    /** 当前用户 id；未登录抛 40100。 */
    public static Long requireUserId() {
        Long id = userId();
        if (id == null) {
            throw new com.ers.common.BizException(com.ers.common.ApiCode.UNAUTHORIZED, "未登录或登录已过期，请重新登录");
        }
        return id;
    }

    public static boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        for (GrantedAuthority a : auth.getAuthorities()) {
            if ("ROLE_ADMIN".equals(a.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    public static String role() {
        return isAdmin() ? "ADMIN" : "USER";
    }
}
