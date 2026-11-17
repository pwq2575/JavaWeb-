package com.ers.transfer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 拒绝转交（API 契约 §4 #17）：理由必填。
 */
public record TransferRejectRequest(
        @NotBlank(message = "请填写拒绝理由")
        @Size(min = 1, max = 100, message = "拒绝理由 1-100 字") String rejectReason) {
}
