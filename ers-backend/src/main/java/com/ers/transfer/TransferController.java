package com.ers.transfer;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ers.common.ApiResponse;
import com.ers.security.UserContext;
import com.ers.transfer.dto.TransferCreateRequest;
import com.ers.transfer.dto.TransferPageVO;
import com.ers.transfer.dto.TransferRejectRequest;
import com.ers.transfer.dto.TransferVO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * 转交（IA §4 P-ASSET-02 发起 / P-ASSET-04 审批，Q1 决策：两个入口分工固定）。
 */
@Tag(name = "转交")
@RestController
@RequestMapping("/api/transfers")
public class TransferController {

    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    @Operation(summary = "申请接手（B 向当前持有者 A 发起转交申请）")
    @PostMapping
    public ApiResponse<TransferVO> create(@Valid @RequestBody TransferCreateRequest req) {
        return ApiResponse.ok(transferService.create(req, UserContext.requireUserId()));
    }

    @Operation(summary = "我的转交（box=incoming 待我审批 / outgoing 我发起的）")
    @GetMapping
    public ApiResponse<TransferPageVO> list(
            @RequestParam(defaultValue = "incoming") String box,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {
        return ApiResponse.ok(transferService.list(box, status, page, size, UserContext.requireUserId()));
    }

    @Operation(summary = "同意转交（持有者变更为 B，应归还日不变）")
    @PutMapping("/{id}/approve")
    public ApiResponse<TransferVO> approve(@PathVariable Long id) {
        return ApiResponse.ok(transferService.approve(id, UserContext.requireUserId()));
    }

    @Operation(summary = "拒绝转交（理由必填）")
    @PutMapping("/{id}/reject")
    public ApiResponse<TransferVO> reject(@PathVariable Long id, @Valid @RequestBody TransferRejectRequest req) {
        return ApiResponse.ok(transferService.reject(id, req.rejectReason(), UserContext.requireUserId()));
    }
}
