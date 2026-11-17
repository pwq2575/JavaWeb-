package com.ers.admin;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ers.admin.dto.AdminDashboardVO;
import com.ers.asset.entity.Asset;
import com.ers.asset.entity.AssetRecord;
import com.ers.asset.entity.TransferRequest;
import com.ers.asset.mapper.AssetMapper;
import com.ers.asset.mapper.AssetRecordMapper;
import com.ers.asset.mapper.TransferRequestMapper;
import com.ers.common.Const;
import com.ers.common.Const.AssetAction;
import com.ers.common.Const.AssetStatus;
import com.ers.config.ConfigService;
import com.ers.procurement.entity.Procurement;
import com.ers.procurement.mapper.ProcurementMapper;
import com.ers.room.entity.Reservation;
import com.ers.room.mapper.ReservationMapper;
import com.ers.user.entity.SysUser;
import com.ers.user.mapper.SysUserMapper;

/**
 * 管理看板（API 契约 §9 #40，IA A6/A7）：KPI + 按类别价值 + 近 6 月借出趋势 + 状态分布 + 场地使用。
 * 数据量小（< 万级），直接在内存聚合，避免复杂 SQL。
 */
@Service
public class AdminDashboardService {

    private final AssetMapper assetMapper;
    private final AssetRecordMapper recordMapper;
    private final TransferRequestMapper transferMapper;
    private final SysUserMapper userMapper;
    private final ProcurementMapper procurementMapper;
    private final ReservationMapper reservationMapper;
    private final com.ers.room.mapper.RoomMapper roomMapper;
    private final ConfigService configService;

    public AdminDashboardService(AssetMapper assetMapper, AssetRecordMapper recordMapper,
            TransferRequestMapper transferMapper, SysUserMapper userMapper,
            ProcurementMapper procurementMapper, ReservationMapper reservationMapper,
            com.ers.room.mapper.RoomMapper roomMapper,
            ConfigService configService) {
        this.assetMapper = assetMapper;
        this.recordMapper = recordMapper;
        this.transferMapper = transferMapper;
        this.userMapper = userMapper;
        this.procurementMapper = procurementMapper;
        this.reservationMapper = reservationMapper;
        this.roomMapper = roomMapper;
        this.configService = configService;
    }

    public AdminDashboardVO build() {
        List<Asset> assets = assetMapper.selectList(null);
        LocalDate today = LocalDate.now();

        long inStock = 0;
        long borrowed = 0;
        long disabled = 0;
        long overdue = 0;
        Map<String, Long> byCategory = new LinkedHashMap<>();
        for (Asset a : assets) {
            if (AssetStatus.IN_STOCK.equals(a.getStatus())) {
                inStock++;
            } else if (AssetStatus.BORROWED.equals(a.getStatus())) {
                borrowed++;
                if (a.getDueDate() != null && a.getDueDate().isBefore(today)) {
                    overdue++;
                }
            } else if (AssetStatus.DISABLED.equals(a.getStatus())) {
                disabled++;
            }
            long value = a.getValue() == null ? 0L : a.getValue().longValue();
            byCategory.merge(a.getCategory() == null ? "未分类" : a.getCategory(), value, Long::sum);
        }

        long totalUsers = count(userMapper.selectCount(Wrappers.<SysUser>lambdaQuery()));
        int threshold = configService.creditThreshold();
        long lowCreditUsers = count(userMapper.selectCount(Wrappers.<SysUser>lambdaQuery()
                .lt(SysUser::getCreditScore, threshold).eq(SysUser::getStatus, 1)));

        long pendingProcurements = count(procurementMapper.selectCount(Wrappers.<Procurement>lambdaQuery()
                .isNull(Procurement::getDeletedAt)
                .eq(Procurement::getStatus, Const.ProcurementStatus.PENDING)));
        long pendingTransfers = count(transferMapper.selectCount(Wrappers.<TransferRequest>lambdaQuery()
                .eq(TransferRequest::getStatus, Const.TransferStatus.PENDING)));

        // 近 6 个月借出次数
        Map<String, Long> trend = new LinkedHashMap<>();
        YearMonth now = YearMonth.now();
        for (int i = 5; i >= 0; i--) {
            trend.put(now.minusMonths(i).toString(), 0L);
        }
        List<AssetRecord> borrows = recordMapper.selectList(Wrappers.<AssetRecord>lambdaQuery()
                .eq(AssetRecord::getAction, AssetAction.BORROW)
                .ge(AssetRecord::getHappenedAt, now.minusMonths(5).atDay(1).atStartOfDay()));
        for (AssetRecord r : borrows) {
            String month = YearMonth.from(r.getHappenedAt()).toString();
            if (trend.containsKey(month)) {
                trend.merge(month, 1L, Long::sum);
            }
        }

        List<AdminDashboardVO.NameValue> statusDistribution = List.of(
                new AdminDashboardVO.NameValue("在库", inStock),
                new AdminDashboardVO.NameValue("借出", borrowed),
                new AdminDashboardVO.NameValue("停用", disabled));

        // 场地使用（近 30 天各场地预约数）
        Map<String, String> roomNames = new LinkedHashMap<>();
        for (com.ers.room.entity.Room room : roomMapper.selectList(null)) {
            roomNames.put(String.valueOf(room.getId()), room.getName());
        }
        List<Reservation> recent = reservationMapper.selectList(Wrappers.<Reservation>lambdaQuery()
                .ge(Reservation::getReserveDate, today.minusDays(30)));
        Map<Long, Long> roomCounts = new LinkedHashMap<>();
        for (Reservation r : recent) {
            if (!Const.ReservationStatus.CANCELLED.equals(r.getStatus())) {
                roomCounts.merge(r.getRoomId(), 1L, Long::sum);
            }
        }
        List<AdminDashboardVO.NameValue> roomUsage = new ArrayList<>();
        for (Map.Entry<Long, Long> e : roomCounts.entrySet()) {
            roomUsage.add(new AdminDashboardVO.NameValue(roomNames.getOrDefault(String.valueOf(e.getKey()), "场地 " + e.getKey()),
                    e.getValue()));
        }

        List<AdminDashboardVO.NameValue> valueByCategory = new ArrayList<>();
        byCategory.forEach((k, v) -> valueByCategory.add(new AdminDashboardVO.NameValue(k, v)));
        List<AdminDashboardVO.MonthCount> monthlyTrend = new ArrayList<>();
        trend.forEach((k, v) -> monthlyTrend.add(new AdminDashboardVO.MonthCount(k, v)));

        return new AdminDashboardVO(assets.size(), inStock, borrowed, overdue, disabled,
                totalUsers, lowCreditUsers, pendingProcurements, pendingTransfers,
                valueByCategory, monthlyTrend, statusDistribution, roomUsage, "ALL");
    }

    private long count(Long value) {
        return value == null ? 0 : value;
    }
}
