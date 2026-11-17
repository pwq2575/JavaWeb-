package com.ers.user;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ers.common.ApiResponse;
import com.ers.security.UserContext;
import com.ers.user.entity.SysUser;
import com.ers.user.mapper.SysUserMapper;

/**
 * 用户选择项：转交接收人等场景需要「人员下拉」，只暴露 id/姓名/账号。
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final SysUserMapper userMapper;

    public UserController(SysUserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public record UserOption(Long id, String username, String realName) {
    }

    @GetMapping("/options")
    public ApiResponse<List<UserOption>> options() {
        Long me = UserContext.requireUserId();
        List<SysUser> users = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getStatus, 1)
                .ne(SysUser::getId, me)
                .orderByAsc(SysUser::getId));
        return ApiResponse.ok(users.stream()
                .map(u -> new UserOption(u.getId(), u.getUsername(), u.getRealName()))
                .toList());
    }
}
