package com.ers.asset;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ers.asset.dto.AssetDetailVO;
import com.ers.asset.dto.AssetVO;
import com.ers.common.ApiResponse;
import com.ers.common.PageResult;
import com.ers.security.UserContext;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 资产台账（员工端）：列表、详情、借出、续借、归还（IA §4 P-ASSET-01 / 02）。
 */
@Tag(name = "资产台账")
@RestController
@RequestMapping("/api")
public class AssetController {

    private final AssetService assetService;

    public AssetController(AssetService assetService) {
        this.assetService = assetService;
    }

    @Operation(summary = "资产列表（分页 + 关键字/状态/分类筛选）")
    @GetMapping("/assets")
    public ApiResponse<PageResult<AssetVO>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category) {
        return ApiResponse.ok(assetService.list(page, size, keyword, status, category, null,
                UserContext.userId(), UserContext.isAdmin()));
    }

    @Operation(summary = "资产详情（含流转时间线与待审批转交）")
    @GetMapping("/assets/{id}")
    public ApiResponse<AssetDetailVO> detail(@PathVariable Long id) {
        return ApiResponse.ok(assetService.detail(id, UserContext.userId(), UserContext.isAdmin()));
    }

    @Operation(summary = "借出资产（借期默认 3 天；高价值资产校验信誉阈值）")
    @PostMapping("/assets/{id}/borrow")
    public ApiResponse<AssetVO> borrow(@PathVariable Long id) {
        return ApiResponse.ok(assetService.borrow(id, UserContext.requireUserId()));
    }

    @Operation(summary = "续借（+1 天，最多 3 次；存在待审批转交时不可续借）")
    @PostMapping("/assets/{id}/renew")
    public ApiResponse<AssetVO> renew(@PathVariable Long id) {
        return ApiResponse.ok(assetService.renew(id, UserContext.requireUserId()));
    }

    @Operation(summary = "归还资产")
    @PostMapping("/assets/{id}/return")
    public ApiResponse<AssetVO> returnAsset(@PathVariable Long id) {
        return ApiResponse.ok(assetService.returnAsset(id, UserContext.requireUserId(), UserContext.isAdmin()));
    }

    @Operation(summary = "我当前持有的资产")
    @GetMapping("/my/borrows")
    public ApiResponse<List<AssetVO>> myBorrows() {
        return ApiResponse.ok(assetService.myBorrows(UserContext.requireUserId()));
    }
}
