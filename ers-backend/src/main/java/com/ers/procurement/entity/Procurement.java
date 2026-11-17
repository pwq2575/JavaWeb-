package com.ers.procurement.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * 采购申请（procurement）：二态审批，本期不自动联动资产台账（IA B2）。
 * 逻辑删除仅用于"删除已驳回"申请，查询时需显式过滤 deleted_at IS NULL。
 */
@Data
@TableName("procurement")
public class Procurement {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String reason;
    private String status;
    private Long applicantId;
    private Long approverId;
    private String approveNote;
    private LocalDateTime createdAt;
    private LocalDateTime decidedAt;
    private LocalDateTime deletedAt;
}
