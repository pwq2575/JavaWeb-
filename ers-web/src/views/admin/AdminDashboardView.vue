<template>
  <div class="ers-page" v-loading="loading">
    <div class="ers-page-head">
      <div>
        <h1 class="ers-page-title">管理看板</h1>
        <p class="ers-page-sub">全量数据视图（固定范围，不提供时间筛选）；统计口径见 API 契约 §9 #40</p>
      </div>
      <el-button :icon="Refresh" @click="load">刷新</el-button>
    </div>

    <div class="ers-kpi-grid">
      <div v-for="kpi in kpis" :key="kpi.label" class="ers-kpi">
        <div class="ers-kpi-label">{{ kpi.label }}</div>
        <div class="ers-kpi-value tabular" :class="kpi.tone">{{ kpi.value }}</div>
        <div class="ers-kpi-hint">{{ kpi.hint }}</div>
      </div>
    </div>

    <div class="chart-grid">
      <div class="ers-card chart-card span-2">
        <h2>资产类别价值分布</h2>
        <div ref="valueChart" class="chart"></div>
      </div>
      <div class="ers-card chart-card">
        <h2>资产状态分布</h2>
        <div ref="statusChart" class="chart"></div>
      </div>
      <div class="ers-card chart-card span-2">
        <h2>近 6 个月借用趋势</h2>
        <div ref="trendChart" class="chart"></div>
      </div>
      <div class="ers-card chart-card">
        <h2>近 30 天场地使用</h2>
        <div ref="roomChart" class="chart"></div>
      </div>
    </div>

    <div class="ers-card todo-card">
      <h2>待处理事项</h2>
      <div class="todo-row">
        <div class="todo-item" @click="$router.push('/procurements')">
          <span class="ers-muted">采购待审批</span>
          <strong class="tabular">{{ data.pendingProcurements }}</strong>
        </div>
        <div class="todo-item" @click="$router.push('/transfers')">
          <span class="ers-muted">转交待审批</span>
          <strong class="tabular">{{ data.pendingTransfers }}</strong>
        </div>
        <div class="todo-item" @click="$router.push('/admin/users')">
          <span class="ers-muted">低信誉用户</span>
          <strong class="tabular">{{ data.lowCreditUsers }}</strong>
        </div>
        <div class="todo-item" @click="$router.push('/admin/assets')">
          <span class="ers-muted">逾期资产</span>
          <strong class="tabular danger">{{ data.overdue }}</strong>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { adminApi } from '@/api'

const loading = ref(false)
const data = reactive({
  totalAssets: 0,
  inStock: 0,
  borrowed: 0,
  overdue: 0,
  disabled: 0,
  totalUsers: 0,
  lowCreditUsers: 0,
  pendingProcurements: 0,
  pendingTransfers: 0,
  valueByCategory: [],
  monthlyBorrowTrend: [],
  statusDistribution: [],
  roomUsage: []
})

const kpis = computed(() => [
  { label: '资产总数', value: data.totalAssets, hint: `停用 ${data.disabled} 件`, tone: '' },
  { label: '在库', value: data.inStock, hint: '可借用', tone: 'ok' },
  { label: '借出中', value: data.borrowed, hint: '含逾期', tone: '' },
  { label: '逾期未还', value: data.overdue, hint: '每日 -5 分', tone: 'danger' },
  { label: '待审批转交', value: data.pendingTransfers, hint: '换人不换期', tone: 'warn' },
  { label: '待审批采购', value: data.pendingProcurements, hint: '二态审批', tone: 'warn' },
  { label: '用户总数', value: data.totalUsers, hint: '含管理员', tone: '' },
  { label: '低信誉用户', value: data.lowCreditUsers, hint: '低于阈值', tone: 'danger' }
])

const valueChart = ref(null)
const statusChart = ref(null)
const trendChart = ref(null)
const roomChart = ref(null)
const charts = []

const PALETTE = ['#2b5ce6', '#5c82ee', '#0a7a57', '#a85b00', '#b3261e', '#6b7688', '#8f6bd8']

function mount(el, option) {
  if (!el) return
  const chart = echarts.init(el)
  chart.setOption(option)
  charts.push(chart)
}

function render() {
  const axisCommon = {
    grid: { left: 12, right: 16, top: 24, bottom: 8, containLabel: true },
    tooltip: { trigger: 'axis' }
  }

  mount(valueChart.value, {
    color: PALETTE,
    tooltip: { trigger: 'item', formatter: '{b}<br/>¥{c}（{d}%）' },
    legend: { bottom: 0, icon: 'circle', itemWidth: 8, itemHeight: 8, textStyle: { fontSize: 11 } },
    series: [
      {
        type: 'pie',
        radius: ['46%', '70%'],
        center: ['50%', '44%'],
        itemStyle: { borderColor: '#fff', borderWidth: 2 },
        label: { show: false },
        data: data.valueByCategory.map((i) => ({ name: i.name, value: i.value }))
      }
    ]
  })

  mount(statusChart.value, {
    color: PALETTE,
    tooltip: { trigger: 'item' },
    legend: { bottom: 0, icon: 'circle', itemWidth: 8, itemHeight: 8, textStyle: { fontSize: 11 } },
    series: [
      {
        type: 'pie',
        radius: '62%',
        center: ['50%', '44%'],
        label: { fontSize: 11, formatter: '{b}\n{c}' },
        data: data.statusDistribution.map((i) => ({ name: i.name, value: i.value }))
      }
    ]
  })

  mount(trendChart.value, {
    ...axisCommon,
    color: ['#2b5ce6'],
    xAxis: { type: 'category', data: data.monthlyBorrowTrend.map((i) => i.month.slice(2)) },
    yAxis: { type: 'value', minInterval: 1, splitLine: { lineStyle: { color: '#eef1f6' } } },
    series: [
      {
        type: 'bar',
        barWidth: 22,
        itemStyle: { borderRadius: [4, 4, 0, 0] },
        data: data.monthlyBorrowTrend.map((i) => i.count)
      }
    ]
  })

  mount(roomChart.value, {
    grid: { left: 12, right: 20, top: 16, bottom: 8, containLabel: true },
    tooltip: { trigger: 'axis' },
    color: ['#0a7a57'],
    xAxis: { type: 'value', minInterval: 1, splitLine: { lineStyle: { color: '#eef1f6' } } },
    yAxis: {
      type: 'category',
      data: data.roomUsage.map((i) => i.name).reverse(),
      axisLine: { show: false },
      axisTick: { show: false }
    },
    series: [
      {
        type: 'bar',
        barWidth: 14,
        itemStyle: { borderRadius: [0, 4, 4, 0] },
        data: data.roomUsage.map((i) => i.value).reverse()
      }
    ]
  })
}

function dispose() {
  while (charts.length) charts.pop().dispose()
}

async function load() {
  loading.value = true
  try {
    Object.assign(data, await adminApi.dashboard())
    await nextTick()
    dispose()
    render()
  } finally {
    loading.value = false
  }
}

const onResize = () => charts.forEach((c) => c.resize())

onMounted(() => {
  load()
  window.addEventListener('resize', onResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  dispose()
})
</script>

<style scoped>
.chart-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
  margin-bottom: 14px;
}

.chart-card {
  padding: 18px 18px 10px;
}

.chart-card h2 {
  margin: 0 0 6px;
  font-size: 14.5px;
  font-weight: 600;
}

.span-2 {
  grid-column: span 2;
}

.chart {
  height: 240px;
}

.todo-card {
  padding: 18px 20px;
}

.todo-card h2 {
  margin: 0 0 12px;
  font-size: 14.5px;
  font-weight: 600;
}

.todo-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.todo-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 14px 16px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 10px;
  cursor: pointer;
  transition: all var(--ers-dur-fast) var(--ers-ease);
}

.todo-item:hover {
  border-color: var(--el-color-primary-light-5);
  background: var(--el-color-primary-light-9);
}

.todo-item strong {
  font-size: 22px;
  font-weight: 600;
}

.ok { color: var(--el-color-success); }
.warn { color: var(--el-color-warning); }
.danger { color: var(--el-color-danger); }

@media (max-width: 1100px) {
  .chart-grid {
    grid-template-columns: 1fr;
  }
  .span-2 {
    grid-column: span 1;
  }
  .todo-row {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
