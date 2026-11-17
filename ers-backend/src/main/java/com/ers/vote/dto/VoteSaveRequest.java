package com.ers.vote.dto;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

/**
 * 新建 / 编辑投票草案（API 契约 §8 #35 / #36；D15：仅 DRAFT 可编辑，发布后只可关闭）。
 */
public record VoteSaveRequest(
        @NotBlank(message = "请填写活动标题")
        @Size(max = 64, message = "标题不超过 64 字") String title,
        @Size(max = 500, message = "活动说明不超过 500 字") String description,
        LocalDateTime deadline,
        @NotEmpty(message = "至少需要 2 个方案")
        @Size(min = 2, max = 8, message = "方案数量为 2-8 个")
        List<@NotBlank(message = "方案名称不能为空")
        @Size(max = 64, message = "方案名称不超过 64 字") String> options) {
}
