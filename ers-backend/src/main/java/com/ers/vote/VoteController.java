package com.ers.vote;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ers.common.ApiResponse;
import com.ers.common.PageResult;
import com.ers.security.UserContext;
import com.ers.vote.dto.VoteDetailVO;
import com.ers.vote.dto.VoteListItemVO;
import com.ers.vote.dto.VoteSubmitRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * 文化投票：员工侧（IA §4 P-VOTE-01 活动列表 / P-VOTE-02 投票详情）。
 */
@Tag(name = "投票（员工侧）")
@RestController
@RequestMapping("/api/votes")
public class VoteController {

    private final VoteService voteService;

    public VoteController(VoteService voteService) {
        this.voteService = voteService;
    }

    @Operation(summary = "投票活动列表（已发布 + 已关闭；管理员可查草稿）")
    @GetMapping
    public ApiResponse<PageResult<VoteListItemVO>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) String status) {
        return ApiResponse.ok(voteService.list(page, size, status, UserContext.isAdmin()));
    }

    @Operation(summary = "投票详情（投前隐藏票数）")
    @GetMapping("/{id}")
    public ApiResponse<VoteDetailVO> detail(@PathVariable Long id) {
        return ApiResponse.ok(voteService.detail(id));
    }

    @Operation(summary = "提交投票（每人一次，不可修改）")
    @PostMapping("/{id}/vote")
    public ApiResponse<VoteDetailVO> vote(@PathVariable Long id, @Valid @RequestBody VoteSubmitRequest req) {
        return ApiResponse.ok(voteService.vote(id, req.optionId()));
    }
}
