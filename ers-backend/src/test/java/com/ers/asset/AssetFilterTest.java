package com.ers.asset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.ers.BaseMockMvcTest;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * 资产列表筛选/排序（PRD §4.2(7)：按名称/编号、状态、分类、持有者、是否逾期筛选；
 * API 契约 §5：highValueOnly、sort 白名单）。
 * <p>回归 BUG-03/04/05/06：此前 holderId/overdue/highValueOnly/sort 四个参数全部被忽略。</p>
 */
@DisplayName("资产列表筛选与排序")
class AssetFilterTest extends BaseMockMvcTest {

    @Test
    @DisplayName("holderId 过滤只返回该持有者的资产")
    void filterByHolder() throws Exception {
        String zhangsan = token("zhangsan");
        JsonNode rows = apiOk(getJson("/api/assets?holderId=3&size=100", zhangsan)).path("records");
        assertThat(rows.size()).isPositive();
        for (JsonNode r : rows) {
            assertThat(r.path("holderId").asLong()).isEqualTo(3L);
        }
    }

    @Test
    @DisplayName("overdue=true 只返回已逾期资产")
    void filterOverdue() throws Exception {
        String zhangsan = token("zhangsan");
        JsonNode rows = apiOk(getJson("/api/assets?overdue=true&size=100", zhangsan)).path("records");
        assertThat(rows.size()).as("种子中 asset1/asset3 已过期").isPositive();
        for (JsonNode r : rows) {
            assertThat(r.path("overdue").asBoolean()).isTrue();
            assertThat(r.path("status").asText()).isEqualTo("BORROWED");
        }
    }

    @Test
    @DisplayName("highValueOnly=true 只返回高价值资产（价值 >= 阈值 500）")
    void filterHighValue() throws Exception {
        String zhangsan = token("zhangsan");
        JsonNode rows = apiOk(getJson("/api/assets?highValueOnly=true&size=100", zhangsan)).path("records");
        assertThat(rows.size()).isPositive();
        for (JsonNode r : rows) {
            assertThat(r.path("highValue").asBoolean()).isTrue();
        }
    }

    @Test
    @DisplayName("sort=value,desc 按价值降序；非法 sort 回退 code 升序且不报错")
    void sortWhitelist() throws Exception {
        String zhangsan = token("zhangsan");

        JsonNode desc = apiOk(getJson("/api/assets?sort=value,desc&size=100", zhangsan)).path("records");
        List<java.math.BigDecimal> descValues = new ArrayList<>();
        for (JsonNode r : desc) {
            descValues.add(r.path("value").decimalValue());
        }
        for (int i = 1; i < descValues.size(); i++) {
            assertThat(descValues.get(i - 1)).isGreaterThanOrEqualTo(descValues.get(i));
        }

        var bad = getJson("/api/assets?sort=hacker,desc&size=5", zhangsan);
        bad.andExpect(status().isOk());
        JsonNode badRows = json(bad).path("data").path("records");
        List<String> codes = new ArrayList<>();
        for (JsonNode r : badRows) {
            codes.add(r.path("code").asText());
        }
        List<String> sorted = new ArrayList<>(codes);
        sorted.sort(String::compareTo);
        assertThat(codes).isEqualTo(sorted);
    }
}
