package com.ers.config.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * 系统配置（sys_config）：信誉阈值 / 高价值阈值 / 借期 / 续借上限。
 * 值以字符串存储，应用层按类型解析（IA S5 策略层）。
 */
@Data
@TableName("sys_config")
public class SysConfig {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String configKey;
    private String configValue;
    private String description;
    private Long updatedBy;
    private LocalDateTime updatedAt;
}
