package com.ers.admin;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ers.admin.dto.AdminUserCreateRequest;
import com.ers.admin.dto.AdminUserUpdateRequest;
import com.ers.auth.dto.UserVO;
import com.ers.common.ApiCode;
import com.ers.common.BizException;
import com.ers.common.Const;
import com.ers.common.PageResult;
import com.ers.user.entity.SysUser;
import com.ers.user.mapper.SysUserMapper;

/**
 * 管理端用户管理（API 契约 §9 #41-45，D14 课程要求：管理员可对用户增删改查 + 重置密码）。
 */
@Service
public class AdminUserService {

    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public AdminUserService(SysUserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    /** #41 用户列表：账号 / 姓名 / 角色 / 状态 筛选，不回密码。 */
    public PageResult<UserVO> list(long page, long size, String keyword, String role, Integer status) {
        LambdaQueryWrapper<SysUser> qw = Wrappers.<SysUser>lambdaQuery();
        if (keyword != null && !keyword.isBlank()) {
            String k = keyword.trim();
            qw.and(w -> w.like(SysUser::getUsername, k)
                    .or().like(SysUser::getRealName, k));
        }
        if (role != null && !role.isBlank()) {
            qw.eq(SysUser::getRole, role.trim());
        }
        if (status != null) {
            qw.eq(SysUser::getStatus, status);
        }
        qw.orderByAsc(SysUser::getId);
        Page<SysUser> p = userMapper.selectPage(new Page<>(Math.max(1, page), Math.max(1, Math.min(size, 100))), qw);
        List<UserVO> records = new ArrayList<>(p.getRecords().size());
        for (SysUser u : p.getRecords()) {
            records.add(toVO(u));
        }
        return new PageResult<>(records, p.getTotal(), p.getCurrent(), p.getSize());
    }

    /** #42 新建用户。 */
    @Transactional
    public UserVO create(AdminUserCreateRequest req) {
        SysUser u = new SysUser();
        u.setUsername(req.username().trim());
        u.setRealName(req.realName().trim());
        u.setRole(req.role() == null || req.role().isBlank() ? Const.Role.USER : req.role());
        u.setPassword(passwordEncoder.encode(req.password()));
        u.setCreditScore(100);
        u.setStatus(1);
        u.setCreatedAt(LocalDateTime.now());
        u.setUpdatedAt(LocalDateTime.now());
        try {
            userMapper.insert(u);
        } catch (DuplicateKeyException e) {
            throw new BizException(ApiCode.CONFLICT, "账号已存在：" + u.getUsername());
        }
        return toVO(u);
    }

    /** #43 编辑姓名 / 角色 / 状态（不许停用自己、不许降级最后一个管理员）。 */
    @Transactional
    public UserVO update(Long id, AdminUserUpdateRequest req, Long operatorId) {
        SysUser u = requireUser(id);
        if (req.realName() != null && !req.realName().isBlank()) {
            u.setRealName(req.realName().trim());
        }
        if (req.role() != null && !req.role().isBlank()) {
            if (!Const.Role.USER.equals(req.role()) && !Const.Role.ADMIN.equals(req.role())) {
                throw new BizException(ApiCode.BAD_REQUEST, "角色只能是 USER 或 ADMIN");
            }
            if (Const.Role.ADMIN.equals(u.getRole()) && Const.Role.USER.equals(req.role())) {
                requireNotLastAdmin(id);
            }
            u.setRole(req.role());
        }
        if (req.status() != null) {
            if (req.status() == 0 && id.equals(operatorId)) {
                throw new BizException(ApiCode.UNPROCESSABLE, "不能停用当前登录账号");
            }
            if (req.status() == 0 && Const.Role.ADMIN.equals(u.getRole())) {
                requireNotLastAdmin(id);
            }
            u.setStatus(req.status());
        }
        u.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(u);
        return toVO(u);
    }

    /** #44 删除用户：有业务数据时拒绝，建议改用停用。 */
    @Transactional
    public void delete(Long id, Long operatorId) {
        if (id.equals(operatorId)) {
            throw new BizException(ApiCode.UNPROCESSABLE, "不能删除当前登录账号");
        }
        SysUser u = requireUser(id);
        if (Const.Role.ADMIN.equals(u.getRole())) {
            requireNotLastAdmin(id);
        }
        try {
            userMapper.deleteById(id);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            throw new BizException(ApiCode.UNPROCESSABLE, "该用户已有业务数据，无法删除，请改为停用");
        }
    }

    /** #45 重置密码。 */
    @Transactional
    public void resetPassword(Long id, String newPassword) {
        SysUser u = requireUser(id);
        u.setPassword(passwordEncoder.encode(newPassword));
        u.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(u);
    }

    // ------------------------------------------------------------- 内部

    private SysUser requireUser(Long id) {
        SysUser u = id == null ? null : userMapper.selectById(id);
        if (u == null) {
            throw new BizException(ApiCode.NOT_FOUND, "用户不存在");
        }
        return u;
    }

    private void requireNotLastAdmin(Long excludeId) {
        Long admins = userMapper.selectCount(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getRole, Const.Role.ADMIN)
                .eq(SysUser::getStatus, 1)
                .ne(SysUser::getId, excludeId));
        if (admins == null || admins == 0) {
            throw new BizException(ApiCode.UNPROCESSABLE, "系统至少需要保留一个可用管理员");
        }
    }

    private UserVO toVO(SysUser u) {
        UserVO vo = new UserVO();
        vo.setId(u.getId());
        vo.setUsername(u.getUsername());
        vo.setRealName(u.getRealName());
        vo.setRole(u.getRole());
        vo.setCreditScore(u.getCreditScore());
        vo.setStatus(u.getStatus());
        vo.setCreatedAt(u.getCreatedAt());
        return vo;
    }
}
