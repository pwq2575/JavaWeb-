package com.ers.room.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * 场地预约（reservation）：固定 30 分钟切片，time_slot 0..17。
 * active_slot 为数据库生成列（仅 BOOKED 占用），应用侧不写入。
 */
@Data
@TableName("reservation")
public class Reservation {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long roomId;
    private Long userId;
    private LocalDate reserveDate;
    private Integer timeSlot;
    private String status;
    private LocalDateTime cancelledAt;
    private LocalDateTime checkedInAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
