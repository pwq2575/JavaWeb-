package com.ers.vote.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * 投票活动（vote_topic）：DRAFT 草稿 / PUBLISHED 进行中 / CLOSED 已关闭。
 * 到点自动停收（按 deadline 判定），提前关闭写 closed_at。
 */
@Data
@TableName("vote_topic")
public class VoteTopic {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String description;
    private LocalDateTime deadline;
    private String status;
    private Long createdBy;
    private LocalDateTime publishedAt;
    private LocalDateTime closedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
