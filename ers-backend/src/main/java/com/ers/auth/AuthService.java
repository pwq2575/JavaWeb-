package com.ers.auth;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ers.auth.dto.AuthResponse;
import com.ers.auth.dto.LoginRequest;
import com.ers.auth.dto.RegisterRequest;
import com.ers.auth.dto.UserVO;
import com.ers.common.BizException;
import com.ers.security.JwtService;
import com.ers.user.entity.SysUser;
import com.ers.user.mapper.SysUserMapper;

/**
 * 认证业务：注册、登录、当前用户（API 契约 §2）。
 */
@Service
public class AuthService {

    private static final String ROLE_USER = "USER";
    private static final int DEFAULT_CREDIT = 100;
    private static final int STATUS_ENABLED = 1;

    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(SysUserMapper userMapper, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        String username = req.getUsername().trim();
        Long exists = userMapper.selectCount(Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, username));
        if (exists != null && exists > 0) {
            throw BizException.conflict("账号已存在，请更换");
        }
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setRealName(req.getRealName().trim());
        user.setRole(ROLE_USER);
        user.setCreditScore(DEFAULT_CREDIT);
        user.setStatus(STATUS_ENABLED);
        userMapper.insert(user);
        return buildAuth(user);
    }

    public AuthResponse login(LoginRequest req) {
        SysUser user = userMapper.selectOne(
                Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, req.getUsername().trim()));
        if (user == null || !passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw BizException.unauthorized("账号或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != STATUS_ENABLED) {
            throw BizException.unauthorized("账号已停用，请联系管理员");
        }
        return buildAuth(user);
    }

    public UserVO me(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw BizException.unauthorized("账号不存在或已删除，请重新登录");
        }
        return toVO(user);
    }

    private AuthResponse buildAuth(SysUser user) {
        String token = jwtService.generateToken(user.getId(), user.getRole());
        return new AuthResponse(token, jwtService.getTtlSeconds(), toVO(user));
    }

    private UserVO toVO(SysUser user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setRole(user.getRole());
        vo.setCreditScore(user.getCreditScore());
        vo.setStatus(user.getStatus());
        vo.setCreatedAt(user.getCreatedAt());
        return vo;
    }
}
