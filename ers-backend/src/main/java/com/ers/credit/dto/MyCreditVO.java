package com.ers.credit.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 个人信誉（API 契约 §5 #20）：分数 + 阈值 + 是否受限 + 扣分流水。
 */
public record MyCreditVO(int creditScore, int threshold, boolean restricted, List<Log> logs) {

    public record Log(Integer delta, String reason, String remark, LocalDate bizDate, LocalDateTime createdAt) {
    }
}
