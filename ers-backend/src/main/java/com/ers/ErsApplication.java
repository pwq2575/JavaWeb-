package com.ers;

import org.mybatis.spring.annotation.MapperScan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 企业资源调度与协同治理平台 (ERS) 后端入口。
 */
@SpringBootApplication
@EnableScheduling
@MapperScan("com.ers.**.mapper")
public class ErsApplication {

    private static final Logger log = LoggerFactory.getLogger(ErsApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(ErsApplication.class, args);
    }

    /**
     * 启动完成后打印可访问地址。Spring Boot 本身不会输出服务地址，
     * 这里统一打印，避免每次启动都要自己去拼端口。
     */
    @Bean
    public ApplicationRunner startupBanner(Environment env) {
        return args -> {
            String port = env.getProperty("server.port", "8080");
            String contextPath = env.getProperty("server.servlet.context-path", "");
            String base = "http://localhost:" + port + contextPath;

            String line = "=".repeat(72);
            log.info(line);
            log.info("  ERS 后端启动完成  (profile: {})", String.join(",", env.getActiveProfiles()));
            log.info("  应用地址     : {}", base);
            log.info("  接口文档     : {}/swagger-ui.html", base);
            log.info("  OpenAPI JSON : {}/v3/api-docs", base);
            log.info("  登录验证     : curl -X POST {}/api/auth/login -H \"Content-Type: application/json\" -d \"{{\\\"username\\\":\\\"admin\\\",\\\"password\\\":\\\"123456\\\"}}\"", base);
            log.info("  停止服务     : Ctrl + C");
            log.info(line);
        };
    }
}
