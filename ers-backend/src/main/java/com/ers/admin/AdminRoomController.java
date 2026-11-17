package com.ers.admin;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ers.admin.dto.AdminRoomSaveRequest;
import com.ers.common.ApiResponse;
import com.ers.room.dto.RoomVO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * 管理端场地管理（IA §4 P-ADM-03；D14 课程要求：管理员可对场地增删改查）。
 */
@Tag(name = "管理端-场地管理")
@RestController
@RequestMapping("/api/admin/rooms")
public class AdminRoomController {

    private final AdminRoomService roomService;

    public AdminRoomController(AdminRoomService roomService) {
        this.roomService = roomService;
    }

    @Operation(summary = "场地列表（含停用）")
    @GetMapping
    public ApiResponse<List<RoomVO>> list() {
        return ApiResponse.ok(roomService.list());
    }

    @Operation(summary = "新增场地")
    @PostMapping
    public ApiResponse<RoomVO> create(@Valid @RequestBody AdminRoomSaveRequest req) {
        return ApiResponse.ok(roomService.create(req));
    }

    @Operation(summary = "编辑场地（停用不影响已有预约）")
    @PutMapping("/{id}")
    public ApiResponse<RoomVO> update(@PathVariable Long id, @Valid @RequestBody AdminRoomSaveRequest req) {
        return ApiResponse.ok(roomService.update(id, req));
    }

    @Operation(summary = "删除场地（仅限无未来预约）")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        roomService.delete(id);
        return ApiResponse.ok(null);
    }
}
