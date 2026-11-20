package com.ers;

import java.util.List;

import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.GlobalConfigUtils;
import com.ers.asset.entity.Asset;
import com.ers.asset.entity.AssetRecord;
import com.ers.asset.entity.TransferRequest;
import com.ers.user.entity.SysUser;

/**
 * 纯 Mockito 单测的公共基类。
 * <p>MyBatis-Plus 的 Lambda 条件构造器依赖实体元数据缓存（{@code TableInfoHelper}），
 * 该缓存在 Spring/MyBatis 启动时由 mapper 扫描填充；脱离容器直接 new Service 时不会触发，
 * 会抛 {@code can not find lambda cache for this entity}。这里在测试类启动前手动注册实体元数据。</p>
 */
public abstract class UnitTestSupport {

    @BeforeAll
    static void initMybatisPlusLambdaCache() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        GlobalConfig globalConfig = GlobalConfigUtils.defaults();
        GlobalConfigUtils.setGlobalConfig(configuration, globalConfig);
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        assistant.setCurrentNamespace("com.ers.test");
        for (Class<?> entity : List.of(Asset.class, AssetRecord.class, TransferRequest.class, SysUser.class)) {
            if (TableInfoHelper.getTableInfo(entity) == null) {
                TableInfoHelper.initTableInfo(assistant, entity);
            }
        }
    }
}
