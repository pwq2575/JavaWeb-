package com.ers.vote.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 投票详情（API 契约 §8 #33）。
 * <p>IA §6.9：投前隐藏票数（showResult=false 时 votes/percent 为 null）。</p>
 */
public record VoteDetailVO(
        Long id,
        String title,
        String description,
        String status,
        LocalDateTime deadline,
        long totalVotes,
        boolean votable,
        Long myOptionId,
        boolean showResult,
        List<VoteOptionVO> options) {
}
