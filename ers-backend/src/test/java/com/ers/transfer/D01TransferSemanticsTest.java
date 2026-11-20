package com.ers.transfer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.ers.BaseMockMvcTest;
import com.ers.common.ApiCode;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * D-01 转交语义验收（PRD §4.2(3)）：B 申请接手，持有者 A 审批。
 * 种子态：资产 3 = BORROWED / 持有者 lisi(3) / dueDate 2026-11-14 / renewCount 0。
 * 对应开发日志 §7.1 与 §8.2 的定向断言，迁移为可重复执行的 Java 用例。
 */
@DisplayName("D-01 转交语义：B 申请，A 审批")
class D01TransferSemanticsTest extends BaseMockMvcTest {

    private static final long ASSET_ID = 3L;
    private static final long HOLDER_LISI = 3L;
    private static final long APPLICANT_ZHANGSAN = 2L;

    @Test
    @DisplayName("非持有者发起申请 -> 200，from_user_id=持有者、to_user_id=申请人（不受请求体影响）")
    void applicantCreatesRequest() throws Exception {
        String zhangsan = token("zhangsan");

        // 故意带上 toUserId=5：接口契约里没有该字段，应被忽略，落库仍按令牌推出 B
        JsonNode created = apiOk(postJson("/api/transfers", zhangsan,
                "{\"assetId\":3,\"applyReason\":\"外勤需要备用机\",\"toUserId\":5}"));
        assertThat(created.path("fromUserId").asLong()).isEqualTo(HOLDER_LISI);
        assertThat(created.path("toUserId").asLong()).isEqualTo(APPLICANT_ZHANGSAN);
        assertThat(created.path("status").asText()).isEqualTo("PENDING");
        assertThat(created.path("fromUserName").asText()).isEqualTo("李四");
        assertThat(created.path("toUserName").asText()).isEqualTo("张三");
        assertThat(created.has("toUserId")).isTrue();
    }

    @Test
    @DisplayName("持有者自己申请 -> 422 / 42200")
    void holderCannotApplyOwnAsset() throws Exception {
        String lisi = token("lisi");

        var ra = postJson("/api/transfers", lisi, "{\"assetId\":3,\"applyReason\":\"自己申请\"}");
        ra.andExpect(status().isUnprocessableEntity());
        assertThat(code(ra)).isEqualTo(ApiCode.UNPROCESSABLE);
        assertThat(json(ra).path("message").asText()).contains("你正持有该资产");
    }

    @Test
    @DisplayName("资产未借出时申请接手 -> 422 / 42200")
    void cannotApplyForAvailableAsset() throws Exception {
        String zhangsan = token("zhangsan");

        var ra = postJson("/api/transfers", zhangsan, "{\"assetId\":2,\"applyReason\":\"在库资产\"}");
        ra.andExpect(status().isUnprocessableEntity());
        assertThat(code(ra)).isEqualTo(ApiCode.UNPROCESSABLE);
        assertThat(json(ra).path("message").asText()).contains("该资产当前未借出");
    }

    @Test
    @DisplayName("同一资产已有待审批申请时重复申请 -> 409 / 40900")
    void duplicatePendingRequestRejected() throws Exception {
        String zhangsan = token("zhangsan");
        apiOk(postJson("/api/transfers", zhangsan, "{\"assetId\":3,\"applyReason\":\"第一次\"}"));

        var ra = postJson("/api/transfers", zhangsan, "{\"assetId\":3,\"applyReason\":\"第二次\"}");
        ra.andExpect(status().isConflict());
        assertThat(code(ra)).isEqualTo(ApiCode.CONFLICT);
        assertThat(json(ra).path("message").asText()).contains("已有待审批的转交申请");
    }

    @Test
    @DisplayName("非当前持有者审批 -> 403 / 40300")
    void nonHolderCannotApprove() throws Exception {
        String zhangsan = token("zhangsan");
        String wangwu = token("wangwu");
        long id = apiOk(postJson("/api/transfers", zhangsan, "{\"assetId\":3,\"applyReason\":\"审批越权\"}"))
                .path("id").asLong();

        var ra = putJson("/api/transfers/" + id + "/approve", wangwu, null);
        ra.andExpect(status().isForbidden());
        assertThat(code(ra)).isEqualTo(ApiCode.FORBIDDEN);
        assertThat(json(ra).path("message").asText()).contains("只有当前持有者可以审批");
    }

    @Test
    @DisplayName("持有者同意 -> 200，holder 变为 B，dueDate/renewCount 不变，新增 1 条 TRANSFER 记录")
    void holderApprovesAndOwnershipMoves() throws Exception {
        String zhangsan = token("zhangsan");
        String lisi = token("lisi");

        JsonNode before = apiOk(getJson("/api/assets/" + ASSET_ID, zhangsan));
        String dueBefore = before.path("asset").path("dueDate").asText();
        int renewBefore = before.path("asset").path("renewCount").asInt();
        long transferRowsBefore = countTransfers(before);

        long id = apiOk(postJson("/api/transfers", zhangsan, "{\"assetId\":3,\"applyReason\":\"同意路径\"}"))
                .path("id").asLong();

        JsonNode approved = apiOk(putJson("/api/transfers/" + id + "/approve", lisi, null));
        assertThat(approved.path("status").asText()).isEqualTo("APPROVED");

        JsonNode after = apiOk(getJson("/api/assets/" + ASSET_ID, zhangsan));
        assertThat(after.path("asset").path("holderId").asLong()).isEqualTo(APPLICANT_ZHANGSAN);
        assertThat(after.path("asset").path("holderName").asText()).isEqualTo("张三");
        assertThat(after.path("asset").path("status").asText()).isEqualTo("BORROWED");
        assertThat(after.path("asset").path("dueDate").asText()).isEqualTo(dueBefore);
        assertThat(after.path("asset").path("renewCount").asInt()).isEqualTo(renewBefore);
        assertThat(countTransfers(after)).isEqualTo(transferRowsBefore + 1);

        JsonNode latest = after.path("records").get(0);
        assertThat(latest.path("action").asText()).isEqualTo("TRANSFER");
        assertThat(latest.path("operatorName").asText()).as("操作者姓名不得为空").isEqualTo("李四");
        assertThat(latest.path("targetName").asText()).isEqualTo("张三");
        assertThat(latest.path("remark").asText()).contains("应归还日不变");
    }

    @Test
    @DisplayName("持有者拒绝 -> 200，holder 不变，B 可再次申请")
    void holderRejectsThenApplicantMayRetry() throws Exception {
        String zhangsan = token("zhangsan");
        String lisi = token("lisi");

        long id = apiOk(postJson("/api/transfers", zhangsan, "{\"assetId\":3,\"applyReason\":\"先拒\"}"))
                .path("id").asLong();

        JsonNode rejected = apiOk(putJson("/api/transfers/" + id + "/reject", lisi,
                "{\"rejectReason\":\"本周还要用\"}"));
        assertThat(rejected.path("status").asText()).isEqualTo("REJECTED");

        JsonNode asset = apiOk(getJson("/api/assets/" + ASSET_ID, zhangsan)).path("asset");
        assertThat(asset.path("holderId").asLong()).isEqualTo(HOLDER_LISI);

        var again = postJson("/api/transfers", zhangsan, "{\"assetId\":3,\"applyReason\":\"再申请\"}");
        again.andExpect(status().isOk());
        assertThat(code(again)).isEqualTo(ApiCode.SUCCESS);
    }

    private long countTransfers(JsonNode assetDetail) {
        long n = 0;
        for (JsonNode r : assetDetail.path("records")) {
            if ("TRANSFER".equals(r.path("action").asText())) {
                n++;
            }
        }
        return n;
    }
}
