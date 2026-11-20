package com.ers.transfer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.ers.BaseMockMvcTest;
import com.ers.common.ApiCode;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * §3 转交冒烟（9 个断言，对应 .tmp/smoke_all.ps1 第 3 段）。
 * 种子态：asset1 = BORROWED/持有者 zhangsan(2)/待审转交 2→3；asset7 = 持有者 zhaoliu(5)。
 */
@DisplayName("§3 转交冒烟")
class TransferSmokeTest extends BaseMockMvcTest {

    @Test
    @DisplayName("待审计数、四类拦截（自申请 422/在库 422/重复 409/越权审批 403）、同意后 holder 变更")
    void transferSection() throws Exception {
        String zhangsan = token("zhangsan");
        String lisi = token("lisi");
        String wangwu = token("wangwu");
        String zhaoliu = token("zhaoliu");

        JsonNode incoming = apiOk(getJson("/api/transfers?box=incoming", zhangsan));
        assertThat(incoming.path("pendingCount").asInt()).isGreaterThanOrEqualTo(1);

        var selfApply = postJson("/api/transfers", zhangsan, "{\"assetId\":1,\"applyReason\":\"self\"}");
        selfApply.andExpect(status().isUnprocessableEntity());

        var inStock = postJson("/api/transfers", zhangsan, "{\"assetId\":2,\"applyReason\":\"not borrowed\"}");
        inStock.andExpect(status().isUnprocessableEntity());

        var duplicate = postJson("/api/transfers", wangwu, "{\"assetId\":1,\"applyReason\":\"dup pending\"}");
        duplicate.andExpect(status().isConflict());

        // B(zhangsan) 对 zhaoliu 持有的 asset7 发起申请，持有者驳回
        JsonNode applied = apiOk(postJson("/api/transfers", zhangsan, "{\"assetId\":7,\"applyReason\":\"want drill\"}"));
        assertThat(applied.path("fromUserId").asLong()).isEqualTo(5L);
        assertThat(applied.path("toUserId").asLong()).isEqualTo(2L);

        long applyId = applied.path("id").asLong();
        JsonNode rejected = apiOk(putJson("/api/transfers/" + applyId + "/reject", zhaoliu,
                "{\"rejectReason\":\"in use this week\"}"));
        assertThat(rejected.path("status").asText()).isEqualTo("REJECTED");

        // asset1 的种子待审：非审批人 lisi 403，持有者 zhangsan 同意后 holder=3 且 dueDate 保留
        long pendingId = -1;
        for (JsonNode t : apiOk(getJson("/api/transfers?box=incoming", zhangsan)).path("records")) {
            if ("PENDING".equals(t.path("status").asText())) {
                pendingId = t.path("id").asLong();
            }
        }
        assertThat(pendingId).as("应存在种子待审转交").isPositive();

        var byNonApprover = putJson("/api/transfers/" + pendingId + "/approve", lisi, null);
        byNonApprover.andExpect(status().isForbidden());

        var byHolder = putJson("/api/transfers/" + pendingId + "/approve", zhangsan, null);
        byHolder.andExpect(status().isOk());

        JsonNode asset1 = apiOk(getJson("/api/assets/1", zhangsan)).path("asset");
        assertThat(asset1.path("holderId").asLong()).isEqualTo(3L);
        assertThat(asset1.path("dueDate").asText()).isNotBlank();
    }
}
