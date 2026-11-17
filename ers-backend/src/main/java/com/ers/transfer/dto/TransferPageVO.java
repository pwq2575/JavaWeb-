package com.ers.transfer.dto;

import java.util.List;

/**
 * 转交列表分页结果 + 待我审批角标数（API 契约 §4 #15）。
 */
public record TransferPageVO(List<TransferVO> records, long total, long page, long size, long pendingCount) {
}
