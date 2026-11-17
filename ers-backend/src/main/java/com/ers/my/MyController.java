package com.ers.my;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ers.common.ApiResponse;
import com.ers.credit.dto.MyCreditVO;
import com.ers.my.dto.WorkbenchVO;
import com.ers.room.dto.ReservationVO;
import com.ers.security.UserContext;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 个人视图（IA §4：P-HOME-01 工作台 / P-ME-01 我的 / P-ME-02 信誉）。
 */
@Tag(name = "个人视图")
@RestController
@RequestMapping("/api/my")
public class MyController {

    private final MyService myService;

    public MyController(MyService myService) {
        this.myService = myService;
    }

    @Operation(summary = "我的预约（含历史）")
    @GetMapping("/reservations")
    public ApiResponse<List<ReservationVO>> reservations() {
        return ApiResponse.ok(myService.myReservations(UserContext.requireUserId()));
    }

    @Operation(summary = "我的信誉（分数 + 阈值 + 流水）")
    @GetMapping("/credit")
    public ApiResponse<MyCreditVO> credit() {
        return ApiResponse.ok(myService.myCredit(UserContext.requireUserId()));
    }

    @Operation(summary = "工作台聚合（首屏一次取数）")
    @GetMapping("/workbench")
    public ApiResponse<WorkbenchVO> workbench() {
        return ApiResponse.ok(myService.workbench(UserContext.requireUserId()));
    }
}
