<template>
  <div class="ers-page" v-loading="loading">
    <div class="ers-page-head">
      <div class="head-left">
        <el-button :icon="ArrowLeft" text @click="$router.back()">返回</el-button>
        <div>
          <h1 class="ers-page-title">{{ detail.title || '投票详情' }}</h1>
          <p class="ers-page-sub">
            <el-tag :type="dictType(VOTE_STATUS, detail.status)" effect="light" size="small">
              {{ dictLabel(VOTE_STATUS, detail.status) }}
            </el-tag>
            <span class="gap">截止 {{ formatDateTime(detail.deadline) }}</span>
            <span class="gap">共 {{ detail.totalVotes || 0 }} 票</span>
          </p>
        </div>
      </div>
    </div>

    <div class="ers-card body">
      <p v-if="detail.description" class="desc">{{ detail.description }}</p>

      <div class="options">
        <div
          v-for="opt in detail.options"
          :key="opt.id"
          class="option"
          :class="{ selected: detail.myOptionId === opt.id, votable: detail.votable }"
          @click="select(opt)"
        >
          <div class="option-top">
            <span class="option-label">
              <el-icon v-if="detail.myOptionId === opt.id" class="check"><CircleCheckFilled /></el-icon>
              {{ opt.label }}
            </span>
            <span v-if="detail.showResult" class="option-count tabular">{{ opt.votes }} 票 · {{ opt.percent }}%</span>
          </div>
          <div v-if="detail.showResult" class="bar">
            <div class="bar-inner" :style="{ width: `${Math.min(opt.percent || 0, 100)}%` }"></div>
          </div>
        </div>
      </div>

      <el-alert
        v-if="!detail.showResult"
        type="info"
        :closable="false"
        show-icon
        title="投票前结果不可见，投出后即可查看实时分布"
      />

      <div class="foot">
        <template v-if="detail.votable">
          <span class="ers-muted">每人只能投一次，提交后不可修改</span>
          <el-button type="primary" size="large" :loading="acting" :disabled="!selectedOptionId" @click="submit">
            确认投票
          </el-button>
        </template>
        <template v-else-if="detail.myOptionId">
          <span class="ers-muted">你已投过票，结果不可修改</span>
          <el-button disabled>已投票</el-button>
        </template>
        <template v-else>
          <span class="ers-muted">{{ detail.status === 'CLOSED' ? '活动已结束' : '活动尚未开始' }}</span>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, CircleCheckFilled } from '@element-plus/icons-vue'
import { voteApi } from '@/api'
import { VOTE_STATUS, dictLabel, dictType, formatDateTime } from '@/utils/dict'

const route = useRoute()
const detail = ref({ options: [] })
const loading = ref(false)
const acting = ref(false)
const selectedOptionId = ref(null)

async function load() {
  loading.value = true
  try {
    detail.value = await voteApi.detail(route.params.id)
  } finally {
    loading.value = false
  }
}

function select(opt) {
  if (!detail.value.votable) return
  selectedOptionId.value = opt.id
}

async function submit() {
  const option = detail.value.options.find((o) => o.id === selectedOptionId.value)
  await ElMessageBox.confirm(`确认投给「${option?.label}」？提交后不可修改。`, '投票确认', {
    confirmButtonText: '确认投票',
    cancelButtonText: '取消'
  })
  acting.value = true
  try {
    await voteApi.submit(detail.value.id, { optionId: selectedOptionId.value })
    ElMessage.success('投票成功')
    load()
  } finally {
    acting.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.head-left {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.gap {
  margin-left: 10px;
}

.body {
  padding: 24px 26px;
  max-width: 860px;
}

.desc {
  margin: 0 0 18px;
  color: var(--el-text-color-regular);
  line-height: 1.8;
  font-size: 13.5px;
}

.options {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 16px;
}

.option {
  padding: 14px 16px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 10px;
  transition: all var(--ers-dur-fast) var(--ers-ease);
}

.option.votable {
  cursor: pointer;
}

.option.votable:hover {
  border-color: var(--el-color-primary-light-5);
}

.option.selected {
  border-color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
}

.option-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.option-label {
  font-size: 14.5px;
  font-weight: 500;
}

.check {
  color: var(--el-color-primary);
  vertical-align: -2px;
  margin-right: 4px;
}

.option-count {
  font-size: 12.5px;
  color: var(--el-text-color-secondary);
}

.bar {
  margin-top: 10px;
  height: 6px;
  border-radius: 999px;
  background: var(--el-fill-color-light);
  overflow: hidden;
}

.bar-inner {
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, #2b5ce6, #5c82ee);
  transition: width var(--ers-dur-enter) var(--ers-ease);
}

.foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid var(--el-border-color-lighter);
}
</style>
