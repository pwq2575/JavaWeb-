package com.ers.credit;

import java.time.LocalDate;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ers.credit.entity.CreditLog;
import com.ers.credit.mapper.CreditLogMapper;
import com.ers.user.entity.SysUser;
import com.ers.user.mapper.SysUserMapper;

/**
 * 信誉分服务（PRD 4.3）：
 * <ul>
 *   <li>资产逾期：每日 -5，按 (user, reason, ref_type, ref_id, biz_date) 唯一键去重；</li>
 *   <li>会议室爽约：一次性 -5；</li>
 *   <li>下限 0；扣分不回补（续借仅"停止继续扣"）。</li>
 * </ul>
 */
@Service
public class CreditService {

    private static final Logger log = LoggerFactory.getLogger(CreditService.class);
    private static final int PENALTY = -5;

    private final CreditLogMapper creditLogMapper;
    private final SysUserMapper userMapper;

    public CreditService(CreditLogMapper creditLogMapper, SysUserMapper userMapper) {
        this.creditLogMapper = creditLogMapper;
        this.userMapper = userMapper;
    }

    /**
     * 幂等扣分：同一 (user, reason, refType, refId, bizDate) 只生效一次。
     *
     * @return true 表示本次实际扣分，false 表示当日已扣过被跳过
     */
    public boolean deductOnce(Long userId, String reason, String refType, Long refId, LocalDate bizDate, String remark) {
        CreditLog logEntity = new CreditLog();
        logEntity.setUserId(userId);
        logEntity.setDelta(PENALTY);
        logEntity.setReason(reason);
        logEntity.setRefType(refType);
        logEntity.setRefId(refId);
        logEntity.setBizDate(bizDate);
        logEntity.setRemark(remark);
        try {
            creditLogMapper.insert(logEntity);
        } catch (DuplicateKeyException e) {
            log.debug("信誉扣分已存在，跳过 userId={} reason={} refId={} date={}", userId, reason, refId, bizDate);
            return false;
        }
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            return true;
        }
        int next = Math.max(0, (user.getCreditScore() == null ? 100 : user.getCreditScore()) + PENALTY);
        SysUser update = new SysUser();
        update.setId(userId);
        update.setCreditScore(next);
        userMapper.updateById(update);
        return true;
    }

    public int scoreOf(Long userId) {
        SysUser user = userMapper.selectById(userId);
        return user == null || user.getCreditScore() == null ? 0 : user.getCreditScore();
    }

    /** 个人信誉流水（倒序）。 */
    public List<CreditLog> historyOf(Long userId, int limit) {
        return creditLogMapper.selectList(Wrappers.<CreditLog>lambdaQuery()
                .eq(CreditLog::getUserId, userId)
                .orderByDesc(CreditLog::getCreatedAt)
                .last("LIMIT " + Math.max(1, Math.min(limit, 200))));
    }
}
