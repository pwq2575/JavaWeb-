<template>
  <div class="ers-page">
    <div class="ers-page-head">
      <div>
        <h1 class="ers-page-title">我的信誉</h1>
        <p class="ers-page-sub">信誉分低于阈值时不能借用高价值资产；逾期每日扣 5 分，场地爽约一次性扣 5 分</p>
      </div>
    </div>

    <div class="credit-grid">
      <div class="ers-card score-card">
        <div class="ers-kpi-label">当前信誉分</div>
        <div class="score" :class="scoreClass">{{ data.creditScore }}</div>
        <div class="ers-muted">限制阈值 {{ data.threshold }} 分</div>
        <el-progress
          :percentage="Math.min(data.creditScore, 100)"
          :stroke-width="8"
          :show-text="false"
          :color="progressColor"
          class="progress"
        />
        <el-tag v-if="data.restricted" type="warning" effect="light">已受限：不可借高价值资产</el-tag>
        <el-tag v-else type="success" effect="light">状态正常</el-tag>
      </div>

      <div class="ers-card rules-card">
        <h2>扣分规则</h2>
        <ul>
          <li><strong>资产逾期</strong>：每逾期 1 天扣 5 分，同一资产同一天只扣一次；续借成功即停止扣分。</li>
          <li><strong>场地爽约</strong>：预约结束仍未标记使用且未取消，一次性扣 5 分。</li>
          <li><strong>分数下限</strong>：信誉分最低为 0，不会为负。</li>
          <li><strong>影响范围</strong>：仅限制借用高价值资产（价值 ≥ 500 元），不影响低价值资产、场地预约与投票。</li>
        </ul>
      </div>
    </div>

    <div class="ers-table-card">
      <div class="ers-toolbar">
        <h2 class="table-title">扣分记录</h2>
      </div>
      <el-table :data="data.logs" style="width: 100%">
        <el-table-column label="时间" width="170">
          <template #default="{ row }"><span class="tabular">{{ formatDateTime(row.createdAt) }}</span></template>
        </el-table-column>
        <el-table-column label="类型" width="140">
          <template #default="{ row }">{{ CREDIT_REASON[row.reason] || row.reason }}</template>
        </el-table-column>
        <el-table-column label="分值" width="100">
          <template #default="{ row }">
            <span class="tabular" :class="row.delta < 0 ? 'neg' : 'pos'">{{ row.delta > 0 ? '+' + row.delta : row.delta }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="说明" min-width="240">
          <template #default="{ row }">{{ row.remark || '—' }}</template>
        </el-table-column>
        <template #empty><el-empty description="没有扣分记录，继续保持" :image-size="70" /></template>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive } from 'vue'
import { myApi } from '@/api'
import { CREDIT_REASON, formatDateTime } from '@/utils/dict'

const data = reactive({ creditScore: 100, threshold: 60, restricted: false, logs: [] })

const scoreClass = computed(() => ({
  warn: data.creditScore < data.threshold + 10 && data.creditScore >= data.threshold,
  danger: data.creditScore < data.threshold
}))

const progressColor = computed(() => (data.restricted ? '#b3261e' : '#0a7a57'))

onMounted(async () => {
  const result = await myApi.credit()
  Object.assign(data, result)
})
</script>

<style scoped>
.credit-grid {
  display: grid;
  grid-template-columns: 300px 1fr;
  gap: 14px;
  margin-bottom: 14px;
}

.score-card {
  padding: 22px 22px 18px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  align-items: flex-start;
}

.score {
  font-size: 48px;
  font-weight: 700;
  line-height: 1;
  font-variant-numeric: tabular-nums;
  color: var(--el-color-success);
}

.score.warn { color: var(--el-color-warning); }
.score.danger { color: var(--el-color-danger); }

.progress {
  width: 100%;
  margin: 6px 0 10px;
}

.rules-card {
  padding: 20px 22px;
}

.rules-card h2,
.table-title {
  margin: 0 0 10px;
  font-size: 15px;
  font-weight: 600;
}

.rules-card ul {
  margin: 0;
  padding-left: 18px;
  color: var(--el-text-color-regular);
  font-size: 13px;
  line-height: 2;
}

.table-title {
  margin: 0;
}

.neg { color: var(--el-color-danger); font-weight: 600; }
.pos { color: var(--el-color-success); font-weight: 600; }

@media (max-width: 900px) {
  .credit-grid {
    grid-template-columns: 1fr;
  }
}
</style>
