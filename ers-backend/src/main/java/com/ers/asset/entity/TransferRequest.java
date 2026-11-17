package com.ers.asset.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * 转交申请（transfer_request）：B 发起 → 当前持有者 A 审批。
 * pending_asset_id 为数据库生成列，应用侧不写入。
 */
@Data
@TableName("transfer_request")
public class TransferRequest {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long assetId;
    private Long fromUserId;
    private Long toUserId;
    private String status;
    private String applyReason;
    private String rejectReason;
    private LocalDateTime createdAt;
    private LocalDateTime decidedAt;
}
