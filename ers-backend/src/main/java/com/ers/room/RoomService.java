package com.ers.room;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ers.common.ApiCode;
import com.ers.common.BizException;
import com.ers.common.Const;
import com.ers.room.dto.ReservationCreateRequest;
import com.ers.room.dto.ReservationVO;
import com.ers.room.dto.RoomVO;
import com.ers.room.dto.SlotVO;
import com.ers.room.dto.SlotsVO;
import com.ers.room.entity.Reservation;
import com.ers.room.entity.Room;
import com.ers.room.mapper.ReservationMapper;
import com.ers.room.mapper.RoomMapper;

/**
 * 场地与预约（PRD 4.4）：单日切片网格（Q3 决策：不做月历）、30 分钟唯一占用、开始前可取消、超时未使用记爽约。
 */
@Service
public class RoomService {

    private final RoomMapper roomMapper;
    private final ReservationMapper reservationMapper;

    public RoomService(RoomMapper roomMapper, ReservationMapper reservationMapper) {
        this.roomMapper = roomMapper;
        this.reservationMapper = reservationMapper;
    }

    // ------------------------------------------------------------------ 场地

    public List<RoomVO> listRooms(boolean includeDisabled) {
        List<Room> rooms = roomMapper.selectList(Wrappers.<Room>lambdaQuery()
                .eq(!includeDisabled, Room::getStatus, 1)
                .orderByAsc(Room::getId));
        List<RoomVO> result = new ArrayList<>(rooms.size());
        for (Room r : rooms) {
            result.add(new RoomVO(r.getId(), r.getName(), r.getLocation(), r.getCapacity(), r.getStatus()));
        }
        return result;
    }

    public SlotsVO slots(Long roomId, LocalDate date, Long currentUserId) {
        Room room = requireRoom(roomId);
        if (date == null || !SlotUtil.inBookableRange(date)) {
            throw new BizException(ApiCode.UNPROCESSABLE, "只能预约今天起 14 天内的时段");
        }
        List<Reservation> booked = reservationMapper.selectList(Wrappers.<Reservation>lambdaQuery()
                .eq(Reservation::getRoomId, room.getId())
                .eq(Reservation::getReserveDate, date)
                .eq(Reservation::getStatus, Const.ReservationStatus.BOOKED));
        Map<Integer, Long> occupant = new HashMap<>();
        for (Reservation r : booked) {
            occupant.put(r.getTimeSlot(), r.getUserId());
        }
        LocalDateTime now = LocalDateTime.now();
        List<SlotVO> slots = new ArrayList<>(SlotUtil.SLOTS_PER_DAY);
        for (int i = 0; i < SlotUtil.SLOTS_PER_DAY; i++) {
            String status;
            boolean mine = false;
            if (!SlotUtil.endAt(date, i).isAfter(now)) {
                status = "PAST";
            } else if (occupant.containsKey(i)) {
                status = "BOOKED";
                mine = currentUserId != null && currentUserId.equals(occupant.get(i));
            } else {
                status = "FREE";
            }
            slots.add(new SlotVO(i, SlotUtil.label(i), status, mine));
        }
        return new SlotsVO(room.getId(), date.toString(), slots, SlotUtil.BOOKABLE_DAYS);
    }

    // ------------------------------------------------------------------ 预约

    @Transactional
    public ReservationVO create(ReservationCreateRequest req, Long userId) {
        Room room = requireRoom(req.roomId());
        if (!Integer.valueOf(1).equals(room.getStatus())) {
            throw new BizException(ApiCode.UNPROCESSABLE, "该场地已停用，暂不可预约");
        }
        if (!SlotUtil.inBookableRange(req.date())) {
            throw new BizException(ApiCode.UNPROCESSABLE, "只能预约今天起 14 天内的时段");
        }
        if (!SlotUtil.isValidSlot(req.slot())) {
            throw new BizException(ApiCode.BAD_REQUEST, "时段不合法");
        }
        if (!SlotUtil.startAt(req.date(), req.slot()).isAfter(LocalDateTime.now())) {
            throw new BizException(ApiCode.UNPROCESSABLE, "该时段已开始，无法预约");
        }
        Reservation r = new Reservation();
        r.setRoomId(room.getId());
        r.setUserId(userId);
        r.setReserveDate(req.date());
        r.setTimeSlot(req.slot());
        r.setStatus(Const.ReservationStatus.BOOKED);
        r.setCreatedAt(LocalDateTime.now());
        try {
            reservationMapper.insert(r);
        } catch (DuplicateKeyException e) {
            throw new BizException(ApiCode.CONFLICT, "该时段已被预约，请刷新后重选");
        }
        return toVO(r, room, userId);
    }

    @Transactional
    public ReservationVO cancel(Long id, Long userId) {
        Reservation r = requireReservation(id);
        if (!r.getUserId().equals(userId)) {
            throw new BizException(ApiCode.FORBIDDEN, "只能取消自己的预约");
        }
        if (!Const.ReservationStatus.BOOKED.equals(r.getStatus())) {
            throw new BizException(ApiCode.UNPROCESSABLE, "该预约已不能取消");
        }
        if (!SlotUtil.startAt(r.getReserveDate(), r.getTimeSlot()).isAfter(LocalDateTime.now())) {
            throw new BizException(ApiCode.UNPROCESSABLE, "预约已开始，无法取消");
        }
        int rows = reservationMapper.update(null, new LambdaUpdateWrapper<Reservation>()
                .set(Reservation::getStatus, Const.ReservationStatus.CANCELLED)
                .set(Reservation::getCancelledAt, LocalDateTime.now())
                .set(Reservation::getUpdatedAt, LocalDateTime.now())
                .eq(Reservation::getId, id)
                .eq(Reservation::getStatus, Const.ReservationStatus.BOOKED));
        if (rows == 0) {
            throw new BizException(ApiCode.CONFLICT, "该预约已变化，请刷新后重试");
        }
        r.setStatus(Const.ReservationStatus.CANCELLED);
        return toVO(r, roomMapper.selectById(r.getRoomId()), userId);
    }

    @Transactional
    public ReservationVO use(Long id, Long userId) {
        Reservation r = requireReservation(id);
        if (!r.getUserId().equals(userId)) {
            throw new BizException(ApiCode.FORBIDDEN, "只能标记自己的预约");
        }
        if (!Const.ReservationStatus.BOOKED.equals(r.getStatus())) {
            throw new BizException(ApiCode.UNPROCESSABLE, "该预约已不能标记使用");
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = SlotUtil.startAt(r.getReserveDate(), r.getTimeSlot());
        LocalDateTime end = SlotUtil.endAt(r.getReserveDate(), r.getTimeSlot()).plusMinutes(10);
        if (now.isBefore(start) || now.isAfter(end)) {
            throw new BizException(ApiCode.UNPROCESSABLE, "只能在预约时段内标记使用");
        }
        int rows = reservationMapper.update(null, new LambdaUpdateWrapper<Reservation>()
                .set(Reservation::getStatus, Const.ReservationStatus.USED)
                .set(Reservation::getCheckedInAt, now)
                .set(Reservation::getUpdatedAt, now)
                .eq(Reservation::getId, id)
                .eq(Reservation::getStatus, Const.ReservationStatus.BOOKED));
        if (rows == 0) {
            throw new BizException(ApiCode.CONFLICT, "该预约已变化，请刷新后重试");
        }
        r.setStatus(Const.ReservationStatus.USED);
        return toVO(r, roomMapper.selectById(r.getRoomId()), userId);
    }

    /** 我的预约（含历史，倒序）。 */
    public List<ReservationVO> myReservations(Long userId) {
        List<Reservation> list = reservationMapper.selectList(Wrappers.<Reservation>lambdaQuery()
                .eq(Reservation::getUserId, userId)
                .orderByDesc(Reservation::getReserveDate)
                .orderByDesc(Reservation::getTimeSlot)
                .last("LIMIT 100"));
        Map<Long, String> roomNames = new HashMap<>();
        Set<Long> roomIds = new HashSet<>();
        for (Reservation r : list) {
            roomIds.add(r.getRoomId());
        }
        if (!roomIds.isEmpty()) {
            for (Room room : roomMapper.selectBatchIds(roomIds)) {
                roomNames.put(room.getId(), room.getName());
            }
        }
        List<ReservationVO> result = new ArrayList<>(list.size());
        LocalDateTime now = LocalDateTime.now();
        for (Reservation r : list) {
            boolean canCancel = Const.ReservationStatus.BOOKED.equals(r.getStatus())
                    && SlotUtil.startAt(r.getReserveDate(), r.getTimeSlot()).isAfter(now);
            boolean canUse = Const.ReservationStatus.BOOKED.equals(r.getStatus())
                    && !now.isBefore(SlotUtil.startAt(r.getReserveDate(), r.getTimeSlot()))
                    && !now.isAfter(SlotUtil.endAt(r.getReserveDate(), r.getTimeSlot()).plusMinutes(10));
            result.add(new ReservationVO(r.getId(), r.getRoomId(), roomNames.get(r.getRoomId()),
                    r.getReserveDate(), r.getTimeSlot(), SlotUtil.label(r.getTimeSlot()), r.getStatus(),
                    r.getCreatedAt(), canCancel, canUse));
        }
        return result;
    }

    /** 今日生效预约数（工作台）。 */
    public long todayCount(Long userId) {
        Long c = reservationMapper.selectCount(Wrappers.<Reservation>lambdaQuery()
                .eq(Reservation::getUserId, userId)
                .eq(Reservation::getReserveDate, LocalDate.now())
                .eq(Reservation::getStatus, Const.ReservationStatus.BOOKED));
        return c == null ? 0 : c;
    }

    // ------------------------------------------------------------------ 内部

    private Room requireRoom(Long roomId) {
        Room room = roomId == null ? null : roomMapper.selectById(roomId);
        if (room == null) {
            throw new BizException(ApiCode.NOT_FOUND, "场地不存在");
        }
        return room;
    }

    private Reservation requireReservation(Long id) {
        Reservation r = id == null ? null : reservationMapper.selectById(id);
        if (r == null) {
            throw new BizException(ApiCode.NOT_FOUND, "预约不存在");
        }
        return r;
    }

    private ReservationVO toVO(Reservation r, Room room, Long userId) {
        LocalDateTime now = LocalDateTime.now();
        boolean canCancel = Const.ReservationStatus.BOOKED.equals(r.getStatus())
                && SlotUtil.startAt(r.getReserveDate(), r.getTimeSlot()).isAfter(now);
        boolean canUse = Const.ReservationStatus.BOOKED.equals(r.getStatus())
                && !now.isBefore(SlotUtil.startAt(r.getReserveDate(), r.getTimeSlot()))
                && !now.isAfter(SlotUtil.endAt(r.getReserveDate(), r.getTimeSlot()).plusMinutes(10));
        return new ReservationVO(r.getId(), r.getRoomId(), room == null ? null : room.getName(),
                r.getReserveDate(), r.getTimeSlot(), SlotUtil.label(r.getTimeSlot()), r.getStatus(),
                r.getCreatedAt(), canCancel, canUse);
    }
}
