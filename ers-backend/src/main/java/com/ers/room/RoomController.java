package com.ers.room;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ers.common.ApiResponse;
import com.ers.room.dto.ReservationCreateRequest;
import com.ers.room.dto.ReservationVO;
import com.ers.room.dto.RoomVO;
import com.ers.room.dto.SlotsVO;
import com.ers.security.UserContext;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * 场地与预约（IA §4 P-ROOM-01 / 02，Q3 决策：单日网格）。
 */
@Tag(name = "场地与预约")
@RestController
@RequestMapping("/api")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @Operation(summary = "可用场地列表")
    @GetMapping("/rooms")
    public ApiResponse<List<RoomVO>> rooms() {
        return ApiResponse.ok(roomService.listRooms(false));
    }

    @Operation(summary = "场地单日切片状态（18 个 30 分钟切片）")
    @GetMapping("/rooms/{id}/slots")
    public ApiResponse<SlotsVO> slots(@PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.ok(roomService.slots(id, date, UserContext.userId()));
    }

    @Operation(summary = "预约一个切片")
    @PostMapping("/reservations")
    public ApiResponse<ReservationVO> create(@Valid @RequestBody ReservationCreateRequest req) {
        return ApiResponse.ok(roomService.create(req, UserContext.requireUserId()));
    }

    @Operation(summary = "取消预约（仅开始前）")
    @PutMapping("/reservations/{id}/cancel")
    public ApiResponse<ReservationVO> cancel(@PathVariable Long id) {
        return ApiResponse.ok(roomService.cancel(id, UserContext.requireUserId()));
    }

    @Operation(summary = "标记已使用（时段内）")
    @PutMapping("/reservations/{id}/use")
    public ApiResponse<ReservationVO> use(@PathVariable Long id) {
        return ApiResponse.ok(roomService.use(id, UserContext.requireUserId()));
    }
}
