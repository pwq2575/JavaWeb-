package com.ers.config;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ers.config.entity.SysConfig;
import com.ers.config.mapper.SysConfigMapper;

/**
 * 系统配置读取（策略层 S5）：信誉阈值 / 高价值阈值 / 借期 / 续借上限。
 * 值以字符串存储，此处统一按 int 解析并带默认值兜底。
 */
@Service
public class ConfigService {

    private final SysConfigMapper mapper;

    public ConfigService(SysConfigMapper mapper) {
        this.mapper = mapper;
    }

    public String get(String key, String defaultValue) {
        SysConfig cfg = mapper.selectOne(Wrappers.<SysConfig>lambdaQuery().eq(SysConfig::getConfigKey, key));
        if (cfg == null || cfg.getConfigValue() == null || cfg.getConfigValue().isBlank()) {
            return defaultValue;
        }
        return cfg.getConfigValue().trim();
    }

    public int getInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(get(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /** 信誉分限制阈值（默认 60，可配 50/60/70/80）。 */
    public int creditThreshold() {
        return getInt(com.ers.common.Const.ConfigKey.CREDIT_THRESHOLD, 60);
    }

    /** 高价值资产阈值（元，默认 500）。 */
    public int highValueThreshold() {
        return getInt(com.ers.common.Const.ConfigKey.HIGH_VALUE_THRESHOLD, 500);
    }

    /** 借出天数（默认 3）。 */
    public int borrowDays() {
        return getInt(com.ers.common.Const.ConfigKey.BORROW_DAYS, 3);
    }

    /** 续借次数上限（默认 3）。 */
    public int renewMaxTimes() {
        return getInt(com.ers.common.Const.ConfigKey.RENEW_MAX_TIMES, 3);
    }

    /**
     * 更新单个配置项（管理员 P-ADM-06 策略配置）：不存在则插入。
     */
    @org.springframework.transaction.annotation.Transactional
    public void set(String key, String value, Long operatorId) {
        SysConfig cfg = mapper.selectOne(Wrappers.<SysConfig>lambdaQuery().eq(SysConfig::getConfigKey, key));
        if (cfg == null) {
            cfg = new SysConfig();
            cfg.setConfigKey(key);
            cfg.setConfigValue(value);
            cfg.setUpdatedBy(operatorId);
            cfg.setUpdatedAt(java.time.LocalDateTime.now());
            mapper.insert(cfg);
            return;
        }
        cfg.setConfigValue(value);
        cfg.setUpdatedBy(operatorId);
        cfg.setUpdatedAt(java.time.LocalDateTime.now());
        mapper.updateById(cfg);
    }
}
