package com.ers.vote.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * 投票选项（vote_option）。
 */
@Data
@TableName("vote_option")
public class VoteOption {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long topicId;
    private String label;
    private Integer sortNo;
}
