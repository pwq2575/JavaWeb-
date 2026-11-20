package com.ers.security;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.ers.user.entity.SysUser;
import com.ers.user.mapper.SysUserMapper;

/**
 * 从 Authorization: Bearer &lt;JWT&gt; 中还原登录态。
 * <p>除了验签，还要回查账号状态与角色：令牌签发后被停用/改角色的账号应立即生效（权限矩阵 §5）。</p>
 * 校验失败不抛异常，交由 Security 入口点统一返回 40100。
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final SysUserMapper userMapper;

    public JwtAuthenticationFilter(JwtService jwtService, SysUserMapper userMapper) {
        this.jwtService = jwtService;
        this.userMapper = userMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            String token = header.substring(BEARER_PREFIX.length()).trim();
            Claims claims = jwtService.parse(token);
            if (claims != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                Long userId = Long.valueOf(claims.getSubject());
                SysUser user = userMapper.selectById(userId);
                if (user == null || Integer.valueOf(0).equals(user.getStatus())) {
                    // 账号被删除/停用：不建立认证，由入口点统一返回 40100
                    log.debug("JWT 认证拒绝：账号不存在或已停用 userId={}", userId);
                } else {
                    // 角色以库中为准，避免降权后旧令牌仍按 ADMIN 放行
                    String role = user.getRole();
                    var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
                    var auth = new UsernamePasswordAuthenticationToken(userId, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    log.debug("JWT 认证通过 userId={} role={}", userId, role);
                }
            }
        }
        filterChain.doFilter(request, response);
    }
}
