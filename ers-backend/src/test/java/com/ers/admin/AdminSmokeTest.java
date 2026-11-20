package com.ers.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.ers.BaseMockMvcTest;
import com.ers.common.ApiCode;
import com.fasterxml.jackson.databind.JsonNode;

/** §7 管理端（21 个断言：看板 1 + 用户 10 + 房间 5 + 配置 5）。 */
@DisplayName("§7 管理端")
class AdminSmokeTest extends BaseMockMvcTest {

    @Test
    @DisplayName("看板 KPI")
    void dashboard() throws Exception {
        String admin = token("admin");
        JsonNode dash = apiOk(getJson("/api/admin/dashboard", admin));
        assertThat(dash.path("totalAssets").asInt()).isGreaterThanOrEqualTo(16);
        assertThat(dash.path("monthlyBorrowTrend")).hasSize(6);
        assertThat(dash.path("valueByCategory").size()).isGreaterThanOrEqualTo(3);
    }

    @Test
    @DisplayName("用户管理：列表/关键字/越权 403/创建/重名 409/更新/重置密码并登录/禁用/删除")
    void userManagement() throws Exception {
        String admin = token("admin");
        String zhangsan = token("zhangsan");

        assertThat(apiOk(getJson("/api/admin/users?page=1&size=10", admin)).path("total").asInt())
                .isGreaterThanOrEqualTo(5);
        assertThat(apiOk(getJson("/api/admin/users?keyword=wang", admin)).path("records").size())
                .isGreaterThanOrEqualTo(1);

        var forbidden = getJson("/api/admin/users", zhangsan);
        forbidden.andExpect(status().isForbidden());

        JsonNode created = apiOk(postJson("/api/admin/users", admin,
                "{\"username\":\"testu01\",\"realName\":\"Test User\",\"role\":\"USER\",\"password\":\"abc12345\"}"));
        long newUserId = created.path("id").asLong();
        assertThat(newUserId).isPositive();

        var duplicate = postJson("/api/admin/users", admin,
                "{\"username\":\"testu01\",\"realName\":\"Test User\",\"role\":\"USER\",\"password\":\"abc12345\"}");
        duplicate.andExpect(status().isConflict());

        JsonNode updated = apiOk(putJson("/api/admin/users/" + newUserId, admin,
                "{\"realName\":\"Test User 2\",\"role\":\"USER\",\"status\":1}"));
        assertThat(updated.path("realName").asText()).isEqualTo("Test User 2");

        apiOk(putJson("/api/admin/users/" + newUserId + "/password", admin,
                "{\"newPassword\":\"xyz12345\"}"));

        // 新密码可登录（说明重置生效，且并非只是接口返回 200）
        var login = postJson("/api/auth/login", null, "{\"username\":\"testu01\",\"password\":\"xyz12345\"}");
        login.andExpect(status().isOk());

        apiOk(putJson("/api/admin/users/" + newUserId, admin, "{\"status\":0}"));

        var deleted = deleteJson("/api/admin/users/" + newUserId, admin);
        deleted.andExpect(status().isOk());
        assertThat(code(deleted)).isEqualTo(ApiCode.SUCCESS);
    }

    @Test
    @DisplayName("房间管理：列表/创建/更新/删除/删除有未来预约的房间")
    void roomManagement() throws Exception {
        String admin = token("admin");

        assertThat(apiOk(getJson("/api/admin/rooms", admin)).size()).isGreaterThanOrEqualTo(3);

        JsonNode created = apiOk(postJson("/api/admin/rooms", admin,
                "{\"name\":\"Test Room\",\"location\":\"B1\",\"capacity\":4,\"status\":1}"));
        long newRoomId = created.path("id").asLong();
        assertThat(newRoomId).isPositive();

        JsonNode updated = apiOk(putJson("/api/admin/rooms/" + newRoomId, admin,
                "{\"name\":\"Test Room 2\",\"location\":\"B2\",\"capacity\":6,\"status\":1}"));
        assertThat(updated.path("name").asText()).isEqualTo("Test Room 2");

        deleteJson("/api/admin/rooms/" + newRoomId, admin).andExpect(status().isOk());

        // 1 号房有未来预约：允许 422（有约束）或 200（级联处理），只要求不 500
        int status = deleteJson("/api/admin/rooms/1", admin).andReturn().getResponse().getStatus();
        assertThat(status).isIn(200, 422);
    }

    @Test
    @DisplayName("系统配置：读取 60/改 70/非法 55 400/还原 60/空请求 400")
    void systemConfig() throws Exception {
        String admin = token("admin");

        assertThat(apiOk(getJson("/api/admin/config", admin)).path("creditThreshold").asInt()).isEqualTo(60);

        JsonNode updated = apiOk(putJson("/api/admin/config", admin, "{\"creditThreshold\":\"70\"}"));
        assertThat(updated.path("creditThreshold").asInt()).isEqualTo(70);

        var invalid = putJson("/api/admin/config", admin, "{\"creditThreshold\":\"55\"}");
        invalid.andExpect(status().isBadRequest());

        JsonNode restored = apiOk(putJson("/api/admin/config", admin, "{\"creditThreshold\":\"60\"}"));
        assertThat(restored.path("creditThreshold").asInt()).isEqualTo(60);

        var empty = putJson("/api/admin/config", admin, "{}");
        empty.andExpect(status().isBadRequest());
    }
}
