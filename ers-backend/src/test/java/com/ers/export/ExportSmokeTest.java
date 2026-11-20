package com.ers.export;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import com.ers.BaseMockMvcTest;

/** §9 导出（4 个断言）：资产导出仅管理员；借用导出仅本人或管理员。 */
@DisplayName("§9 导出")
class ExportSmokeTest extends BaseMockMvcTest {

    @Test
    @DisplayName("资产导出 200/非管理员 403；借用导出本人 200/他人 403")
    void exports() throws Exception {
        String admin = token("admin");
        String zhangsan = token("zhangsan");

        var assetsCsv = getJson("/api/exports/assets", admin);
        assetsCsv.andExpect(status().isOk());
        assertThat(assetsCsv.andReturn().getResponse().getHeader(HttpHeaders.CONTENT_TYPE))
                .contains("application/octet-stream");

        getJson("/api/exports/assets", zhangsan).andExpect(status().isForbidden());

        getJson("/api/exports/borrows", zhangsan).andExpect(status().isOk());

        getJson("/api/exports/borrows?userId=3", zhangsan).andExpect(status().isForbidden());
    }
}
