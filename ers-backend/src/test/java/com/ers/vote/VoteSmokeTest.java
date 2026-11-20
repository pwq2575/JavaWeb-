package com.ers.vote;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.ers.BaseMockMvcTest;
import com.ers.common.ApiCode;
import com.fasterxml.jackson.databind.JsonNode;

/** §5 投票（14 个断言，含“投票前隐藏票数”这一 IA 硬规则）。 */
@DisplayName("§5 投票")
class VoteSmokeTest extends BaseMockMvcTest {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @Test
    @DisplayName("投票详情隐藏票数/投票/重复投票 409/投后可见/管理端草稿-发布-关闭全链路")
    void voteLifecycle() throws Exception {
        String admin = token("admin");
        String zhangsan = token("zhangsan");
        String zhaoliu = token("zhaoliu");

        JsonNode votes = apiOk(getJson("/api/votes", zhangsan));
        long pubId = -1;
        for (JsonNode v : votes.path("records")) {
            if ("PUBLISHED".equals(v.path("status").asText())) {
                pubId = v.path("id").asLong();
            }
        }
        assertThat(pubId).as("种子中应存在已发布投票").isPositive();

        JsonNode detail = apiOk(getJson("/api/votes/" + pubId, zhaoliu));
        assertThat(detail.path("showResult").asBoolean()).isFalse();
        assertThat(detail.path("votable").asBoolean()).isTrue();
        JsonNode firstOption = detail.path("options").get(0);
        JsonNode votesNode = firstOption.path("votes");
        assertThat(votesNode.isMissingNode() || votesNode.isNull())
                .as("投票前不得返回票数，实际=%s", votesNode)
                .isTrue();
        long optId = firstOption.path("id").asLong();
        long opt2Id = detail.path("options").get(1).path("id").asLong();

        apiOk(postJson("/api/votes/" + pubId + "/vote", zhaoliu, "{\"optionId\":" + optId + "}"));

        var voteTwice = postJson("/api/votes/" + pubId + "/vote", zhaoliu, "{\"optionId\":" + opt2Id + "}");
        voteTwice.andExpect(status().isConflict());

        JsonNode after = apiOk(getJson("/api/votes/" + pubId, zhaoliu));
        assertThat(after.path("showResult").asBoolean()).isTrue();
        assertThat(after.path("myOptionId").asLong()).isEqualTo(optId);

        var badOption = postJson("/api/votes/" + pubId + "/vote", zhaoliu, "{\"optionId\":99999}");
        assertThat(badOption.andReturn().getResponse().getStatus()).isIn(400, 409);

        String deadline = LocalDateTime.now().plusDays(3).format(TS);
        JsonNode draft = apiOk(postJson("/api/admin/votes", admin,
                "{\"title\":\"New office plant\",\"description\":\"pick one\",\"deadline\":\"" + deadline
                        + "\",\"options\":[\"cactus\",\"fern\"]}"));
        assertThat(draft.path("status").asText()).isEqualTo("DRAFT");
        long draftId = draft.path("id").asLong();

        var userCreate = postJson("/api/admin/votes", zhangsan,
                "{\"title\":\"x\",\"description\":\"y\",\"deadline\":\"" + deadline + "\",\"options\":[\"a\",\"b\"]}");
        userCreate.andExpect(status().isForbidden());

        apiOk(putJson("/api/admin/votes/" + draftId, admin,
                "{\"title\":\"New office plant v2\",\"description\":\"pick one\",\"deadline\":\"" + deadline
                        + "\",\"options\":[\"cactus\",\"fern\",\"bamboo\"]}"));

        JsonNode published = apiOk(postJson("/api/admin/votes/" + draftId + "/publish", admin, null));
        assertThat(published.path("status").asText()).isEqualTo("PUBLISHED");

        var editPublished = putJson("/api/admin/votes/" + draftId, admin,
                "{\"title\":\"edit published\",\"description\":\"x\",\"deadline\":\"" + deadline
                        + "\",\"options\":[\"a\",\"b\"]}");
        editPublished.andExpect(status().isUnprocessableEntity());

        JsonNode closed = apiOk(postJson("/api/admin/votes/" + draftId + "/close", admin, null));
        assertThat(closed.path("status").asText()).isEqualTo("CLOSED");

        var voteAfterClose = postJson("/api/votes/" + draftId + "/vote", zhaoliu, "{\"optionId\":1}");
        voteAfterClose.andExpect(status().isUnprocessableEntity());
    }
}
