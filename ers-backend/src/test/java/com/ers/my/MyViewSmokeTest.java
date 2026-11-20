package com.ers.my;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.ers.BaseMockMvcTest;
import com.ers.common.ApiCode;

/** §6 我的视图（3 个断言）。 */
@DisplayName("§6 我的视图")
class MyViewSmokeTest extends BaseMockMvcTest {

    @Test
    @DisplayName("我的信誉（wangwu 55/阈值 60/受限）、工作台、我的借用")
    void myViews() throws Exception {
        String zhangsan = token("zhangsan");
        String lisi = token("lisi");
        String wangwu = token("wangwu");

        var credit = apiOk(getJson("/api/my/credit", wangwu));
        assertThat(credit.path("creditScore").asInt()).isEqualTo(55);
        assertThat(credit.path("threshold").asInt()).isEqualTo(60);
        assertThat(credit.path("restricted").asBoolean()).isTrue();

        assertThat(code(getJson("/api/my/workbench", zhangsan))).isEqualTo(ApiCode.SUCCESS);
        assertThat(code(getJson("/api/my/borrows", lisi))).isEqualTo(ApiCode.SUCCESS);
    }
}
