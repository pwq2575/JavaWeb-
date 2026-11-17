package com.ers.asset.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * 资产流转记录（asset_record，只增不改，审计源，IA A3）。
 */
@Data
@TableName("asset_record")
public class AssetRecord {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long assetId;
    private String action;
    private Long operatorId;
    private Long targetUserId;
    private String fromStatus;
    private String toStatus;
    private String remark;
    private LocalDateTime happenedAt;
}
