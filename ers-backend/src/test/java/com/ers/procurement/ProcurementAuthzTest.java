package com.ers.procurement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.ers.BaseMockMvcTest;
import com.ers.common.ApiCode;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * 采购审批鉴权（API 契约 #29/#30：ADMIN）。
 * <p>回归 BUG-01：此前路径不在 /api/admin/** 下，Spring Security 只校验"已登录"，
 * 普通用户可审批/驳回任何人的采购申请（含自己的）。</p>
 */
@DisplayName("采购审批鉴权")
class ProcurementAuthzTest extends BaseMockMvcTest {

    @Test
    @DisplayName("普通用户 approve/reject → 403；管理员 approve → 200")
    void onlyAdminCanDecide() throws Exception {
        String zhangsan = token("zhangsan");
        String lisi = token("lisi");
        String admin = token("admin");

        long id = apiOk(postJson("/api/procurements", lisi,
                "{\"title\":\"鉴权测试\",\"reason\":\"验证普通用户不能审批\"}")).path("id").asLong();
        assertThat(id).isPositive();

        var userApprove = putJson("/api/procurements/" + id + "/approve", zhangsan,
                "{\"approveNote\":\"越权审批\"}");
        userApprove.andExpect(status().isForbidden());
        assertThat(code(userApprove)).isEqualTo(ApiCode.FORBIDDEN);

        var userReject = putJson("/api/procurements/" + id + "/reject", zhangsan,
                "{\"approveNote\":\"越权驳回\"}");
        userReject.andExpect(status().isForbidden());

        // 越权尝试后单据仍为待审批，管理员可正常处理
        JsonNode approved = apiOk(putJson("/api/procurements/" + id + "/approve", admin,
                "{\"approveNote\":\"管理员同意\"}"));
        assertThat(approved.path("status").asText()).isEqualTo("APPROVED");
    }

    @Test
    @DisplayName("申请人本人也不能审批自己的采购单")
    void applicantCannotApproveOwnRequest() throws Exception {
        String wangwu = token("wangwu");

        long id = apiOk(postJson("/api/procurements", wangwu,
                "{\"title\":\"自审测试\",\"reason\":\"自己批自己应被拒绝\"}")).path("id").asLong();

        var selfApprove = putJson("/api/procurements/" + id + "/approve", wangwu, null);
        selfApprove.andExpect(status().isForbidden());
        assertThat(code(selfApprove)).isEqualTo(ApiCode.FORBIDDEN);
    }
}
