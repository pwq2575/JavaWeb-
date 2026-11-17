package com.ers.vote.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * 投票记录（vote_record）：每人每活动一票，不可改（唯一键 uk_vote_user）。
 */
@Data
@TableName("vote_record")
public class VoteRecord {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long topicId;
    private Long optionId;
    private Long userId;
    private LocalDateTime createdAt;
}
