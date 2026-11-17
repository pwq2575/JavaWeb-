package com.ers.vote.dto;

/**
 * 投票选项（API 契约 §8 #33）：未到可见时机时 votes/percent 为 null。
 */
public record VoteOptionVO(Long id, String label, Long votes, Double percent) {
}
