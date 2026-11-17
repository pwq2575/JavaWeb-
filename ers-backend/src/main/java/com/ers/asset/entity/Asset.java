package com.ers.asset.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * 资产台账（asset）。单一资产实体，状态以枚举字符串存储（IA B5 / B6）。
 */
@Data
@TableName("asset")
public class Asset {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String code;
    private String name;
    private String category;
    private BigDecimal value;
    private String status;
    private Long holderId;
    private LocalDateTime borrowAt;
    private LocalDate dueDate;
    private Integer renewCount;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
