package com.ers.config;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ers.user.entity.SysUser;
import com.ers.user.mapper.SysUserMapper;

/**
 * 启动时把种子数据中的占位密码 __DEMO__ 转换为 BCrypt 密文（数据库设计说明书 §6.1）。
 * 幂等：只处理 password = '__DEMO__' 的行。
 */
@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    private static final String DEMO_PLACEHOLDER = "__DEMO__";
    private static final String DEMO_PASSWORD = "123456";

    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(SysUserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<SysUser> pending = userMapper.selectList(
                Wrappers.<SysUser>lambdaQuery().eq(SysUser::getPassword, DEMO_PLACEHOLDER));
        if (pending.isEmpty()) {
            return;
        }
        String encoded = passwordEncoder.encode(DEMO_PASSWORD);
        for (SysUser user : pending) {
            SysUser update = new SysUser();
            update.setId(user.getId());
            update.setPassword(encoded);
            userMapper.updateById(update);
        }
        log.info("已将 {} 个种子账号的占位密码初始化为演示密码 123456", pending.size());
    }
}
