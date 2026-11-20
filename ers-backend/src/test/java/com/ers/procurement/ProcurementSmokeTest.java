package com.ers.procurement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.ers.BaseMockMvcTest;
import com.ers.common.ApiCode;
import com.fasterxml.jackson.databind.JsonNode;

/** §4 采购（10 个断言）。 */
@DisplayName("§4 采购")
class ProcurementSmokeTest extends BaseMockMvcTest {

    @Test
    @DisplayName("列表/创建/审批/重复审批 409/驳回必填/他人删除 403/删除后 404")
    void procurementLifecycle() throws Exception {
        String admin = token("admin");
        String zhangsan = token("zhangsan");
        String lisi = token("lisi");
        String wangwu = token("wangwu");

        assertThat(code(getJson("/api/procurements", zhangsan))).isEqualTo(ApiCode.SUCCESS);
        assertThat(code(getJson("/api/procurements", admin))).isEqualTo(ApiCode.SUCCESS);

        JsonNode proc = apiOk(postJson("/api/procurements", lisi,
                "{\"title\":\"USB hub 8 ports\",\"reason\":\"team needs more USB ports\"}"));
        long procId = proc.path("id").asLong();
        assertThat(procId).isPositive();

        JsonNode approved = apiOk(putJson("/api/procurements/" + procId + "/approve", admin,
                "{\"approveNote\":\"ok buy it\"}"));
        assertThat(approved.path("status").asText()).isEqualTo("APPROVED");

        var approveTwice = putJson("/api/procurements/" + procId + "/approve", admin,
                "{\"approveNote\":\"again\"}");
        approveTwice.andExpect(status().isConflict());

        long proc2 = apiOk(postJson("/api/procurements", wangwu,
                "{\"title\":\"chair\",\"reason\":\"need a chair\"}")).path("id").asLong();

        var rejectNoReason = putJson("/api/procurements/" + proc2 + "/reject", admin,
                "{\"approveNote\":\"\"}");
        rejectNoReason.andExpect(status().isBadRequest());

        JsonNode rejected = apiOk(putJson("/api/procurements/" + proc2 + "/reject", admin,
                "{\"approveNote\":\"budget cut\"}"));
        assertThat(rejected.path("status").asText()).isEqualTo("REJECTED");

        var deleteByOther = deleteJson("/api/procurements/" + proc2, lisi);
        deleteByOther.andExpect(status().isForbidden());

        var deleteOwn = deleteJson("/api/procurements/" + proc2, wangwu);
        deleteOwn.andExpect(status().isOk());

        var deleteAgain = deleteJson("/api/procurements/" + proc2, wangwu);
        deleteAgain.andExpect(status().isNotFound());
    }
}
