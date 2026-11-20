package com.ers.room;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.ers.BaseMockMvcTest;
import com.ers.common.ApiCode;
import com.fasterxml.jackson.databind.JsonNode;

/** §2 场地与预约（11 个断言，含 409 唯一索引冲突）。 */
@DisplayName("§2 场地与预约")
class RoomReservationSmokeTest extends BaseMockMvcTest {

    private final String tomorrow = LocalDate.now().plusDays(1).toString();

    @Test
    @DisplayName("场地列表 >=3，18 个时段且可预约 14 天")
    void roomsAndSlots() throws Exception {
        String zhangsan = token("zhangsan");

        JsonNode rooms = apiOk(getJson("/api/rooms", zhangsan));
        assertThat(rooms.size()).isGreaterThanOrEqualTo(3);

        JsonNode slots = apiOk(getJson("/api/rooms/1/slots?date=" + tomorrow, zhangsan));
        assertThat(slots.path("slots")).hasSize(18);
        assertThat(slots.path("bookableDays").asInt()).isEqualTo(14);
    }

    @Test
    @DisplayName("预约全链路：创建/同段冲突 409/非法时段 400/超窗 422/越权取消 403/取消/重复取消 422/未开始使用 422/我的预约")
    void reservationLifecycle() throws Exception {
        String zhangsan = token("zhangsan");
        String lisi = token("lisi");
        String wangwu = token("wangwu");

        JsonNode created = apiOk(postJson("/api/reservations", lisi,
                "{\"roomId\":1,\"date\":\"" + tomorrow + "\",\"slot\":8}"));
        long resvId = created.path("id").asLong();
        assertThat(resvId).isPositive();

        var conflict = postJson("/api/reservations", wangwu,
                "{\"roomId\":1,\"date\":\"" + tomorrow + "\",\"slot\":8}");
        conflict.andExpect(status().isConflict());
        assertThat(code(conflict)).isEqualTo(ApiCode.CONFLICT);

        var badSlot = postJson("/api/reservations", zhangsan,
                "{\"roomId\":1,\"date\":\"" + tomorrow + "\",\"slot\":99}");
        badSlot.andExpect(status().isBadRequest());

        var outOfWindow = postJson("/api/reservations", zhangsan,
                "{\"roomId\":1,\"date\":\"2030-01-01\",\"slot\":1}");
        outOfWindow.andExpect(status().isUnprocessableEntity());

        var cancelByOther = putJson("/api/reservations/" + resvId + "/cancel", wangwu, null);
        cancelByOther.andExpect(status().isForbidden());

        var cancelOwn = putJson("/api/reservations/" + resvId + "/cancel", lisi, null);
        cancelOwn.andExpect(status().isOk());

        var cancelTwice = putJson("/api/reservations/" + resvId + "/cancel", lisi, null);
        cancelTwice.andExpect(status().isUnprocessableEntity());

        var useTooEarly = putJson("/api/reservations/" + resvId + "/use", lisi, null);
        useTooEarly.andExpect(status().isUnprocessableEntity());

        var mine = getJson("/api/my/reservations", lisi);
        mine.andExpect(status().isOk());
        assertThat(code(mine)).isEqualTo(ApiCode.SUCCESS);
    }
}
