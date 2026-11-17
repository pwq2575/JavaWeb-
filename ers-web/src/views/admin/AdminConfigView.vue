<template>
  <div class="ers-page" v-loading="loading">
    <div class="ers-page-head">
      <div>
        <h1 class="ers-page-title">策略配置</h1>
        <p class="ers-page-sub">调整信誉阈值、高价值资产阈值、借期与续借上限；保存后立即对全站生效</p>
      </div>
      <el-button :icon="Refresh" @click="load">刷新</el-button>
    </div>

    <div class="ers-card form-card">
      <el-form :model="form" label-position="top">
        <el-form-item label="信誉阈值（分）">
          <el-radio-group v-model="form.creditThreshold">
            <el-radio-button v-for="v in [50, 60, 70, 80]" :key="v" :value="v">{{ v }}</el-radio-button>
          </el-radio-group>
          <p class="ers-muted hint">信誉分低于该值时，不能借用价值达到高价值阈值的资产；普通用户默认 60 分。</p>
        </el-form-item>

        <el-form-item label="高价值资产阈值（元）">
          <el-input-number v-model="form.highValueThreshold" :min="0" :max="1000000" :step="100" controls-position="right" />
          <p class="ers-muted hint">资产价值 ≥ 该值即视为高价值，默认 500 元。</p>
        </el-form-item>

        <el-form-item label="固定借期（天）">
          <el-input-number v-model="form.borrowDays" :min="1" :max="30" controls-position="right" />
          <p class="ers-muted hint">借出时应归还日 = 借出日 + 该天数，默认 3 天。</p>
        </el-form-item>

        <el-form-item label="最大续借次数（次）">
          <el-input-number v-model="form.renewMaxTimes" :min="0" :max="10" controls-position="right" />
          <p class="ers-muted hint">每次续借 +1 天，默认 3 次；达到上限后需归还。</p>
        </el-form-item>

        <div class="foot">
          <span class="ers-muted">最近更新：{{ formatDateTime(data.updatedAt) || '—' }}</span>
          <div class="foot-actions">
            <el-button @click="load">撤销修改</el-button>
            <el-button type="primary" :loading="acting" @click="save">保存配置</el-button>
          </div>
        </div>
      </el-form>
    </div>

    <div class="ers-card note-card">
      <h2>配置影响面</h2>
      <ul>
        <li>信誉阈值调高 = 更严格：受影响用户增多的同时，低价值资产、场地与投票不受影响。</li>
        <li>高价值阈值调低 = 更多资产被判定为高价值，低信誉用户的借用范围进一步收窄。</li>
        <li>借期与续借上限仅影响新发生的借出与续借，不改动进行中的应归还日。</li>
        <li>所有配置变更会写入系统配置表并记录操作人，供审计追溯。</li>
      </ul>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { adminApi } from '@/api'
import { formatDateTime } from '@/utils/dict'

const loading = ref(false)
const acting = ref(false)
const data = reactive({ updatedAt: null })
const form = reactive({ creditThreshold: 60, highValueThreshold: 500, borrowDays: 3, renewMaxTimes: 3 })

async function load() {
  loading.value = true
  try {
    const res = await adminApi.config()
    Object.assign(data, res)
    Object.assign(form, {
      creditThreshold: Number(res.creditThreshold),
      highValueThreshold: Number(res.highValueThreshold),
      borrowDays: Number(res.borrowDays),
      renewMaxTimes: Number(res.renewMaxTimes)
    })
  } finally {
    loading.value = false
  }
}

async function save() {
  acting.value = true
  try {
    await adminApi.updateConfig({
      creditThreshold: String(form.creditThreshold),
      highValueThreshold: form.highValueThreshold,
      borrowDays: form.borrowDays,
      renewMaxTimes: form.renewMaxTimes
    })
    ElMessage.success('配置已保存，已对全站生效')
    load()
  } finally {
    acting.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.form-card {
  padding: 24px 26px;
  max-width: 760px;
}

.hint {
  margin: 6px 0 0;
  font-size: 12.5px;
  line-height: 1.7;
}

.foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 16px;
  margin-top: 8px;
  border-top: 1px solid var(--el-border-color-lighter);
}

.foot-actions {
  display: flex;
  gap: 10px;
}

.note-card {
  margin-top: 14px;
  padding: 18px 22px;
  max-width: 760px;
}

.note-card h2 {
  margin: 0 0 8px;
  font-size: 14.5px;
  font-weight: 600;
}

.note-card ul {
  margin: 0;
  padding-left: 18px;
  color: var(--el-text-color-regular);
  font-size: 13px;
  line-height: 2;
}
</style>
