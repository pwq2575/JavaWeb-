package com.ers;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 企业资源调度与协同治理平台 (ERS) 后端入口。
 */
@SpringBootApplication
@MapperScan("com.ers.**.mapper")
public class ErsApplication {

    public static void main(String[] args) {
        SpringApplication.run(ErsApplication.class, args);
    }
}
