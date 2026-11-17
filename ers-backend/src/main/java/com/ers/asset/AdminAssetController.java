package com.ers.asset;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ers.asset.dto.AssetSaveRequest;
import com.ers.asset.dto.AssetVO;
import com.ers.common.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * 管理端资产台账维护（PRD 4.2(7)）：新增 / 编辑 / 删除 / 启停。
 * 路径前缀 /api/admin/** 由 Spring Security 统一要求 ROLE_ADMIN。
 */
@Tag(name = "管理端-资产台账")
@RestController
@RequestMapping("/api/admin/assets")
public class AdminAssetController {

    private final AssetService assetService;

    public AdminAssetController(AssetService assetService) {
        this.assetService = assetService;
    }

    @Operation(summary = "新增资产")
    @PostMapping
    public ApiResponse<AssetVO> create(@Valid @RequestBody AssetSaveRequest req) {
        return ApiResponse.ok(assetService.create(req));
    }

    @Operation(summary = "编辑资产（借出中不可编辑）")
    @PutMapping("/{id}")
    public ApiResponse<AssetVO> update(@PathVariable Long id, @Valid @RequestBody AssetSaveRequest req) {
        return ApiResponse.ok(assetService.update(id, req));
    }

    @Operation(summary = "删除资产（仅限无流转记录）")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        assetService.delete(id);
        return ApiResponse.ok(null);
    }

    @Operation(summary = "停用资产（借出中不可停用）")
    @PostMapping("/{id}/disable")
    public ApiResponse<AssetVO> disable(@PathVariable Long id) {
        return ApiResponse.ok(assetService.setEnabled(id, false));
    }

    @Operation(summary = "启用资产")
    @PostMapping("/{id}/enable")
    public ApiResponse<AssetVO> enable(@PathVariable Long id) {
        return ApiResponse.ok(assetService.setEnabled(id, true));
    }
}
