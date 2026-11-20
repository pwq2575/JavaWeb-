package com.ers.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.ers.BaseMockMvcTest;
import com.fasterxml.jackson.databind.JsonNode;

/** §1 登录（5 个断言）：5 个种子账号均可登录并拿到 token。 */
@DisplayName("§1 登录")
class LoginSmokeTest extends BaseMockMvcTest {

    @Test
    @DisplayName("admin/zhangsan/lisi/wangwu/zhaoliu 均可登录")
    void allSeedAccountsCanLogin() throws Exception {
        for (String username : new String[] { "admin", "zhangsan", "lisi", "wangwu", "zhaoliu" }) {
            JsonNode data = apiOk(postJson("/api/auth/login", null,
                    "{\"username\":\"" + username + "\",\"password\":\"" + DEMO_PASSWORD + "\"}"));
            assertThat(data.path("token").asText()).as("账号 %s 的 token", username).isNotBlank();
        }
    }
}
