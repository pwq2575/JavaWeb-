package com.ers.my;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ers.asset.entity.Asset;
import com.ers.asset.entity.TransferRequest;
import com.ers.asset.mapper.AssetMapper;
import com.ers.asset.mapper.TransferRequestMapper;
import com.ers.common.Const;
import com.ers.config.ConfigService;
import com.ers.credit.CreditService;
import com.ers.credit.dto.MyCreditVO;
import com.ers.credit.entity.CreditLog;
import com.ers.my.dto.WorkbenchVO;
import com.ers.room.RoomService;
import com.ers.room.dto.ReservationVO;
import com.ers.user.entity.SysUser;
import com.ers.user.mapper.SysUserMapper;

/**
 * 个人视图（API 契约 §5）：我的预约 / 我的信誉 / 工作台聚合。
 */
@Service
public class MyService {

    private final AssetMapper assetMapper;
    private final TransferRequestMapper transferMapper;
    private final SysUserMapper userMapper;
    private final CreditService creditService;
    private final ConfigService configService;
    private final RoomService roomService;

    public MyService(AssetMapper assetMapper, TransferRequestMapper transferMapper, SysUserMapper userMapper,
                     CreditService creditService, ConfigService configService, RoomService roomService) {
        this.assetMapper = assetMapper;
        this.transferMapper = transferMapper;
        this.userMapper = userMapper;
        this.creditService = creditService;
        this.configService = configService;
        this.roomService = roomService;
    }

    public List<ReservationVO> myReservations(Long userId) {
        return roomService.myReservations(userId);
    }

    public MyCreditVO myCredit(Long userId) {
        int score = creditService.scoreOf(userId);
        int threshold = configService.creditThreshold();
        List<MyCreditVO.Log> logs = new ArrayList<>();
        for (CreditLog l : creditService.historyOf(userId, 50)) {
            logs.add(new MyCreditVO.Log(l.getDelta(), l.getReason(), l.getRemark(), l.getBizDate(), l.getCreatedAt()));
        }
        return new MyCreditVO(score, threshold, score < threshold, logs);
    }

    public WorkbenchVO workbench(Long userId) {
        LocalDate today = LocalDate.now();
        List<Asset> held = assetMapper.selectList(Wrappers.<Asset>lambdaQuery()
                .eq(Asset::getHolderId, userId)
                .eq(Asset::getStatus, Const.AssetStatus.BORROWED)
                .orderByAsc(Asset::getDueDate));

        long dueSoon = 0;
        long overdue = 0;
        List<WorkbenchVO.BorrowItem> items = new ArrayList<>(held.size());
        for (Asset a : held) {
            long overdueDays = a.getDueDate() == null ? 0 : ChronoUnit.DAYS.between(a.getDueDate(), today);
            if (overdueDays > 0) {
                overdue++;
            } else if (overdueDays >= -2) {
                dueSoon++;
            }
            items.add(new WorkbenchVO.BorrowItem(a.getId(), a.getName(), a.getDueDate(), Math.max(0, overdueDays)));
        }

        List<TransferRequest> pending = transferMapper.selectList(Wrappers.<TransferRequest>lambdaQuery()
                .eq(TransferRequest::getFromUserId, userId)
                .eq(TransferRequest::getStatus, Const.TransferStatus.PENDING)
                .orderByDesc(TransferRequest::getCreatedAt));
        List<WorkbenchVO.TransferItem> todo = new ArrayList<>(pending.size());
        for (TransferRequest t : pending) {
            Asset a = assetMapper.selectById(t.getAssetId());
            SysUser to = userMapper.selectById(t.getToUserId());
            todo.add(new WorkbenchVO.TransferItem(t.getId(), a == null ? null : a.getName(),
                    to == null ? null : to.getRealName(), t.getCreatedAt()));
        }

        int score = creditService.scoreOf(userId);
        return new WorkbenchVO(score, configService.creditThreshold(), held.size(), dueSoon, overdue,
                pending.size(), roomService.todayCount(userId), items, todo);
    }
}
