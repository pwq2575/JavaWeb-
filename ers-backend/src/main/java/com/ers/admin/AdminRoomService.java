package com.ers.admin;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ers.admin.dto.AdminRoomSaveRequest;
import com.ers.common.ApiCode;
import com.ers.common.BizException;
import com.ers.room.dto.RoomVO;
import com.ers.room.entity.Reservation;
import com.ers.room.entity.Room;
import com.ers.room.mapper.ReservationMapper;
import com.ers.room.mapper.RoomMapper;

/**
 * 管理端场地管理（API 契约 §9 #46-49，D14）：增删改查；删除仅限无未来预约，停用不影响已有预约。
 */
@Service
public class AdminRoomService {

    private final RoomMapper roomMapper;
    private final ReservationMapper reservationMapper;

    public AdminRoomService(RoomMapper roomMapper, ReservationMapper reservationMapper) {
        this.roomMapper = roomMapper;
        this.reservationMapper = reservationMapper;
    }

    /** #46 场地列表（含停用，管理视角）。 */
    public List<RoomVO> list() {
        List<Room> rooms = roomMapper.selectList(Wrappers.<Room>lambdaQuery().orderByAsc(Room::getId));
        List<RoomVO> result = new ArrayList<>(rooms.size());
        for (Room r : rooms) {
            result.add(toVO(r));
        }
        return result;
    }

    /** #47 新增场地。 */
    @Transactional
    public RoomVO create(AdminRoomSaveRequest req) {
        Room r = new Room();
        r.setName(req.name().trim());
        r.setLocation(trimToNull(req.location()));
        r.setCapacity(req.capacity() == null ? 1 : req.capacity());
        r.setStatus(req.status() == null ? 1 : req.status());
        r.setCreatedAt(LocalDateTime.now());
        r.setUpdatedAt(LocalDateTime.now());
        roomMapper.insert(r);
        return toVO(r);
    }

    /** #48 编辑场地。 */
    @Transactional
    public RoomVO update(Long id, AdminRoomSaveRequest req) {
        Room r = requireRoom(id);
        r.setName(req.name().trim());
        r.setLocation(trimToNull(req.location()));
        if (req.capacity() != null) {
            r.setCapacity(req.capacity());
        }
        if (req.status() != null) {
            r.setStatus(req.status());
        }
        r.setUpdatedAt(LocalDateTime.now());
        roomMapper.updateById(r);
        return toVO(r);
    }

    /** #49 删除场地：仅限无未来预约（停用不影响已有预约）。 */
    @Transactional
    public void delete(Long id) {
        requireRoom(id);
        Long future = reservationMapper.selectCount(Wrappers.<Reservation>lambdaQuery()
                .eq(Reservation::getRoomId, id)
                .ge(Reservation::getReserveDate, LocalDate.now())
                .eq(Reservation::getStatus, "BOOKED"));
        if (future != null && future > 0) {
            throw new BizException(ApiCode.UNPROCESSABLE, "该场地仍有未来预约，无法删除，请先处理预约或改为停用");
        }
        roomMapper.deleteById(id);
    }

    private Room requireRoom(Long id) {
        Room r = id == null ? null : roomMapper.selectById(id);
        if (r == null) {
            throw new BizException(ApiCode.NOT_FOUND, "场地不存在");
        }
        return r;
    }

    private RoomVO toVO(Room r) {
        return new RoomVO(r.getId(), r.getName(), r.getLocation(), r.getCapacity(), r.getStatus());
    }

    private String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String v = s.trim();
        return v.isEmpty() ? null : v;
    }
}
