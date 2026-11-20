<template>
  <div class="ers-page">
    <div class="ers-page-head">
      <div>
        <h1 class="ers-page-title">场地预约</h1>
        <p class="ers-page-sub">30 分钟为一格，开放未来 {{ bookableDays }} 天；开始前可取消，结束未使用且未取消记为爽约（扣 5 分）</p>
      </div>
      <div class="head-actions">
        <el-date-picker
          v-model="date"
          type="date"
          value-format="YYYY-MM-DD"
          :disabled-date="disabledDate"
          placeholder="选择日期"
          style="width: 150px"
          @change="loadSlots"
        />
        <el-button :icon="Refresh" @click="refresh">刷新</el-button>
      </div>
    </div>

    <div class="room-tabs">
      <button
        v-for="room in rooms"
        :key="room.id"
        class="room-tab"
        :class="{ active: room.id === roomId }"
        :disabled="room.status !== 1"
        @click="selectRoom(room)"
      >
        <span class="room-name">{{ room.name }}</span>
        <span class="ers-muted">{{ room.location }} · {{ room.capacity }} 人{{ room.status !== 1 ? ' · 停用' : '' }}</span>
      </button>
    </div>

    <div class="ers-card slots-card" v-loading="loading">
      <div class="slots-head">
        <h2>{{ currentRoom?.name || '请选择场地' }} · {{ date }}</h2>
        <div class="legend">
          <span class="lg"><i class="dot free"></i>可预约</span>
          <span class="lg"><i class="dot mine"></i>我的预约</span>
          <span class="lg"><i class="dot taken"></i>已被占用</span>
          <span class="lg"><i class="dot past"></i>已过期</span>
        </div>
      </div>

      <div class="slot-grid">
        <button
          v-for="slot in slots"
          :key="slot.slot"
          class="slot"
          :class="slotClass(slot)"
          :disabled="slot.status !== 'FREE'"
          @click="onSlotClick(slot)"
        >
          <span class="slot-label">{{ slot.label }}</span>
          <span class="slot-state">{{ slotStateText(slot) }}</span>
        </button>
      </div>
    </div>

    <div class="ers-card my-card">
      <div class="slots-head">
        <h2>我的预约（{{ myReservations.length }}）</h2>
      </div>
      <el-table :data="myReservations" style="width: 100%" size="small">
        <el-table-column prop="roomName" label="场地" width="140" />
        <el-table-column prop="reserveDate" label="日期" width="120">
          <template #default="{ row }"><span class="tabular">{{ row.reserveDate }}</span></template>
        </el-table-column>
        <el-table-column prop="timeSlot" label="时段" width="160">
          <template #default="{ row }"><span class="tabular">{{ slotLabel(row.timeSlot) }}</span></template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="dictType(RESERVATION_STATUS, row.status)" effect="light" size="small">
              {{ dictLabel(RESERVATION_STATUS, row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="180">
          <template #default="{ row }">
            <template v-if="row.status === 'BOOKED'">
              <el-button link type="primary" @click="cancel(row)">取消预约</el-button>
              <el-button v-if="canUse(row)" link type="success" @click="markUsed(row)">标记已使用</el-button>
            </template>
            <span v-else class="ers-muted">—</span>
          </template>
        </el-table-column>
        <template #empty><el-empty description="还没有预约记录" :image-size="60" /></template>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { myApi, roomApi } from '@/api'
import { RESERVATION_STATUS, dictLabel, dictType, todayISO } from '@/utils/dict'

const rooms = ref([])
const roomId = ref(null)
const date = ref(todayISO())
const slots = ref([])
const bookableDays = ref(14)
const loading = ref(false)
const myReservations = ref([])

const currentRoom = computed(() => rooms.value.find((r) => r.id === roomId.value))

function disabledDate(d) {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const target = new Date(d)
  target.setHours(0, 0, 0, 0)
  const max = new Date(today)
  max.setDate(max.getDate() + bookableDays.value - 1)
  return target < today || target > max
}

function slotLabel(timeSlot) {
  const start = 9 + Math.floor(timeSlot / 2)
  const minute = timeSlot % 2 === 0 ? '00' : '30'
  const endTotal = 9 * 60 + (timeSlot + 1) * 30
  const endH = String(Math.floor(endTotal / 60)).padStart(2, '0')
  const endM = String(endTotal % 60).padStart(2, '0')
  return `${String(start).padStart(2, '0')}:${minute}-${endH}:${endM}`
}

function slotClass(slot) {
  if (slot.status === 'PAST') return 'past'
  if (slot.status === 'BOOKED') return slot.reservedByMe ? 'mine' : 'taken'
  return 'free'
}

function slotStateText(slot) {
  if (slot.status === 'PAST') return '已过期'
  if (slot.status === 'BOOKED') return slot.reservedByMe ? '我的' : '占用'
  return '可预约'
}

async function loadRooms() {
  rooms.value = await roomApi.list()
  if (!roomId.value && rooms.value.length) {
    const firstEnabled = rooms.value.find((r) => r.status === 1) || rooms.value[0]
    roomId.value = firstEnabled.id
  }
}

async function loadSlots() {
  if (!roomId.value) return
  loading.value = true
  try {
    const data = await roomApi.slots(roomId.value, date.value)
    slots.value = data.slots || []
    bookableDays.value = data.bookableDays || 14
  } finally {
    loading.value = false
  }
}

async function loadMine() {
  const data = await myApi.reservations({ page: 1, size: 50 })
  myReservations.value = data.records || data || []
}

function selectRoom(room) {
  if (room.status !== 1) return
  roomId.value = room.id
  loadSlots()
}

async function onSlotClick(slot) {
  await ElMessageBox.confirm(
    `确认预约 ${currentRoom.value?.name} ${date.value} ${slot.label}？`,
    '预约确认',
    { confirmButtonText: '确认预约', cancelButtonText: '取消' }
  )
  try {
    await roomApi.book({ roomId: roomId.value, date: date.value, slot: slot.slot })
    ElMessage.success('预约成功')
  } catch (e) {
    // 冲突（409，后端文案“该时段已被预约，请刷新后重选”）等失败：提示已由 request.js 统一弹出，
    // 这里静默处理并按 IA §7.6 自动刷新网格，让用户看到最新占用情况；
    // 只刷新数据，不清空当前场地/日期等其它选择。
  } finally {
    loadSlots()
    loadMine()
  }
}

async function cancel(row) {
  await ElMessageBox.confirm(`确认取消 ${row.reserveDate} ${slotLabel(row.timeSlot)} 的预约？`, '取消预约', {
    confirmButtonText: '确认取消',
    cancelButtonText: '再想想'
  })
  await roomApi.cancel(row.id)
  ElMessage.success('已取消')
  loadSlots()
  loadMine()
}

function canUse(row) {
  const start = new Date(`${row.reserveDate}T09:00:00`)
  start.setMinutes(start.getMinutes() + row.timeSlot * 30)
  return new Date() >= start
}

async function markUsed(row) {
  await roomApi.use(row.id)
  ElMessage.success('已标记使用')
  loadMine()
}

function refresh() {
  loadSlots()
  loadMine()
}

onMounted(async () => {
  await loadRooms()
  await loadSlots()
  await loadMine()
})
</script>

<style scoped>
.head-actions {
  display: flex;
  gap: 10px;
}

.room-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 14px;
}

.room-tab {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 2px;
  padding: 10px 14px;
  min-width: 150px;
  border: 1px solid var(--el-border-color);
  border-radius: 10px;
  background: #fff;
  cursor: pointer;
  transition: all var(--ers-dur-fast) var(--ers-ease);
}

.room-tab.active {
  border-color: var(--el-color-primary);
  box-shadow: 0 0 0 3px var(--el-color-primary-light-9);
}

.room-tab:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.room-name {
  font-weight: 600;
  font-size: 14px;
}

.slots-card {
  padding: 18px 20px;
  margin-bottom: 14px;
}

.slots-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.slots-head h2 {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
}

.legend {
  display: flex;
  gap: 14px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.lg {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 3px;
  display: inline-block;
}

.dot.free { background: #dff0e9; border: 1px solid #0a7a57; }
.dot.mine { background: #2b5ce6; }
.dot.taken { background: #f0d3d0; }
.dot.past { background: #e5e7eb; }

.slot-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 8px;
}

.slot {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 2px;
  padding: 10px 12px;
  border-radius: 9px;
  border: 1px solid var(--el-border-color-lighter);
  background: #fff;
  cursor: pointer;
  text-align: left;
  transition: all var(--ers-dur-fast) var(--ers-ease);
}

.slot.free {
  border-color: #b9e3d5;
  background: #f2fbf7;
}

.slot.free:hover {
  border-color: var(--el-color-success);
  transform: translateY(-1px);
}

.slot.mine {
  background: var(--el-color-primary);
  border-color: var(--el-color-primary);
  color: #fff;
}

.slot.taken {
  background: #fbf3f2;
  border-color: #edd6d3;
  color: #a15b54;
  cursor: not-allowed;
}

.slot.past {
  background: #f5f6f8;
  color: #9aa3b2;
  cursor: not-allowed;
}

.slot-label {
  font-size: 13px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}

.slot-state {
  font-size: 11.5px;
  opacity: 0.85;
}

.my-card {
  padding: 18px 20px;
}
</style>
