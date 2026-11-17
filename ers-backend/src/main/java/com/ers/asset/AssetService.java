package com.ers.asset;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ers.asset.dto.AssetDetailVO;
import com.ers.asset.dto.AssetRecordVO;
import com.ers.asset.dto.AssetSaveRequest;
import com.ers.asset.dto.AssetVO;
import com.ers.asset.entity.Asset;
import com.ers.asset.entity.AssetRecord;
import com.ers.asset.entity.TransferRequest;
import com.ers.asset.mapper.AssetMapper;
import com.ers.asset.mapper.AssetRecordMapper;
import com.ers.asset.mapper.TransferRequestMapper;
import com.ers.common.ApiCode;
import com.ers.common.BizException;
import com.ers.common.Const;
import com.ers.common.PageResult;
import com.ers.config.ConfigService;
import com.ers.credit.CreditService;
import com.ers.user.entity.SysUser;
import com.ers.user.mapper.SysUserMapper;

/**
 * 资产台账与流转（PRD 4.2）：列表 / 详情 / 借出 / 续借 / 归还 + 管理端增删改。
 * <p>并发控制：状态流转一律「条件更新」，影响行数为 0 视为冲突（API 契约 §1.7）。</p>
 */
@Service
public class AssetService {

    private final AssetMapper assetMapper;
    private final AssetRecordMapper recordMapper;
    private final TransferRequestMapper transferMapper;
    private final SysUserMapper userMapper;
    private final ConfigService configService;
    private final CreditService creditService;

    public AssetService(AssetMapper assetMapper, AssetRecordMapper recordMapper,
                        TransferRequestMapper transferMapper, SysUserMapper userMapper,
                        ConfigService configService, CreditService creditService) {
        this.assetMapper = assetMapper;
        this.recordMapper = recordMapper;
        this.transferMapper = transferMapper;
        this.userMapper = userMapper;
        this.configService = configService;
        this.creditService = creditService;
    }

    // ------------------------------------------------------------------ 查询

    public PageResult<AssetVO> list(long page, long size, String keyword, String status, String category,
                                    Long holderId, Long viewerId, boolean viewerAdmin) {
        LambdaQueryWrapper<Asset> qw = Wrappers.<Asset>lambdaQuery();
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            qw.and(w -> w.like(Asset::getName, kw).or().like(Asset::getCode, kw));
        }
        if (status != null && !status.isBlank()) {
            qw.eq(Asset::getStatus, status.trim());
        }
        if (category != null && !category.isBlank()) {
            qw.eq(Asset::getCategory, category.trim());
        }
        if (holderId != null) {
            qw.eq(Asset::getHolderId, holderId);
        }
        qw.orderByAsc(Asset::getCode);

        Page<Asset> p = assetMapper.selectPage(new Page<>(Math.max(1, page), Math.max(1, Math.min(size, 100))), qw);
        Map<Long, String> names = userNames(collectHolderIds(p.getRecords()));
        Map<Long, Long> pending = pendingTransferByAsset(p.getRecords().stream().map(Asset::getId).toList());
        List<AssetVO> records = new ArrayList<>(p.getRecords().size());
        for (Asset a : p.getRecords()) {
            records.add(toVO(a, names, pending.get(a.getId()), viewerId, viewerAdmin));
        }
        return new PageResult<>(records, p.getTotal(), p.getCurrent(), p.getSize());
    }

    public AssetDetailVO detail(Long id, Long viewerId, boolean viewerAdmin) {
        Asset asset = requireAsset(id);
        Map<Long, String> names = userNames(collectUserIds(asset));
        List<AssetRecordVO> records = new ArrayList<>();
        for (AssetRecord r : timeline(id)) {
            records.add(new AssetRecordVO(r.getId(), r.getAction(), r.getOperatorId(),
                    names.get(r.getOperatorId()), r.getTargetUserId(), names.get(r.getTargetUserId()),
                    r.getFromStatus(), r.getToStatus(), r.getRemark(), r.getHappenedAt()));
        }
        TransferRequest pending = pendingTransferOf(id);
        AssetDetailVO.TransferBrief brief = null;
        if (pending != null) {
            Map<Long, String> pn = userNames(List.of(pending.getFromUserId(), pending.getToUserId()));
            brief = new AssetDetailVO.TransferBrief(pending.getId(), pending.getFromUserId(),
                    pn.get(pending.getFromUserId()), pending.getToUserId(), pn.get(pending.getToUserId()),
                    pending.getApplyReason(), pending.getCreatedAt());
        }
        return new AssetDetailVO(toVO(asset, names, pending == null ? null : pending.getId(), viewerId, viewerAdmin), records, brief);
    }

    /** 流转时间线（倒序）。 */
    public List<AssetRecord> timeline(Long assetId) {
        return recordMapper.selectList(Wrappers.<AssetRecord>lambdaQuery()
                .eq(AssetRecord::getAssetId, assetId)
                .orderByDesc(AssetRecord::getHappenedAt)
                .orderByDesc(AssetRecord::getId));
    }

    // ------------------------------------------------------------------ 业务动作

    /** 借出：固定借期 borrow.days 天（默认 3），高价值资产受信誉阈值限制（PRD 4.2 / 4.3）。 */
    @Transactional
    public AssetVO borrow(Long assetId, Long userId) {
        Asset asset = requireAsset(assetId);
        if (Const.AssetStatus.DISABLED.equals(asset.getStatus())) {
            throw BizException.conflict("资产已停用，无法借出");
        }
        if (Const.AssetStatus.BORROWED.equals(asset.getStatus())) {
            throw BizException.conflict("资产当前已被借出，无法重复借出");
        }
        int highValue = configService.highValueThreshold();
        int threshold = configService.creditThreshold();
        BigDecimal value = asset.getValue() == null ? BigDecimal.ZERO : asset.getValue();
        boolean high = value.compareTo(BigDecimal.valueOf(highValue)) >= 0;
        if (high) {
            int score = creditService.scoreOf(userId);
            if (score < threshold) {
                throw new BizException(ApiCode.CREDIT_INSUFFICIENT,
                        "信誉分不足（当前 " + score + " 分，低于阈值 " + threshold + " 分），暂不能借出高价值资产");
            }
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDate due = LocalDate.now().plusDays(configService.borrowDays());

        LambdaUpdateWrapper<Asset> uw = Wrappers.<Asset>lambdaUpdate()
                .eq(Asset::getId, assetId)
                .eq(Asset::getStatus, Const.AssetStatus.IN_STOCK)
                .set(Asset::getStatus, Const.AssetStatus.BORROWED)
                .set(Asset::getHolderId, userId)
                .set(Asset::getBorrowAt, now)
                .set(Asset::getDueDate, due)
                .set(Asset::getRenewCount, 0)
                .set(Asset::getUpdatedAt, now);
        if (assetMapper.update(null, uw) == 0) {
            throw BizException.conflict("资产状态已变化，请刷新后重试");
        }
        writeRecord(assetId, Const.AssetAction.BORROW, userId, null,
                Const.AssetStatus.IN_STOCK, Const.AssetStatus.BORROWED,
                "借出，应归还日 " + due);
        return toVO(requireAsset(assetId), userNames(collectUserIds(asset)), null, userId, false);
    }

    /** 续借：+1 天/次，最多 renew.max.times 次；有待审批转交时不可续借；允许逾期后续借（止损，PRD D10）。 */
    @Transactional
    public AssetVO renew(Long assetId, Long userId) {
        Asset asset = requireAsset(assetId);
        if (!Const.AssetStatus.BORROWED.equals(asset.getStatus())) {
            throw BizException.conflict("资产当前不在借出状态，无法续借");
        }
        if (!Objects.equals(asset.getHolderId(), userId)) {
            throw BizException.forbidden("只有当前持有者可以续借");
        }
        int max = configService.renewMaxTimes();
        int used = asset.getRenewCount() == null ? 0 : asset.getRenewCount();
        if (used >= max) {
            throw new BizException("续借次数已达上限（" + max + " 次）");
        }
        if (pendingTransferOf(assetId) != null) {
            throw new BizException("该资产存在待审批的转交申请，暂不可续借");
        }
        LocalDate newDue = asset.getDueDate().plusDays(1);
        LambdaUpdateWrapper<Asset> uw = Wrappers.<Asset>lambdaUpdate()
                .eq(Asset::getId, assetId)
                .eq(Asset::getStatus, Const.AssetStatus.BORROWED)
                .eq(Asset::getHolderId, userId)
                .eq(Asset::getRenewCount, used)
                .set(Asset::getDueDate, newDue)
                .set(Asset::getRenewCount, used + 1)
                .set(Asset::getUpdatedAt, LocalDateTime.now());
        if (assetMapper.update(null, uw) == 0) {
            throw BizException.conflict("资产状态已变化，请刷新后重试");
        }
        return toVO(requireAsset(assetId), userNames(collectUserIds(asset)), null, userId, false);
    }

    /** 归还：仅当前持有者或管理员；归还后回到在库并清空借用信息。 */
    @Transactional
    public AssetVO returnAsset(Long assetId, Long operatorId, boolean operatorAdmin) {
        Asset asset = requireAsset(assetId);
        if (!Const.AssetStatus.BORROWED.equals(asset.getStatus())) {
            throw BizException.conflict("资产当前不在借出状态，无需归还");
        }
        Long holderId = asset.getHolderId();
        if (!operatorAdmin && !Objects.equals(holderId, operatorId)) {
            throw BizException.forbidden("只有当前持有者可以归还该资产");
        }
        boolean overdue = asset.getDueDate() != null && asset.getDueDate().isBefore(LocalDate.now());
        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<Asset> uw = Wrappers.<Asset>lambdaUpdate()
                .eq(Asset::getId, assetId)
                .eq(Asset::getStatus, Const.AssetStatus.BORROWED)
                .eq(Asset::getHolderId, holderId)
                .set(Asset::getStatus, Const.AssetStatus.IN_STOCK)
                .set(Asset::getHolderId, null)
                .set(Asset::getBorrowAt, null)
                .set(Asset::getDueDate, null)
                .set(Asset::getRenewCount, 0)
                .set(Asset::getUpdatedAt, now);
        if (assetMapper.update(null, uw) == 0) {
            throw BizException.conflict("资产状态已变化，请刷新后重试");
        }
        // 归还后，该资产的待审批转交自动取消，避免悬挂单据
        transferMapper.update(null, Wrappers.<TransferRequest>lambdaUpdate()
                .eq(TransferRequest::getAssetId, assetId)
                .eq(TransferRequest::getStatus, Const.TransferStatus.PENDING)
                .set(TransferRequest::getStatus, Const.TransferStatus.CANCELLED)
                .set(TransferRequest::getDecidedAt, now));
        writeRecord(assetId, Const.AssetAction.RETURN, operatorId, holderId,
                Const.AssetStatus.BORROWED, Const.AssetStatus.IN_STOCK,
                overdue ? "逾期归还" : "归还");
        return toVO(requireAsset(assetId), userNames(collectUserIds(asset)), null, operatorId, operatorAdmin);
    }

    /** 我的持有资产（P-ME-01）。 */
    public List<AssetVO> myBorrows(Long userId) {
        List<Asset> assets = assetMapper.selectList(Wrappers.<Asset>lambdaQuery()
                .eq(Asset::getHolderId, userId)
                .orderByAsc(Asset::getDueDate));
        Map<Long, String> names = userNames(collectHolderIds(assets));
        Map<Long, Long> pending = pendingTransferByAsset(assets.stream().map(Asset::getId).toList());
        List<AssetVO> list = new ArrayList<>(assets.size());
        for (Asset a : assets) {
            list.add(toVO(a, names, pending.get(a.getId()), userId, false));
        }
        return list;
    }

    // ------------------------------------------------------------------ 管理端 CRUD

    @Transactional
    public AssetVO create(AssetSaveRequest req) {
        Asset asset = new Asset();
        asset.setCode(req.getCode().trim());
        asset.setName(req.getName().trim());
        asset.setCategory(req.getCategory().trim());
        asset.setValue(req.getValue() == null ? BigDecimal.ZERO : req.getValue());
        asset.setStatus(Const.AssetStatus.IN_STOCK);
        asset.setRenewCount(0);
        asset.setDescription(req.getDescription());
        try {
            assetMapper.insert(asset);
        } catch (DuplicateKeyException e) {
            throw BizException.conflict("资产编号已存在：" + req.getCode());
        }
        // 回查一次，补齐数据库默认值（created_at / updated_at）
        Asset saved = assetMapper.selectById(asset.getId());
        return toVO(saved == null ? asset : saved, Map.of(), null, null, true);
    }

    @Transactional
    public AssetVO update(Long id, AssetSaveRequest req) {
        Asset asset = requireAsset(id);
        if (Const.AssetStatus.BORROWED.equals(asset.getStatus())) {
            throw BizException.conflict("资产借出中，请先归还再修改");
        }
        LambdaUpdateWrapper<Asset> uw = Wrappers.<Asset>lambdaUpdate()
                .eq(Asset::getId, id)
                .set(Asset::getCode, req.getCode().trim())
                .set(Asset::getName, req.getName().trim())
                .set(Asset::getCategory, req.getCategory().trim())
                .set(Asset::getValue, req.getValue() == null ? BigDecimal.ZERO : req.getValue())
                .set(Asset::getDescription, req.getDescription())
                .set(Asset::getUpdatedAt, LocalDateTime.now());
        try {
            assetMapper.update(null, uw);
        } catch (DuplicateKeyException e) {
            throw BizException.conflict("资产编号已存在：" + req.getCode());
        }
        return toVO(requireAsset(id), Map.of(), null, null, true);
    }

    /** 删除：仅限从未产生流转记录的资产，否则改用停用（PRD 4.2(7)）。 */
    @Transactional
    public void delete(Long id) {
        Asset asset = requireAsset(id);
        Long count = recordMapper.selectCount(Wrappers.<AssetRecord>lambdaQuery().eq(AssetRecord::getAssetId, id));
        if (count != null && count > 0) {
            throw BizException.conflict("该资产已有流转记录，不能删除，请改为停用");
        }
        assetMapper.deleteById(asset.getId());
    }

    /** 启用/停用（停用不影响已发生的历史记录）。 */
    @Transactional
    public AssetVO setEnabled(Long id, boolean enabled) {
        Asset asset = requireAsset(id);
        if (enabled && !Const.AssetStatus.DISABLED.equals(asset.getStatus())) {
            return toVO(asset, Map.of(), null, null, true);
        }
        if (enabled) {
            LambdaUpdateWrapper<Asset> uw = Wrappers.<Asset>lambdaUpdate()
                    .eq(Asset::getId, id)
                    .eq(Asset::getStatus, Const.AssetStatus.DISABLED)
                    .set(Asset::getStatus, Const.AssetStatus.IN_STOCK)
                    .set(Asset::getUpdatedAt, LocalDateTime.now());
            assetMapper.update(null, uw);
            return toVO(requireAsset(id), Map.of(), null, null, true);
        }
        if (Const.AssetStatus.BORROWED.equals(asset.getStatus())) {
            throw BizException.conflict("资产借出中，不能停用");
        }
        LambdaUpdateWrapper<Asset> uw = Wrappers.<Asset>lambdaUpdate()
                .eq(Asset::getId, id)
                .ne(Asset::getStatus, Const.AssetStatus.DISABLED)
                .set(Asset::getStatus, Const.AssetStatus.DISABLED)
                .set(Asset::getUpdatedAt, LocalDateTime.now());
        assetMapper.update(null, uw);
        return toVO(requireAsset(id), Map.of(), null, null, true);
    }

    // ------------------------------------------------------------------ 内部工具

    public Asset requireAsset(Long id) {
        Asset asset = id == null ? null : assetMapper.selectById(id);
        if (asset == null) {
            throw BizException.notFound("资产不存在");
        }
        return asset;
    }

    public TransferRequest pendingTransferOf(Long assetId) {
        return transferMapper.selectOne(Wrappers.<TransferRequest>lambdaQuery()
                .eq(TransferRequest::getAssetId, assetId)
                .eq(TransferRequest::getStatus, Const.TransferStatus.PENDING)
                .last("LIMIT 1"));
    }

    private Map<Long, Long> pendingTransferByAsset(Collection<Long> assetIds) {
        if (assetIds == null || assetIds.isEmpty()) {
            return Map.of();
        }
        List<TransferRequest> list = transferMapper.selectList(Wrappers.<TransferRequest>lambdaQuery()
                .in(TransferRequest::getAssetId, assetIds)
                .eq(TransferRequest::getStatus, Const.TransferStatus.PENDING));
        Map<Long, Long> map = new HashMap<>();
        for (TransferRequest t : list) {
            map.put(t.getAssetId(), t.getId());
        }
        return map;
    }

    void writeRecord(Long assetId, String action, Long operatorId, Long targetUserId,
                     String fromStatus, String toStatus, String remark) {
        AssetRecord record = new AssetRecord();
        record.setAssetId(assetId);
        record.setAction(action);
        record.setOperatorId(operatorId);
        record.setTargetUserId(targetUserId);
        record.setFromStatus(fromStatus);
        record.setToStatus(toStatus);
        record.setRemark(remark);
        record.setHappenedAt(LocalDateTime.now());
        recordMapper.insert(record);
    }

    private List<Long> collectHolderIds(List<Asset> assets) {
        return assets.stream().map(Asset::getHolderId).filter(Objects::nonNull).distinct().toList();
    }

    private List<Long> collectUserIds(Asset asset) {
        List<Long> ids = new ArrayList<>();
        if (asset.getHolderId() != null) {
            ids.add(asset.getHolderId());
        }
        return ids;
    }

    /** 批量取用户姓名，供列表/详情展示（避免 N+1）。 */
    Map<Long, String> userNames(Collection<Long> ids) {
        Map<Long, String> map = new HashMap<>();
        if (ids == null || ids.isEmpty()) {
            return map;
        }
        for (SysUser u : userMapper.selectBatchIds(ids.stream().filter(Objects::nonNull).distinct().toList())) {
            map.put(u.getId(), u.getRealName());
        }
        return map;
    }

    AssetVO toVO(Asset a, Map<Long, String> names, Long pendingTransferId) {
        return toVO(a, names, pendingTransferId, null, false);
    }

    AssetVO toVO(Asset a, Map<Long, String> names, Long pendingTransferId, Long viewerId, boolean viewerAdmin) {
        LocalDate today = LocalDate.now();
        boolean borrowed = Const.AssetStatus.BORROWED.equals(a.getStatus());
        Long daysLeft = a.getDueDate() == null ? null : ChronoUnit.DAYS.between(today, a.getDueDate());
        boolean overdue = borrowed && a.getDueDate() != null && a.getDueDate().isBefore(today);
        boolean isHolder = viewerId != null && Objects.equals(viewerId, a.getHolderId());
        int max = configService.renewMaxTimes();
        int used = a.getRenewCount() == null ? 0 : a.getRenewCount();
        boolean canRenew = borrowed && isHolder && used < max && pendingTransferId == null;
        boolean canReturn = borrowed && (isHolder || viewerAdmin);
        boolean canBorrow = Const.AssetStatus.IN_STOCK.equals(a.getStatus()) && viewerId != null;
        BigDecimal value = a.getValue() == null ? BigDecimal.ZERO : a.getValue();
        boolean high = value.compareTo(BigDecimal.valueOf(configService.highValueThreshold())) >= 0;
        String holderName = a.getHolderId() == null ? null : names.get(a.getHolderId());
        return new AssetVO(a.getId(), a.getCode(), a.getName(), a.getCategory(), value, a.getStatus(),
                a.getHolderId(), holderName, a.getBorrowAt(), a.getDueDate(), used,
                daysLeft, overdue, a.getDescription(), high, canBorrow, canRenew, canReturn,
                pendingTransferId, a.getCreatedAt());
    }
}
