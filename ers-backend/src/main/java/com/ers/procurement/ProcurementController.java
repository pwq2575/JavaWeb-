package com.ers.procurement;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ers.common.ApiResponse;
import com.ers.common.BizException;
import com.ers.common.PageResult;
import com.ers.procurement.dto.ProcurementCreateRequest;
import com.ers.procurement.dto.ProcurementDecideRequest;
import com.ers.procurement.dto.ProcurementVO;
import com.ers.security.UserContext;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * 采购审批（IA §4 P-PROC-01 我的申请 / P-ADM-04 审批列表）。
 */
@Tag(name = "采购审批")
@RestController
@RequestMapping("/api/procurements")
public class ProcurementController {

    private final ProcurementService procurementService;

    public ProcurementController(ProcurementService procurementService) {
        this.procurementService = procurementService;
    }

    @Operation(summary = "采购申请列表（管理员看全部，普通用户看自己）")
    @GetMapping
    public ApiResponse<PageResult<ProcurementVO>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) String status) {
        return ApiResponse.ok(procurementService.list(page, size, status,
                UserContext.requireUserId(), UserContext.isAdmin()));
    }

    @Operation(summary = "提交采购申请（名称 + 理由）")
    @PostMapping
    public ApiResponse<ProcurementVO> create(@Valid @RequestBody ProcurementCreateRequest req) {
        return ApiResponse.ok(procurementService.create(req, UserContext.requireUserId()));
    }

    @Operation(summary = "同意入库（管理员；仅改状态，不写资产台账）")
    @PutMapping("/{id}/approve")
    public ApiResponse<ProcurementVO> approve(@PathVariable Long id,
            @RequestBody(required = false) ProcurementDecideRequest req) {
        requireAdmin();
        return ApiResponse.ok(procurementService.approve(id, req == null ? null : req.approveNote(),
                UserContext.requireUserId()));
    }

    @Operation(summary = "驳回（管理员；理由必填）")
    @PutMapping("/{id}/reject")
    public ApiResponse<ProcurementVO> reject(@PathVariable Long id,
            @RequestBody(required = false) ProcurementDecideRequest req) {
        requireAdmin();
        return ApiResponse.ok(procurementService.reject(id, req == null ? null : req.approveNote(),
                UserContext.requireUserId()));
    }

    /**
     * 审批入口统一鉴权（API 契约 #29/#30：ADMIN）。
     * <p>路径不在 /api/admin/** 下，Spring Security 只校验「已登录」，因此必须在此显式校验角色。</p>
     */
    private void requireAdmin() {
        if (!UserContext.isAdmin()) {
            throw BizException.forbidden("只有管理员可以审批采购申请");
        }
    }

    @Operation(summary = "删除已驳回的本人申请")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        procurementService.delete(id, UserContext.requireUserId(), UserContext.isAdmin());
        return ApiResponse.ok(null);
    }
}
