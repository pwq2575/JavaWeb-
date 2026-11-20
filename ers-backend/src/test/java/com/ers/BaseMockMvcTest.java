package com.ers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.ers.common.ApiCode;
import com.ers.config.DataInitializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 集成测试基类（测试计划 S-01~S-20 落库）：
 * - 只连 ers_test（构造时断言，防止误连演示库 ers）；
 * - 每个用例前用 db/schema.sql + db/data.sql 重置为“种子态”，再用 DataInitializer 复现
 *   应用启动时的 __DEMO__ → BCrypt(123456) 处理，保证可重复、与手工冒烟同源；
 * - 令牌通过真实 /api/auth/login 获取，不伪造 JWT，鉴权链路一并覆盖。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class BaseMockMvcTest {

    /** 种子账号统一演示密码（与 DataInitializer、部署手册一致） */
    protected static final String DEMO_PASSWORD = "123456";

    @Autowired
    protected MockMvc mvc;
    @Autowired
    protected ObjectMapper mapper;
    @Autowired
    protected JdbcTemplate jdbc;
    @Autowired
    protected DataInitializer dataInitializer;
    @Autowired
    private DataSource dataSource;

    @BeforeEach
    void resetToSeedState() throws Exception {
        String db = jdbc.queryForObject("select database()", String.class);
        assertThat(db)
                .as("集成测试必须连 ers_test，实际连接库为 %s（检查 application-test.yml / 激活的 profile）", db)
                .isEqualTo("ers_test");

        executeScript("../db/schema.sql");
        executeScript("../db/data.sql");
        // 数据脚本把密码重置为占位符 __DEMO__；复用生产逻辑转成 BCrypt，登录才能通过
        dataInitializer.run(null);
    }

    private void executeScript(String path) throws Exception {
        File file = new File(path);
        assertThat(file)
                .as("SQL 脚本不存在：%s（测试需在 ers-backend 目录下运行 mvn test）", file.getAbsolutePath())
                .exists();
        try (Connection cn = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(cn,
                    new EncodedResource(new FileSystemResource(file), StandardCharsets.UTF_8));
        }
    }

    // ---------------------------------------------------------------- HTTP 助手

    protected String token(String username) throws Exception {
        return token(username, DEMO_PASSWORD);
    }

    protected String token(String username, String password) throws Exception {
        ResultActions ra = mvc.perform(post("/api/auth/login")
                .contentType("application/json;charset=UTF-8")
                .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"));
        JsonNode body = json(ra);
        assertThat(body.path("code").asInt())
                .as("登录失败：%s -> %s", username, body.path("message").asText())
                .isEqualTo(ApiCode.SUCCESS);
        String token = body.path("data").path("token").asText();
        assertThat(token).as("登录未返回 token：%s", username).isNotBlank();
        return token;
    }

    protected ResultActions getJson(String path, String token) throws Exception {
        return mvc.perform(withAuth(get(path), token));
    }

    protected ResultActions postJson(String path, String token, String json) throws Exception {
        var req = post(path).contentType("application/json;charset=UTF-8");
        if (json != null) {
            req = req.content(json);
        }
        return mvc.perform(withAuth(req, token));
    }

    protected ResultActions putJson(String path, String token, String json) throws Exception {
        var req = put(path).contentType("application/json;charset=UTF-8");
        if (json != null) {
            req = req.content(json);
        }
        return mvc.perform(withAuth(req, token));
    }

    protected ResultActions deleteJson(String path, String token) throws Exception {
        return mvc.perform(withAuth(delete(path), token));
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder withAuth(
            org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder builder, String token) {
        return token == null ? builder : builder.header("Authorization", "Bearer " + token);
    }

    // ---------------------------------------------------------------- JSON 助手

    protected JsonNode json(ResultActions ra) throws Exception {
        String raw = ra.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return raw == null || raw.isBlank() ? mapper.nullNode() : mapper.readTree(raw);
    }

    /** 取统一响应体里的 data 节点 */
    protected JsonNode data(ResultActions ra) throws Exception {
        return json(ra).path("data");
    }

    /** 断言业务码（code 字段），返回 data 以便继续断言 */
    protected JsonNode apiOk(ResultActions ra) throws Exception {
        JsonNode body = json(ra);
        assertThat(body.path("code").asInt())
                .as("期望业务码 0，实际 %s（%s）", body.path("code").asInt(), body.path("message").asText())
                .isEqualTo(ApiCode.SUCCESS);
        return body.path("data");
    }

    protected int code(ResultActions ra) throws Exception {
        return json(ra).path("code").asInt();
    }
}
