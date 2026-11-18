<template>
  <div class="ers-page">
    <div class="ers-page-head">
      <div>
        <h1 class="ers-page-title">你好，{{ auth.displayName }}</h1>
        <p class="ers-page-sub">{{ today }} · 今天要处理 {{ todoCount }} 项事务</p>
      </div>
      <div class="head-actions">
        <el-button :icon="Refresh" @click="load">刷新</el-button>
        <el-button type="primary" @click="$router.push('/assets')">去借资产</el-button>
      </div>
    </div>

    <div class="ers-kpi-grid">
      <div class="ers-kpi">
        <div class="ers-kpi-label">我的信誉分</div>
        <div class="ers-kpi-value" :class="creditClass">{{ data.creditScore }}</div>
        <div class="ers-muted">阈值 {{ data.creditThreshold }} 分；低于阈值限制借用高价值资产</div>
      </div>
      <div class="ers-kpi">
        <div class="ers-kpi-label">在借资产</div>
        <div class="ers-kpi-value tabular">{{ data.myBorrows }}</div>
        <div class="ers-muted">其中 {{ data.dueSoon }} 件 3 天内到期</div>
      </div>
      <div class="ers-kpi">
        <div class="ers-kpi-label">逾期未还</div>
        <div class="ers-kpi-value tabular" :class="{ 'is-danger': data.overdue > 0 }">{{ data.overdue }}</div>
        <div class="ers-muted">逾期每日扣 5 分，续借成功即停止扣分</div>
      </div>
      <div class="ers-kpi">
        <div class="ers-kpi-label">待我处理</div>
        <div class="ers-kpi-value tabular">{{ data.pendingApprovals }}</div>
        <div class="ers-muted">待确认接手 {{ data.todoTransferList?.length || 0 }} 条 · 我的预约 {{ data.myReservations }} 条</div>
      </div>
    </div>

    <div class="grid-2">
      <div class="ers-card panel">
        <div class="panel-head">
          <h2>我的在借（{{ data.todayBorrowList?.length || 0 }}）</h2>
          <el-link type="primary" :underline="false" @click="$router.push('/my/borrows')">全部借用</el-link>
        </div>
        <el-empty v-if="!data.todayBorrowList?.length" description="暂无在借资产" :image-size="72" />
        <ul v-else class="list">
          <li v-for="item in data.todayBorrowList" :key="item.assetId" class="list-item" @click="$router.push(`/assets/${item.assetId}`)">
            <div class="li-main">
              <span class="li-title">{{ item.assetName }}</span>
              <span class="ers-muted">应归还 {{ item.dueDate }}</span>
            </div>
            <el-tag v-if="item.overdueDays > 0" type="danger" effect="light" size="small">逾期 {{ item.overdueDays }} 天</el-tag>
            <el-tag v-else type="success" effect="light" size="small">在借</el-tag>
          </li>
        </ul>
      </div>

      <div class="ers-card panel">
        <div class="panel-head">
          <h2>待我确认的接手申请（{{ data.todoTransferList?.length || 0 }}）</h2>
          <el-link type="primary" :underline="false" @click="$router.push('/transfers')">去处理</el-link>
        </div>
        <el-empty v-if="!data.todoTransferList?.length" description="没有待确认的接手申请" :image-size="72" />
        <ul v-else class="list">
          <li v-for="item in data.todoTransferList" :key="item.transferId" class="list-item">
            <div class="li-main">
              <span class="li-title">{{ item.assetName }}</span>
              <span class="ers-muted">{{ item.toUserName }} 申请接手 · {{ formatTime(item.createdAt) }}</span>
            </div>
            <el-button size="small" type="primary" plain @click="$router.push('/transfers')">去确认</el-button>
          </li>
        </ul>
      </div>
    </div>

    <div class="ers-card panel quick">
      <div class="panel-head"><h2>快捷入口</h2></div>
      <div class="quick-grid">
        <button v-for="entry in quickEntries" :key="entry.path" class="quick-item" @click="$router.push(entry.path)">
          <el-icon :size="20"><component :is="entry.icon" /></el-icon>
          <span>{{ entry.title }}</span>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { myApi } from '@/api'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const data = reactive({
  creditScore: 100,
  creditThreshold: 60,
  myBorrows: 0,
  dueSoon: 0,
  overdue: 0,
  pendingApprovals: 0,
  myReservations: 0,
  todayBorrowList: [],
  todoTransferList: []
})

const today = new Date().toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric', weekday: 'long' })

const todoCount = computed(() => (data.pendingApprovals || 0) + (data.overdue || 0))
const creditClass = computed(() => ({
  'is-warn': data.creditScore < data.creditThreshold + 10 && data.creditScore >= data.creditThreshold,
  'is-danger': data.creditScore < data.creditThreshold
}))

const quickEntries = [
  { title: '资产台账', path: '/assets', icon: 'Box' },
  { title: '场地预约', path: '/rooms', icon: 'OfficeBuilding' },
  { title: '采购申请', path: '/procurements', icon: 'ShoppingCart' },
  { title: '文化投票', path: '/votes', icon: 'DataAnalysis' },
  { title: '我的借用', path: '/my/borrows', icon: 'Tickets' },
  { title: '我的信誉', path: '/my/credit', icon: 'Medal' }
]

function formatTime(value) {
  if (!value) return ''
  return String(value).replace('T', ' ').slice(0, 16)
}

async function load() {
  const result = await myApi.workbench()
  Object.assign(data, result)
}

onMounted(load)
</script>

<style scoped>
.head-actions {
  display: flex;
  gap: 10px;
}

.grid-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
  margin-top: 16px;
}

.panel {
  padding: 16px 18px 8px;
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.panel-head h2 {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
}

.list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.list-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 0;
  border-top: 1px solid var(--el-border-color-lighter);
  cursor: pointer;
}

.li-main {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
}

.li-title {
  font-size: 14px;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.quick {
  margin-top: 16px;
  padding-bottom: 18px;
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  gap: 10px;
  margin-top: 8px;
}

.quick-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 16px 8px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 10px;
  background: #fff;
  color: var(--el-text-color-regular);
  cursor: pointer;
  transition: all var(--ers-dur-fast) var(--ers-ease);
  font-size: 13px;
}

.quick-item:hover {
  border-color: var(--el-color-primary-light-5);
  color: var(--el-color-primary);
  transform: translateY(-1px);
}

@media (max-width: 900px) {
  .grid-2 {
    grid-template-columns: 1fr;
  }
}
</style>
