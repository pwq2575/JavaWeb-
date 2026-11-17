package com.ers.transfer;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ers.asset.entity.Asset;
import com.ers.asset.entity.AssetRecord;
import com.ers.asset.entity.TransferRequest;
import com.ers.asset.mapper.AssetMapper;
import com.ers.asset.mapper.AssetRecordMapper;
import com.ers.asset.mapper.TransferRequestMapper;
import com.ers.common.ApiCode;
import com.ers.common.BizException;
import com.ers.common.Const;
import com.ers.transfer.dto.TransferCreateRequest;
import com.ers.transfer.dto.TransferPageVO;
import com.ers.transfer.dto.TransferVO;
import com.ers.user.entity.SysUser;
import com.ers.user.mapper.SysUserMapper;

/**
 * 转交（PRD 4.2(3)，IA §6.5）：B 向当前持有者 A 发起 → A 审批 → 持有者变 B，应归还日不变。
 * <p>IA B3：转交是独立事件（写 asset_record TRANSFER），不是"归还 + 再借出"两步。</p>
 */
@Service
public class TransferService {

    private final TransferRequestMapper transferMapper;
    private final AssetMapper assetMapper;
    private final AssetRecordMapper recordMapper;
    private final SysUserMapper userMapper;

    public TransferService(TransferRequestMapper transferMapper, AssetMapper assetMapper,
                           AssetRecordMapper recordMapper, SysUserMapper userMapper) {
        this.transferMapper = transferMapper;
        this.assetMapper = assetMapper;
        this.recordMapper = recordMapper;
        this.userMapper = userMapper;
    }

    // ------------------------------------------------------------------ 发起

    @Transactional
    public TransferVO create(TransferCreateRequest req, Long currentUserId, boolean admin) {
        Asset asset = assetMapper.selectById(req.assetId());
        if (asset == null) {
            throw new BizException(ApiCode.NOT_FOUND, "资产不存在");
        }
        if (!Const.AssetStatus.BORROWED.equals(asset.getStatus()) || asset.getHolderId() == null) {
            throw new BizException(ApiCode.UNPROCESSABLE, "该资产当前未借出，无法转交");
        }
        Long holderId = asset.getHolderId();
        if (!admin && !holderId.equals(currentUserId)) {
            throw new BizException(ApiCode.FORBIDDEN, "只有当前持有者可以发起转交");
        }
        if (holderId.equals(req.toUserId())) {
            throw new BizException(ApiCode.UNPROCESSABLE, "不能转交给自己");
        }
        SysUser to = userMapper.selectById(req.toUserId());
        if (to == null || Integer.valueOf(0).equals(to.getStatus())) {
            throw new BizException(ApiCode.BAD_REQUEST, "接收人不存在或已停用");
        }
        Long pending = transferMapper.selectCount(Wrappers.<TransferRequest>lambdaQuery()
                .eq(TransferRequest::getAssetId, asset.getId())
                .eq(TransferRequest::getStatus, Const.TransferStatus.PENDING));
        if (pending != null && pending > 0) {
            throw new BizException(ApiCode.CONFLICT, "该资产已有待审批的转交申请");
        }

        TransferRequest t = new TransferRequest();
        t.setAssetId(asset.getId());
        t.setFromUserId(holderId);
        t.setToUserId(req.toUserId());
        t.setStatus(Const.TransferStatus.PENDING);
        t.setApplyReason(req.applyReason() == null ? null : req.applyReason().trim());
        t.setCreatedAt(LocalDateTime.now());
        transferMapper.insert(t);
        return toVO(t, asset, nameOf(holderId), nameOf(req.toUserId()));
    }

    // ------------------------------------------------------------------ 查询

    public TransferPageVO list(String box, String status, long page, long size, Long currentUserId) {
        LambdaQueryWrapper<TransferRequest> qw = Wrappers.<TransferRequest>lambdaQuery();
        if ("outgoing".equalsIgnoreCase(box)) {
            qw.eq(TransferRequest::getFromUserId, currentUserId);
        } else {
            // 默认 incoming：待我审批（我是当前持有者）
            qw.eq(TransferRequest::getFromUserId, currentUserId);
            if (status == null || status.isBlank()) {
                qw.eq(TransferRequest::getStatus, Const.TransferStatus.PENDING);
            }
        }
        if (status != null && !status.isBlank()) {
            qw.eq(TransferRequest::getStatus, status.trim());
        }
        qw.orderByDesc(TransferRequest::getCreatedAt);

        Page<TransferRequest> p = transferMapper.selectPage(new Page<>(Math.max(1, page), clampSize(size)), qw);
        Map<Long, Asset> assets = assetsOf(p.getRecords());
        Map<Long, String> names = namesOf(p.getRecords());

        List<TransferVO> records = new ArrayList<>();
        for (TransferRequest t : p.getRecords()) {
            records.add(toVO(t, assets.get(t.getAssetId()), names.get(t.getFromUserId()), names.get(t.getToUserId())));
        }
        Long pendingCount = transferMapper.selectCount(Wrappers.<TransferRequest>lambdaQuery()
                .eq(TransferRequest::getFromUserId, currentUserId)
                .eq(TransferRequest::getStatus, Const.TransferStatus.PENDING));
        return new TransferPageVO(records, p.getTotal(), p.getCurrent(), p.getSize(),
                pendingCount == null ? 0 : pendingCount);
    }

    // ------------------------------------------------------------------ 审批

    @Transactional
    public TransferVO approve(Long id, Long currentUserId) {
        TransferRequest t = requirePending(id, currentUserId);
        Asset asset = assetMapper.selectById(t.getAssetId());
        if (asset == null) {
            throw new BizException(ApiCode.NOT_FOUND, "资产不存在");
        }
        int rows = assetMapper.update(null, Wrappers.<Asset>lambdaUpdate()
                .set(Asset::getHolderId, t.getToUserId())
                .set(Asset::getUpdatedAt, LocalDateTime.now())
                .eq(Asset::getId, asset.getId())
                .eq(Asset::getHolderId, t.getFromUserId())
                .eq(Asset::getStatus, Const.AssetStatus.BORROWED));
        if (rows == 0) {
            throw new BizException(ApiCode.CONFLICT, "资产状态已变化，请刷新后重试");
        }
        LocalDateTime now = LocalDateTime.now();
        transferMapper.update(null, Wrappers.<TransferRequest>lambdaUpdate()
                .set(TransferRequest::getStatus, Const.TransferStatus.APPROVED)
                .set(TransferRequest::getDecidedAt, now)
                .eq(TransferRequest::getId, id)
                .eq(TransferRequest::getStatus, Const.TransferStatus.PENDING));

        AssetRecord record = new AssetRecord();
        record.setAssetId(asset.getId());
        record.setAction(Const.AssetAction.TRANSFER);
        record.setOperatorId(currentUserId);
        record.setTargetUserId(t.getToUserId());
        record.setFromStatus(Const.AssetStatus.BORROWED);
        record.setToStatus(Const.AssetStatus.BORROWED);
        record.setRemark("转交至" + nameOf(t.getToUserId()) + "，应归还日不变");
        record.setHappenedAt(now);
        recordMapper.insert(record);

        t.setStatus(Const.TransferStatus.APPROVED);
        t.setDecidedAt(now);
        return toVO(t, asset, nameOf(t.getFromUserId()), nameOf(t.getToUserId()));
    }

    @Transactional
    public TransferVO reject(Long id, String rejectReason, Long currentUserId) {
        TransferRequest t = requirePending(id, currentUserId);
        LocalDateTime now = LocalDateTime.now();
        int rows = transferMapper.update(null, Wrappers.<TransferRequest>lambdaUpdate()
                .set(TransferRequest::getStatus, Const.TransferStatus.REJECTED)
                .set(TransferRequest::getRejectReason, rejectReason.trim())
                .set(TransferRequest::getDecidedAt, now)
                .eq(TransferRequest::getId, id)
                .eq(TransferRequest::getStatus, Const.TransferStatus.PENDING));
        if (rows == 0) {
            throw new BizException(ApiCode.CONFLICT, "该转交申请已被处理，请刷新后重试");
        }
        t.setStatus(Const.TransferStatus.REJECTED);
        t.setRejectReason(rejectReason.trim());
        t.setDecidedAt(now);
        return toVO(t, assetMapper.selectById(t.getAssetId()), nameOf(t.getFromUserId()), nameOf(t.getToUserId()));
    }

    /** 待我审批的转交条数（工作台 / 侧栏角标）。 */
    public long pendingCountFor(Long userId) {
        Long c = transferMapper.selectCount(Wrappers.<TransferRequest>lambdaQuery()
                .eq(TransferRequest::getFromUserId, userId)
                .eq(TransferRequest::getStatus, Const.TransferStatus.PENDING));
        return c == null ? 0 : c;
    }

    /** 某资产待审批的转交（列表/详情复用）。 */
    public TransferRequest pendingOf(Long assetId) {
        return transferMapper.selectOne(Wrappers.<TransferRequest>lambdaQuery()
                .eq(TransferRequest::getAssetId, assetId)
                .eq(TransferRequest::getStatus, Const.TransferStatus.PENDING)
                .last("LIMIT 1"));
    }

    // ------------------------------------------------------------------ 内部

    private TransferRequest requirePending(Long id, Long currentUserId) {
        TransferRequest t = transferMapper.selectById(id);
        if (t == null) {
            throw new BizException(ApiCode.NOT_FOUND, "转交申请不存在");
        }
        if (!Const.TransferStatus.PENDING.equals(t.getStatus())) {
            throw new BizException(ApiCode.CONFLICT, "该转交申请已处理，请刷新后重试");
        }
        if (!t.getFromUserId().equals(currentUserId)) {
            throw new BizException(ApiCode.FORBIDDEN, "只有当前持有者可以审批该转交申请");
        }
        return t;
    }

    private String nameOf(Long userId) {
        if (userId == null) {
            return null;
        }
        SysUser u = userMapper.selectById(userId);
        return u == null ? null : u.getRealName();
    }

    private Map<Long, String> namesOf(Collection<TransferRequest> list) {
        Set<Long> ids = new HashSet<>();
        for (TransferRequest t : list) {
            ids.add(t.getFromUserId());
            ids.add(t.getToUserId());
        }
        Map<Long, String> map = new HashMap<>();
        if (ids.isEmpty()) {
            return map;
        }
        for (SysUser u : userMapper.selectBatchIds(ids)) {
            map.put(u.getId(), u.getRealName());
        }
        return map;
    }

    private Map<Long, Asset> assetsOf(Collection<TransferRequest> list) {
        Set<Long> ids = new HashSet<>();
        for (TransferRequest t : list) {
            ids.add(t.getAssetId());
        }
        Map<Long, Asset> map = new HashMap<>();
        if (ids.isEmpty()) {
            return map;
        }
        for (Asset a : assetMapper.selectBatchIds(ids)) {
            map.put(a.getId(), a);
        }
        return map;
    }

    private TransferVO toVO(TransferRequest t, Asset asset, String fromName, String toName) {
        return new TransferVO(t.getId(),
                t.getAssetId(),
                asset == null ? null : asset.getCode(),
                asset == null ? null : asset.getName(),
                t.getFromUserId(), fromName,
                t.getToUserId(), toName,
                t.getStatus(), t.getApplyReason(), t.getRejectReason(),
                t.getCreatedAt(), t.getDecidedAt());
    }

    private long clampSize(long size) {
        return Math.max(1, Math.min(size, 100));
    }

    /** 供其它服务复用：把某资产全部待审批转交置为取消（归还时清理悬挂单据）。 */
    public void cancelPendingOfAsset(Long assetId) {
        transferMapper.update(null, new LambdaUpdateWrapper<TransferRequest>()
                .set(TransferRequest::getStatus, Const.TransferStatus.CANCELLED)
                .set(TransferRequest::getDecidedAt, LocalDateTime.now())
                .eq(TransferRequest::getAssetId, assetId)
                .eq(TransferRequest::getStatus, Const.TransferStatus.PENDING));
    }
}
