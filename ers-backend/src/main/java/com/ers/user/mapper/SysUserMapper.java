package com.ers.user.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ers.user.entity.SysUser;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
}
