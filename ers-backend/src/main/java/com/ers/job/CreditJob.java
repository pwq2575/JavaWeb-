package com.ers.job;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ers.asset.entity.Asset;
import com.ers.asset.mapper.AssetMapper;
import com.ers.common.Const;
import com.ers.common.Const.AssetStatus;
import com.ers.common.Const.CreditReason;
import com.ers.common.Const.CreditRefType;
import com.ers.credit.CreditService;
import com.ers.room.SlotUtil;
import com.ers.room.entity.Reservation;
import com.ers.room.mapper.ReservationMapper;

/**
 * 信誉扣分定时任务（PRD 4.3，IA S5 策略层）：
 * <ul>
 *   <li>资产逾期：每天对仍借出且已过应归还日的资产扣 5 分，按 (资产, 日期) 去重（当天只扣一次）；</li>
 *   <li>会议室爽约：开始后超过 10 分钟仍未标记使用且未取消 → NO_SHOW 并一次性扣 5 分。</li>
 * </ul>
 * 另外在应用启动后立即补跑一次，避免服务停机跨天造成的漏扣。
 */
@Component
public class CreditJob {

    private static final Logger log = LoggerFactory.getLogger(CreditJob.class);

    private final AssetMapper assetMapper;
    private final ReservationMapper reservationMapper;
    private final CreditService creditService;

    public CreditJob(AssetMapper assetMapper, ReservationMapper reservationMapper, CreditService creditService) {
        this.assetMapper = assetMapper;
        this.reservationMapper = reservationMapper;
        this.creditService = creditService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        try {
            int overdue = deductOverdue();
            int noShow = markNoShow();
            log.info("[credit-job] 启动补跑完成：逾期扣分 {} 条，爽约标记 {} 条", overdue, noShow);
        } catch (Exception e) {
            log.warn("[credit-job] 启动补跑失败：{}", e.getMessage());
        }
    }

    /** 每天 00:05 扫描逾期。 */
    @Scheduled(cron = "0 5 0 * * ?")
    public void dailyOverdue() {
        int n = deductOverdue();
        log.info("[credit-job] 每日逾期扫描完成，本次扣分 {} 条", n);
    }

    /** 每 10 分钟扫描爽约。 */
    @Scheduled(cron = "0 */10 * * * ?")
    public void noShowScan() {
        int n = markNoShow();
        if (n > 0) {
            log.info("[credit-job] 爽约扫描完成，本次标记 {} 条", n);
        }
    }

    /** 逾期扣分：按 (资产, 当天日期) 幂等。 */
    public int deductOverdue() {
        LocalDate today = LocalDate.now();
        List<Asset> overdue = assetMapper.selectList(Wrappers.<Asset>lambdaQuery()
                .eq(Asset::getStatus, AssetStatus.BORROWED)
                .isNotNull(Asset::getDueDate)
                .lt(Asset::getDueDate, today));
        int count = 0;
        for (Asset a : overdue) {
            if (a.getHolderId() == null) {
                continue;
            }
            long days = java.time.temporal.ChronoUnit.DAYS.between(a.getDueDate(), today);
            boolean ok = creditService.deductOnce(a.getHolderId(), CreditReason.OVERDUE, CreditRefType.ASSET,
                    a.getId(), today, "资产逾期 " + days + " 天（" + a.getCode() + "）");
            if (ok) {
                count++;
            }
        }
        return count;
    }

    /** 爽约标记：开始后 10 分钟仍未使用且未取消。 */
    public int markNoShow() {
        LocalDateTime now = LocalDateTime.now();
        List<Reservation> booked = reservationMapper.selectList(Wrappers.<Reservation>lambdaQuery()
                .eq(Reservation::getStatus, Const.ReservationStatus.BOOKED));
        int count = 0;
        for (Reservation r : booked) {
            if (r.getReserveDate() == null || r.getTimeSlot() == null) {
                continue;
            }
            LocalDateTime start = SlotUtil.startAt(r.getReserveDate(), r.getTimeSlot());
            if (now.isAfter(start.plusMinutes(10))) {
                int rows = reservationMapper.update(null, com.baomidou.mybatisplus.core.toolkit.Wrappers
                        .<Reservation>lambdaUpdate()
                        .set(Reservation::getStatus, Const.ReservationStatus.NO_SHOW)
                        .set(Reservation::getUpdatedAt, now)
                        .eq(Reservation::getId, r.getId())
                        .eq(Reservation::getStatus, Const.ReservationStatus.BOOKED));
                if (rows > 0) {
                    creditService.deductOnce(r.getUserId(), CreditReason.NO_SHOW, CreditRefType.RESERVATION,
                            r.getId(), r.getReserveDate(), "会议室爽约（" + r.getTimeSlot() + " 时段未使用）");
                    count++;
                }
            }
        }
        return count;
    }
}
