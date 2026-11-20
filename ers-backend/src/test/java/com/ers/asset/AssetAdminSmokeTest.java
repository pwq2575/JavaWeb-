package com.ers.asset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.ers.BaseMockMvcTest;
import com.fasterxml.jackson.databind.JsonNode;

/** §8 资产管理补充（2 个断言）：管理员创建后可删除新资产。 */
@DisplayName("§8 资产管理")
class AssetAdminSmokeTest extends BaseMockMvcTest {

    @Test
    @DisplayName("创建资产 -> 返回 createdAt；随后删除成功")
    void createThenDeleteAsset() throws Exception {
        String admin = token("admin");

        JsonNode created = apiOk(postJson("/api/admin/assets", admin,
                "{\"code\":\"A-9001\",\"name\":\"Test Asset\",\"category\":\"其他\",\"value\":100,\"description\":\"smoke\"}"));
        assertThat(created.path("createdAt").isMissingNode()).isFalse();
        long newAssetId = created.path("id").asLong();
        assertThat(newAssetId).isPositive();

        var deleted = deleteJson("/api/admin/assets/" + newAssetId, admin);
        deleted.andExpect(status().isOk());
    }
}
