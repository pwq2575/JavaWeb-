package com.ers.vote;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ers.common.ApiResponse;
import com.ers.vote.dto.VoteDetailVO;
import com.ers.vote.dto.VoteSaveRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * 文化投票：管理员侧（IA §4 P-ADM-05）。SecurityConfig 已统一要求 ROLE_ADMIN。
 */
@Tag(name = "投票（管理端）")
@RestController
@RequestMapping("/api/admin/votes")
public class AdminVoteController {

    private final VoteService voteService;

    public AdminVoteController(VoteService voteService) {
        this.voteService = voteService;
    }

    @Operation(summary = "新建投票草案")
    @PostMapping
    public ApiResponse<VoteDetailVO> create(@Valid @RequestBody VoteSaveRequest req) {
        return ApiResponse.ok(voteService.create(req));
    }

    @Operation(summary = "编辑草案（仅草稿）")
    @PutMapping("/{id}")
    public ApiResponse<VoteDetailVO> update(@PathVariable Long id, @Valid @RequestBody VoteSaveRequest req) {
        return ApiResponse.ok(voteService.update(id, req));
    }

    @Operation(summary = "删除草案（仅草稿）")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        voteService.delete(id);
        return ApiResponse.ok(null);
    }

    @Operation(summary = "发布投票（≥2 方案）")
    @PostMapping("/{id}/publish")
    public ApiResponse<VoteDetailVO> publish(@PathVariable Long id) {
        return ApiResponse.ok(voteService.publish(id));
    }

    @Operation(summary = "手动提前关闭")
    @PostMapping("/{id}/close")
    public ApiResponse<VoteDetailVO> close(@PathVariable Long id) {
        return ApiResponse.ok(voteService.close(id));
    }
}
