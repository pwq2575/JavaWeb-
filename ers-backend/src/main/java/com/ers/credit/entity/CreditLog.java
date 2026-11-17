package com.ers.credit.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * 信誉分流水（credit_log）：逾期每日 -5 去重、爽约一次性 -5。
 */
@Data
@TableName("credit_log")
public class CreditLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Integer delta;
    private String reason;
    private String refType;
    private Long refId;
    private LocalDate bizDate;
    private String remark;
    private LocalDateTime createdAt;
}
