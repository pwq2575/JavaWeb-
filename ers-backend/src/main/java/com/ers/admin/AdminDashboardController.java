package com.ers.admin;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ers.admin.dto.AdminDashboardVO;
import com.ers.common.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 管理看板（IA §4 P-ADM-01）。
 */
@Tag(name = "管理端-看板")
@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {

    private final AdminDashboardService dashboardService;

    public AdminDashboardController(AdminDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @Operation(summary = "看板数据（KPI + 价值分布 + 借出趋势 + 状态分布）")
    @GetMapping
    public ApiResponse<AdminDashboardVO> dashboard() {
        return ApiResponse.ok(dashboardService.build());
    }
}
