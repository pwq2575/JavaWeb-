package com.ers.export;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ers.common.ApiCode;
import com.ers.common.BizException;
import com.ers.security.UserContext;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Excel 导出（IA §4 P-ADM-02 资产台账导出 / P-ME 个人借用记录导出）。
 */
@Tag(name = "导出")
@RestController
@RequestMapping("/api/exports")
public class ExportController {

    private final ExportService exportService;

    public ExportController(ExportService exportService) {
        this.exportService = exportService;
    }

    @Operation(summary = "导出资产台账（管理员）")
    @GetMapping("/assets")
    public ResponseEntity<byte[]> assets() {
        if (!UserContext.isAdmin()) {
            throw new BizException(ApiCode.FORBIDDEN, "只有管理员可以导出资产台账");
        }
        return exportService.exportAssets();
    }

    @Operation(summary = "导出借用记录（本人；管理员可传 userId）")
    @GetMapping("/borrows")
    public ResponseEntity<byte[]> borrows(@RequestParam(required = false) Long userId) {
        return exportService.exportBorrows(userId, UserContext.requireUserId(), UserContext.isAdmin());
    }
}
