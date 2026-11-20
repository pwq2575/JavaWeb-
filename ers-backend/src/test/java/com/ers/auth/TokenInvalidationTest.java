package com.ers.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.ers.BaseMockMvcTest;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * 令牌与账号状态一致性（权限矩阵 §5：账号停用后"登录与已签发令牌均失效"）。
 * <p>回归 BUG-02：此前过滤器只验签，不查库，停用账号的旧令牌仍可访问。</p>
 */
@DisplayName("令牌与账号状态")
class TokenInvalidationTest extends BaseMockMvcTest {

    @Test
    @DisplayName("账号被停用后，已签发的旧令牌立即 401")
    void disabledUserTokenRejected() throws Exception {
        String admin = token("admin");

        JsonNode created = apiOk(postJson("/api/admin/users", admin,
                "{\"username\":\"tokenuser\",\"realName\":\"令牌测试\",\"role\":\"USER\",\"password\":\"abc12345\"}"));
        long uid = created.path("id").asLong();

        String userToken = token("tokenuser", "abc12345");
        getJson("/api/auth/me", userToken).andExpect(status().isOk());

        apiOk(putJson("/api/admin/users/" + uid, admin,
                "{\"realName\":\"令牌测试\",\"role\":\"USER\",\"status\":0}"));

        getJson("/api/auth/me", userToken).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("正常账号的令牌仍可访问（无回归）")
    void activeUserTokenStillWorks() throws Exception {
        String zhangsan = token("zhangsan");
        JsonNode me = apiOk(getJson("/api/auth/me", zhangsan));
        assertThat(me.path("username").asText()).isEqualTo("zhangsan");
    }
}
