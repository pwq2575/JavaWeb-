package com.ers.transfer.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 申请接手（转交）请求（PRD §4.2(3)、API 契约 §4 #14）。
 * <p>语义：B（申请人 = 当前登录用户）到库房发现资产被 A 借走，向当前持有者 A 申请接手；
 * 申请人**不指定**接收人，{@code to_user_id} 由令牌推出，{@code from_user_id} 取资产当前持有者。</p>
 */
public record TransferCreateRequest(
        @NotNull(message = "资产不能为空") Long assetId,
        @Size(max = 200, message = "申请说明不超过 200 字") String applyReason) {
}
