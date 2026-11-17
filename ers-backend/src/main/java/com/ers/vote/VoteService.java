package com.ers.vote;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.dao.DuplicateKeyException;
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
import com.ers.security.UserContext;
import com.ers.vote.dto.VoteDetailVO;
import com.ers.vote.dto.VoteListItemVO;
import com.ers.vote.dto.VoteOptionVO;
import com.ers.vote.dto.VoteSaveRequest;
import com.ers.vote.entity.VoteOption;
import com.ers.vote.entity.VoteRecord;
import com.ers.vote.entity.VoteTopic;
import com.ers.vote.mapper.VoteOptionMapper;
import com.ers.vote.mapper.VoteRecordMapper;
import com.ers.vote.mapper.VoteTopicMapper;

/**
 * 文化投票（PRD 4.6，IA §6.9）：管理员发布方案+截止时间，每人一次不可改，到点自动停收（读时惰性关闭），
 * 投前隐藏票数（Q4 已决：投票后与截止后可见）。
 */
@Service
public class VoteService {

    private final VoteTopicMapper topicMapper;
    private final VoteOptionMapper optionMapper;
    private final VoteRecordMapper recordMapper;

    public VoteService(VoteTopicMapper topicMapper, VoteOptionMapper optionMapper, VoteRecordMapper recordMapper) {
        this.topicMapper = topicMapper;
        this.optionMapper = optionMapper;
        this.recordMapper = recordMapper;
    }

    /** #32 列表：普通用户看 PUBLISHED/CLOSED；管理员可加 status 过滤看全部（含 DRAFT）。 */
    public PageResult<VoteListItemVO> list(long page, long size, String status, boolean admin) {
        closeExpired();
        LambdaQueryWrapper<VoteTopic> qw = Wrappers.<VoteTopic>lambdaQuery();
        if (status != null && !status.isBlank()) {
            qw.eq(VoteTopic::getStatus, status.trim());
            if (!admin) {
                qw.ne(VoteTopic::getStatus, Const.VoteStatus.DRAFT);
            }
        } else if (!admin) {
            qw.in(VoteTopic::getStatus, Const.VoteStatus.PUBLISHED, Const.VoteStatus.CLOSED);
        }
        qw.orderByDesc(VoteTopic::getCreatedAt);
        Page<VoteTopic> p = topicMapper.selectPage(new Page<>(Math.max(1, page), clampSize(size)), qw);

        Long me = UserContext.userId();
        List<VoteListItemVO> records = new ArrayList<>();
        for (VoteTopic t : p.getRecords()) {
            long total = countVotes(t.getId());
            boolean voted = me != null && myOption(t.getId(), me) != null;
            records.add(new VoteListItemVO(t.getId(), t.getTitle(), t.getStatus(), t.getDeadline(), total, voted));
        }
        return new PageResult<>(records, p.getTotal(), p.getCurrent(), p.getSize());
    }

    /** #33 详情。 */
    public VoteDetailVO detail(Long id) {
        closeExpired();
        VoteTopic t = requireTopic(id);
        Long me = UserContext.userId();
        if (Const.VoteStatus.DRAFT.equals(t.getStatus()) && !UserContext.isAdmin()) {
            throw new BizException(ApiCode.NOT_FOUND, "投票活动不存在");
        }
        List<VoteOption> options = optionMapper.selectList(Wrappers.<VoteOption>lambdaQuery()
                .eq(VoteOption::getTopicId, id)
                .orderByAsc(VoteOption::getSortNo, VoteOption::getId));
        Map<Long, Long> counts = new HashMap<>();
        for (VoteRecord r : recordMapper.selectList(Wrappers.<VoteRecord>lambdaQuery()
                .eq(VoteRecord::getTopicId, id))) {
            counts.merge(r.getOptionId(), 1L, Long::sum);
        }
        long total = counts.values().stream().mapToLong(Long::longValue).sum();
        Long myOption = me == null ? null : myOption(id, me);
        boolean closed = Const.VoteStatus.CLOSED.equals(t.getStatus());
        boolean votable = Const.VoteStatus.PUBLISHED.equals(t.getStatus())
                && !closed && t.getDeadline() != null && t.getDeadline().isAfter(LocalDateTime.now())
                && myOption == null;
        boolean showResult = myOption != null || closed;
        List<VoteOptionVO> opts = new ArrayList<>();
        for (VoteOption o : options) {
            Long c = counts.getOrDefault(o.getId(), 0L);
            if (showResult) {
                double percent = total == 0 ? 0D : Math.round(c * 10000D / total) / 100D;
                opts.add(new VoteOptionVO(o.getId(), o.getLabel(), c, percent));
            } else {
                opts.add(new VoteOptionVO(o.getId(), o.getLabel(), null, null));
            }
        }
        return new VoteDetailVO(t.getId(), t.getTitle(), t.getDescription(), t.getStatus(), t.getDeadline(),
                total, votable, myOption, showResult, opts);
    }

    /** #34 投票：每人一次，不可修改。 */
    @Transactional
    public VoteDetailVO vote(Long id, Long optionId) {
        Long me = UserContext.requireUserId();
        closeExpired();
        VoteTopic t = requireTopic(id);
        if (!Const.VoteStatus.PUBLISHED.equals(t.getStatus())) {
            throw new BizException(ApiCode.UNPROCESSABLE, "投票已结束，无法提交");
        }
        if (t.getDeadline() != null && !t.getDeadline().isAfter(LocalDateTime.now())) {
            throw new BizException(ApiCode.UNPROCESSABLE, "投票已截止，无法提交");
        }
        VoteOption option = optionMapper.selectById(optionId);
        if (option == null || !option.getTopicId().equals(id)) {
            throw new BizException(ApiCode.BAD_REQUEST, "选项不属于该投票活动");
        }
        if (myOption(id, me) != null) {
            throw new BizException(ApiCode.CONFLICT, "你已投过票，不能修改。");
        }
        VoteRecord r = new VoteRecord();
        r.setTopicId(id);
        r.setOptionId(optionId);
        r.setUserId(me);
        r.setCreatedAt(LocalDateTime.now());
        try {
            recordMapper.insert(r);
        } catch (DuplicateKeyException e) {
            throw new BizException(ApiCode.CONFLICT, "你已投过票，不能修改。");
        }
        return detail(id);
    }

    // ------------------------------------------------------------- 管理端

    /** #35 新建草案。 */
    @Transactional
    public VoteDetailVO create(VoteSaveRequest req) {
        VoteTopic t = new VoteTopic();
        t.setTitle(req.title().trim());
        t.setDescription(trimToNull(req.description()));
        t.setDeadline(req.deadline());
        t.setStatus(Const.VoteStatus.DRAFT);
        t.setCreatedBy(UserContext.requireUserId());
        t.setCreatedAt(LocalDateTime.now());
        t.setUpdatedAt(LocalDateTime.now());
        topicMapper.insert(t);
        replaceOptions(t.getId(), req.options());
        return detail(t.getId());
    }

    /** #36 编辑草案（仅 DRAFT）。 */
    @Transactional
    public VoteDetailVO update(Long id, VoteSaveRequest req) {
        VoteTopic t = requireDraft(id);
        int rows = topicMapper.update(null, new LambdaUpdateWrapper<VoteTopic>()
                .set(VoteTopic::getTitle, req.title().trim())
                .set(VoteTopic::getDescription, trimToNull(req.description()))
                .set(VoteTopic::getDeadline, req.deadline())
                .set(VoteTopic::getUpdatedAt, LocalDateTime.now())
                .eq(VoteTopic::getId, id)
                .eq(VoteTopic::getStatus, Const.VoteStatus.DRAFT));
        if (rows == 0) {
            throw new BizException(ApiCode.CONFLICT, "该活动不是草稿状态，无法编辑");
        }
        replaceOptions(id, req.options());
        return detail(id);
    }

    /** #37 删除草案（仅 DRAFT；已发布只可关闭）。 */
    @Transactional
    public void delete(Long id) {
        requireDraft(id);
        optionMapper.delete(Wrappers.<VoteOption>lambdaQuery().eq(VoteOption::getTopicId, id));
        topicMapper.deleteById(id);
    }

    /** #38 发布（≥2 选项，截止时间须在未来）。 */
    @Transactional
    public VoteDetailVO publish(Long id) {
        VoteTopic t = requireDraft(id);
        long optionCount = optionMapper.selectCount(Wrappers.<VoteOption>lambdaQuery().eq(VoteOption::getTopicId, id));
        if (optionCount < 2) {
            throw new BizException(ApiCode.UNPROCESSABLE, "至少需要 2 个方案才能发布");
        }
        if (t.getDeadline() == null || !t.getDeadline().isAfter(LocalDateTime.now())) {
            throw new BizException(ApiCode.UNPROCESSABLE, "截止时间必须晚于当前时间");
        }
        int rows = topicMapper.update(null, new LambdaUpdateWrapper<VoteTopic>()
                .set(VoteTopic::getStatus, Const.VoteStatus.PUBLISHED)
                .set(VoteTopic::getPublishedAt, LocalDateTime.now())
                .set(VoteTopic::getUpdatedAt, LocalDateTime.now())
                .eq(VoteTopic::getId, id)
                .eq(VoteTopic::getStatus, Const.VoteStatus.DRAFT));
        if (rows == 0) {
            throw new BizException(ApiCode.CONFLICT, "该活动无法发布，请刷新后重试");
        }
        return detail(id);
    }

    /** #39 手动提前关闭。 */
    @Transactional
    public VoteDetailVO close(Long id) {
        VoteTopic t = requireTopic(id);
        if (Const.VoteStatus.CLOSED.equals(t.getStatus())) {
            return detail(id);
        }
        topicMapper.update(null, new LambdaUpdateWrapper<VoteTopic>()
                .set(VoteTopic::getStatus, Const.VoteStatus.CLOSED)
                .set(VoteTopic::getClosedAt, LocalDateTime.now())
                .set(VoteTopic::getUpdatedAt, LocalDateTime.now())
                .eq(VoteTopic::getId, id)
                .ne(VoteTopic::getStatus, Const.VoteStatus.DRAFT));
        return detail(id);
    }

    // ------------------------------------------------------------- 内部

    /** 到点自动停收：读取时惰性把已过截止时间的 PUBLISHED 置为 CLOSED。 */
    public void closeExpired() {
        topicMapper.update(null, new LambdaUpdateWrapper<VoteTopic>()
                .set(VoteTopic::getStatus, Const.VoteStatus.CLOSED)
                .set(VoteTopic::getClosedAt, LocalDateTime.now())
                .set(VoteTopic::getUpdatedAt, LocalDateTime.now())
                .eq(VoteTopic::getStatus, Const.VoteStatus.PUBLISHED)
                .lt(VoteTopic::getDeadline, LocalDateTime.now()));
    }

    private long countVotes(Long topicId) {
        Long c = recordMapper.selectCount(Wrappers.<VoteRecord>lambdaQuery().eq(VoteRecord::getTopicId, topicId));
        return c == null ? 0 : c;
    }

    private Long myOption(Long topicId, Long userId) {
        VoteRecord r = recordMapper.selectOne(Wrappers.<VoteRecord>lambdaQuery()
                .eq(VoteRecord::getTopicId, topicId)
                .eq(VoteRecord::getUserId, userId)
                .last("limit 1"));
        return r == null ? null : r.getOptionId();
    }

    private void replaceOptions(Long topicId, List<String> labels) {
        optionMapper.delete(Wrappers.<VoteOption>lambdaQuery().eq(VoteOption::getTopicId, topicId));
        int sort = 1;
        for (String label : labels) {
            VoteOption o = new VoteOption();
            o.setTopicId(topicId);
            o.setLabel(label.trim());
            o.setSortNo(sort++);
            optionMapper.insert(o);
        }
    }

    private VoteTopic requireTopic(Long id) {
        VoteTopic t = id == null ? null : topicMapper.selectById(id);
        if (t == null) {
            throw new BizException(ApiCode.NOT_FOUND, "投票活动不存在");
        }
        return t;
    }

    private VoteTopic requireDraft(Long id) {
        VoteTopic t = requireTopic(id);
        if (!Const.VoteStatus.DRAFT.equals(t.getStatus())) {
            throw new BizException(ApiCode.UNPROCESSABLE, "仅草稿状态的活动可以修改或删除");
        }
        return t;
    }

    private String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String v = s.trim();
        return v.isEmpty() ? null : v;
    }

    private long clampSize(long size) {
        return Math.max(1, Math.min(size, 100));
    }

    /** 供看板/管理统计：全部活动（含草稿）。 */
    public Map<String, Long> statusCounts() {
        Map<String, Long> m = new LinkedHashMap<>();
        for (String s : List.of(Const.VoteStatus.DRAFT, Const.VoteStatus.PUBLISHED, Const.VoteStatus.CLOSED)) {
            Long c = topicMapper.selectCount(Wrappers.<VoteTopic>lambdaQuery().eq(VoteTopic::getStatus, s));
            m.put(s, c == null ? 0 : c);
        }
        return m;
    }
}
