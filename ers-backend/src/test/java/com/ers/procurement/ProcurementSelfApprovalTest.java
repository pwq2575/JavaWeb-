package com.ers.procurement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.ers.BaseMockMvcTest;
import com.ers.common.ApiCode;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * GAP-08：采购自审标记（权限矩阵 §6 / PRD Q8、D8）。
 * <p>管理员可以审批自己提交的申请，但必须显式勾选「自审确认」（selfReview=true），
 * 落库 {@code procurement.self_approved=1} 供审计；未勾选返回 422。</p>
 */
@DisplayName("采购自审标记")
class ProcurementSelfApprovalTest extends BaseMockMvcTest {

    @Test
    @DisplayName("自审未勾选 → 422，单据仍待审批")
    void selfApproveWithoutConfirmationIsRejected() throws Exception {
        String admin = token("admin");
        long id = apiOk(postJson("/api/procurements", admin,
                "{\"title\":\"自审未勾选\",\"reason\":\"管理员自己提交自己批，但没勾选\"}")).path("id").asLong();

        var ra = putJson("/api/procurements/" + id + "/approve", admin,
                "{\"approveNote\":\"直接批\"}");
        ra.andExpect(status().isUnprocessableEntity());
        assertThat(code(ra)).isEqualTo(ApiCode.UNPROCESSABLE);

        // 失败后仍是 PENDING，管理员勾选后才能处理
        JsonNode pending = apiOk(getJson("/api/procurements?status=PENDING&size=100", admin));
        boolean stillPending = false;
        for (JsonNode row : pending.path("records")) {
            if (row.path("id").asLong() == id) {
                stillPending = true;
                assertThat(row.path("selfApproved").asBoolean()).isFalse();
            }
        }
        assertThat(stillPending).as("自审未勾选时不应改变单据状态").isTrue();
    }

    @Test
    @DisplayName("勾选自审 → 200 且 selfApproved=true")
    void selfApproveWithConfirmationRecordsFlag() throws Exception {
        String admin = token("admin");
        long id = apiOk(postJson("/api/procurements", admin,
                "{\"title\":\"自审勾选\",\"reason\":\"管理员自己提交，勾选自审后批准\"}")).path("id").asLong();

        JsonNode approved = apiOk(putJson("/api/procurements/" + id + "/approve", admin,
                "{\"approveNote\":\"同意，走耗材补货\",\"selfReview\":true}"));
        assertThat(approved.path("status").asText()).isEqualTo("APPROVED");
        assertThat(approved.path("selfApproved").asBoolean()).isTrue();

        // 列表回读也带标记（前端据此显示「自审」标签）
        JsonNode list = apiOk(getJson("/api/procurements?status=APPROVED&size=100", admin));
        boolean found = false;
        for (JsonNode row : list.path("records")) {
            if (row.path("id").asLong() == id) {
                found = true;
                assertThat(row.path("selfApproved").asBoolean()).isTrue();
            }
        }
        assertThat(found).isTrue();
    }

    @Test
    @DisplayName("管理员审批他人申请 → selfApproved=false，无需勾选")
    void approveOthersIsNotSelfApproval() throws Exception {
        String admin = token("admin");
        String lisi = token("lisi");
        long id = apiOk(postJson("/api/procurements", lisi,
                "{\"title\":\"他人提交\",\"reason\":\"李四提交的管理员审批\"}")).path("id").asLong();

        JsonNode approved = apiOk(putJson("/api/procurements/" + id + "/approve", admin,
                "{\"approveNote\":\"管理员同意\"}"));
        assertThat(approved.path("status").asText()).isEqualTo("APPROVED");
        assertThat(approved.path("selfApproved").asBoolean()).isFalse();
    }

    @Test
    @DisplayName("驳回自己的申请同样需勾选自审")
    void selfRejectRequiresConfirmation() throws Exception {
        String admin = token("admin");
        long id = apiOk(postJson("/api/procurements", admin,
                "{\"title\":\"自审驳回\",\"reason\":\"提交错了，自己驳回\"}")).path("id").asLong();

        var miss = putJson("/api/procurements/" + id + "/reject", admin,
                "{\"approveNote\":\"不买了\"}");
        miss.andExpect(status().isUnprocessableEntity());
        assertThat(code(miss)).isEqualTo(ApiCode.UNPROCESSABLE);

        JsonNode rejected = apiOk(putJson("/api/procurements/" + id + "/reject", admin,
                "{\"approveNote\":\"不买了\",\"selfReview\":true}"));
        assertThat(rejected.path("status").asText()).isEqualTo("REJECTED");
        assertThat(rejected.path("selfApproved").asBoolean()).isTrue();
    }
}
