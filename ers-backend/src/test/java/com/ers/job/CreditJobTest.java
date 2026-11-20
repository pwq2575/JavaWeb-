package com.ers.job;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.ers.BaseMockMvcTest;

/**
 * 信誉扣分定时任务验收（PRD 4.3 / IA S5）：
 * 逾期每日 -5、按 (user, reason, ref_type, ref_id, biz_date) 唯一键防重复、下限 0 不出现负分。
 * 直接调用 CreditJob.deductOverdue()，不依赖 cron 触发时间。
 */
@DisplayName("信誉扣分定时任务")
class CreditJobTest extends BaseMockMvcTest {

    /** wangwu，种子信誉分 55 */
    private static final long WANGWU = 4L;

    @Autowired
    private CreditJob creditJob;

    @Test
    @DisplayName("逾期扣 5 分、同日重跑不再扣、信誉分下限为 0")
    void overdueDeduction() {
        // 先跑一遍，把种子里已有的逾期资产在“今天”的扣分消化掉，后续断言只看新增资产
        creditJob.deductOverdue();
        int scoreBefore = scoreOf(WANGWU);
        assertThat(scoreBefore).isEqualTo(55);

        long assetId = insertOverdueAsset("T-OVD-1");

        int deducted = creditJob.deductOverdue();
        assertThat(deducted).as("本次应至少对我新增的逾期资产扣分").isGreaterThanOrEqualTo(1);
        assertThat(scoreOf(WANGWU)).as("逾期一天扣 5 分").isEqualTo(scoreBefore - 5);

        Map<String, Object> logRow = jdbc.queryForMap(
                "select delta, reason, ref_type, ref_id from credit_log where user_id=? and ref_id=? and reason='OVERDUE'",
                WANGWU, assetId);
        assertThat(((Number) logRow.get("delta")).intValue()).isEqualTo(-5);
        assertThat(logRow.get("ref_type")).isEqualTo("ASSET");

        assertThat(creditJob.deductOverdue()).as("同日重跑必须幂等，不得重复扣分").isZero();
        assertThat(scoreOf(WANGWU)).isEqualTo(scoreBefore - 5);

        // 下限：把分数压到 3，再制造一笔新的逾期
        jdbc.update("update sys_user set credit_score=3 where id=?", WANGWU);
        insertOverdueAsset("T-OVD-2");
        creditJob.deductOverdue();
        assertThat(scoreOf(WANGWU)).as("信誉分下限为 0，不得为负").isZero();
    }

    private int scoreOf(long userId) {
        return jdbc.queryForObject("select credit_score from sys_user where id=?", Integer.class, userId);
    }

    private long insertOverdueAsset(String code) {
        jdbc.update("insert into asset (code,name,category,value,status,holder_id,borrow_at,due_date,renew_count) "
                + "values (?,?,'电子设备',100,'BORROWED',?,now(),?,0)",
                code, "测试逾期资产", WANGWU, LocalDate.now().minusDays(2));
        return jdbc.queryForObject("select id from asset where code=?", Long.class, code);
    }
}
