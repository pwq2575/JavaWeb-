package com.ers.transfer.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 发起转交请求（API 契约 §4 #14）：由 B 向当前持有者 A 发起。
 */
public record TransferCreateRequest(
        @NotNull(message = "资产不能为空") Long assetId,
        @NotNull(message = "接收人不能为空") Long toUserId,
        @Size(max = 200, message = "转交说明不超过 200 字") String applyReason) {
}
