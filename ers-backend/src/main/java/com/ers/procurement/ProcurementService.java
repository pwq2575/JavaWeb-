package com.ers.procurement;

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
import com.ers.common.ApiCode;
import com.ers.common.BizException;
import com.ers.common.Const;
import com.ers.common.PageResult;
import com.ers.procurement.dto.ProcurementCreateRequest;
import com.ers.procurement.dto.ProcurementVO;
import com.ers.procurement.entity.Procurement;
import com.ers.procurement.mapper.ProcurementMapper;
import com.ers.user.entity.SysUser;
import com.ers.user.mapper.SysUserMapper;

/**
 * 采购审批（PRD 4.5，IA B2）：二态审批，**同意入库不写资产台账**；驳回理由必填；已驳回可本人删除（软删）。
 */
@Service
public class ProcurementService {

    private final ProcurementMapper procurementMapper;
    private final SysUserMapper userMapper;

    public ProcurementService(ProcurementMapper procurementMapper, SysUserMapper userMapper) {
        this.procurementMapper = procurementMapper;
        this.userMapper = userMapper;
    }

    public PageResult<ProcurementVO> list(long page, long size, String status, Long currentUserId, boolean admin) {
        LambdaQueryWrapper<Procurement> qw = Wrappers.<Procurement>lambdaQuery()
                .isNull(Procurement::getDeletedAt);
        if (!admin) {
            qw.eq(Procurement::getApplicantId, currentUserId);
        }
        if (status != null && !status.isBlank()) {
            qw.eq(Procurement::getStatus, status.trim());
        }
        qw.orderByDesc(Procurement::getCreatedAt);
        Page<Procurement> p = procurementMapper.selectPage(new Page<>(Math.max(1, page), clampSize(size)), qw);

        Set<Long> userIds = new HashSet<>();
        for (Procurement item : p.getRecords()) {
            userIds.add(item.getApplicantId());
            if (item.getApproverId() != null) {
                userIds.add(item.getApproverId());
            }
        }
        Map<Long, String> names = new HashMap<>();
        if (!userIds.isEmpty()) {
            for (SysUser u : userMapper.selectBatchIds(userIds)) {
                names.put(u.getId(), u.getRealName());
            }
        }
        List<ProcurementVO> records = new ArrayList<>(p.getRecords().size());
        for (Procurement item : p.getRecords()) {
            records.add(toVO(item, names, currentUserId));
        }
        return new PageResult<>(records, p.getTotal(), p.getCurrent(), p.getSize());
    }

    @Transactional
    public ProcurementVO create(ProcurementCreateRequest req, Long currentUserId) {
        Procurement p = new Procurement();
        p.setTitle(req.title().trim());
        p.setReason(req.reason().trim());
        p.setStatus(Const.ProcurementStatus.PENDING);
        p.setApplicantId(currentUserId);
        p.setCreatedAt(LocalDateTime.now());
        procurementMapper.insert(p);
        Map<Long, String> names = new HashMap<>();
        SysUser me = userMapper.selectById(currentUserId);
        if (me != null) {
            names.put(me.getId(), me.getRealName());
        }
        return toVO(p, names, currentUserId);
    }

    @Transactional
    public ProcurementVO approve(Long id, String note, Long approverId) {
        Procurement p = requirePending(id);
        applyDecision(p, Const.ProcurementStatus.APPROVED, note, approverId);
        return toVO(p, namesOf(p), approverId);
    }

    @Transactional
    public ProcurementVO reject(Long id, String note, Long approverId) {
        if (note == null || note.isBlank()) {
            throw new BizException(ApiCode.BAD_REQUEST, "请填写驳回理由");
        }
        Procurement p = requirePending(id);
        applyDecision(p, Const.ProcurementStatus.REJECTED, note, approverId);
        return toVO(p, namesOf(p), approverId);
    }

    @Transactional
    public void delete(Long id, Long currentUserId, boolean admin) {
        Procurement p = procurementMapper.selectById(id);
        if (p == null || p.getDeletedAt() != null) {
            throw new BizException(ApiCode.NOT_FOUND, "采购申请不存在");
        }
        if (!admin && !p.getApplicantId().equals(currentUserId)) {
            throw new BizException(ApiCode.FORBIDDEN, "只能删除自己提交的申请");
        }
        if (!Const.ProcurementStatus.REJECTED.equals(p.getStatus())) {
            throw new BizException(ApiCode.UNPROCESSABLE, "只能删除已驳回的申请");
        }
        procurementMapper.update(null, new LambdaUpdateWrapper<Procurement>()
                .set(Procurement::getDeletedAt, LocalDateTime.now())
                .eq(Procurement::getId, id));
    }

    /** 待审批数量（管理员看板 / 侧栏角标）。 */
    public long pendingCount() {
        Long c = procurementMapper.selectCount(Wrappers.<Procurement>lambdaQuery()
                .isNull(Procurement::getDeletedAt)
                .eq(Procurement::getStatus, Const.ProcurementStatus.PENDING));
        return c == null ? 0 : c;
    }

    // ------------------------------------------------------------------ 内部

    private void applyDecision(Procurement p, String status, String note, Long approverId) {
        int rows = procurementMapper.update(null, new LambdaUpdateWrapper<Procurement>()
                .set(Procurement::getStatus, status)
                .set(Procurement::getApproverId, approverId)
                .set(Procurement::getApproveNote, note == null ? null : note.trim())
                .set(Procurement::getDecidedAt, LocalDateTime.now())
                .eq(Procurement::getId, p.getId())
                .eq(Procurement::getStatus, Const.ProcurementStatus.PENDING));
        if (rows == 0) {
            throw new BizException(ApiCode.CONFLICT, "该申请已被处理，请刷新后重试");
        }
        p.setStatus(status);
        p.setApproverId(approverId);
        p.setApproveNote(note == null ? null : note.trim());
        p.setDecidedAt(LocalDateTime.now());
    }

    private Procurement requirePending(Long id) {
        Procurement p = id == null ? null : procurementMapper.selectById(id);
        if (p == null || p.getDeletedAt() != null) {
            throw new BizException(ApiCode.NOT_FOUND, "采购申请不存在");
        }
        if (!Const.ProcurementStatus.PENDING.equals(p.getStatus())) {
            throw new BizException(ApiCode.CONFLICT, "该申请已被处理，请刷新后重试");
        }
        return p;
    }

    private Map<Long, String> namesOf(Procurement p) {
        Set<Long> ids = new HashSet<>();
        ids.add(p.getApplicantId());
        if (p.getApproverId() != null) {
            ids.add(p.getApproverId());
        }
        Map<Long, String> names = new HashMap<>();
        for (SysUser u : userMapper.selectBatchIds(ids)) {
            names.put(u.getId(), u.getRealName());
        }
        return names;
    }

    private ProcurementVO toVO(Procurement p, Map<Long, String> names, Long currentUserId) {
        boolean canDelete = Const.ProcurementStatus.REJECTED.equals(p.getStatus())
                && (currentUserId != null && currentUserId.equals(p.getApplicantId()));
        return new ProcurementVO(p.getId(), p.getTitle(), p.getReason(), p.getStatus(),
                p.getApplicantId(), names.get(p.getApplicantId()),
                p.getApproverId() == null ? null : names.get(p.getApproverId()),
                p.getApproveNote(), p.getCreatedAt(), p.getDecidedAt(), canDelete);
    }

    private long clampSize(long size) {
        return Math.max(1, Math.min(size, 100));
    }

    /** 供其它服务统计使用。 */
    public List<Procurement> pendingList() {
        return procurementMapper.selectList(Wrappers.<Procurement>lambdaQuery()
                .isNull(Procurement::getDeletedAt)
                .eq(Procurement::getStatus, Const.ProcurementStatus.PENDING));
    }

    static Collection<Procurement> unused() {
        return List.of();
    }
}
