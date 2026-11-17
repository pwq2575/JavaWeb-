package com.ers.vote.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 提交投票（API 契约 §8 #34：每人一次，不可修改）。
 */
public record VoteSubmitRequest(@NotNull(message = "请选择一个方案") Long optionId) {
}
