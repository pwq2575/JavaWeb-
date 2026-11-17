package com.ers.admin;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ers.admin.dto.AdminConfigUpdateRequest;
import com.ers.admin.dto.AdminConfigVO;
import com.ers.common.ApiCode;
import com.ers.common.ApiResponse;
import com.ers.common.Const;
import com.ers.common.BizException;
import com.ers.config.ConfigService;
import com.ers.security.UserContext;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * 管理端策略配置（IA §4 P-ADM-06，策略层 S5）。
 */
@Tag(name = "管理端-策略配置")
@RestController
@RequestMapping("/api/admin/config")
public class AdminConfigController {

    private final ConfigService configService;

    public AdminConfigController(ConfigService configService) {
        this.configService = configService;
    }

    @Operation(summary = "读取策略配置")
    @GetMapping
    public ApiResponse<AdminConfigVO> get() {
        return ApiResponse.ok(new AdminConfigVO(configService.creditThreshold(),
                configService.highValueThreshold(), configService.borrowDays(), configService.renewMaxTimes(), null));
    }

    @Operation(summary = "更新策略配置（部分更新；信誉阈值只能 50/60/70/80）")
    @PutMapping
    public ApiResponse<AdminConfigVO> update(@Valid @RequestBody AdminConfigUpdateRequest req) {
        Long operator = UserContext.requireUserId();
        if (req.creditThreshold() != null) {
            configService.set(Const.ConfigKey.CREDIT_THRESHOLD, req.creditThreshold(), operator);
        }
        if (req.highValueThreshold() != null) {
            configService.set(Const.ConfigKey.HIGH_VALUE_THRESHOLD, String.valueOf(req.highValueThreshold()), operator);
        }
        if (req.borrowDays() != null) {
            configService.set(Const.ConfigKey.BORROW_DAYS, String.valueOf(req.borrowDays()), operator);
        }
        if (req.renewMaxTimes() != null) {
            configService.set(Const.ConfigKey.RENEW_MAX_TIMES, String.valueOf(req.renewMaxTimes()), operator);
        }
        if (req.creditThreshold() == null && req.highValueThreshold() == null
                && req.borrowDays() == null && req.renewMaxTimes() == null) {
            throw new BizException(ApiCode.BAD_REQUEST, "没有需要更新的配置项");
        }
        return get();
    }
}
