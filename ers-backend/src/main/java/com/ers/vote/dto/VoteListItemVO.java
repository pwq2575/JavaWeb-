package com.ers.vote.dto;

import java.time.LocalDateTime;

/**
 * 投票列表项（API 契约 §8 #32）。
 */
public record VoteListItemVO(
        Long id,
        String title,
        String status,
        LocalDateTime deadline,
        long totalVotes,
        boolean voted) {
}
