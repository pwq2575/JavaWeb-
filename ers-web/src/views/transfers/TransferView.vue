<template>
  <div class="ers-page">
    <div class="ers-page-head">
      <div>
        <h1 class="ers-page-title">转交审批</h1>
        <p class="ers-page-sub">同事申请接手你手中的资产时，由你确认；同意后换人不换归还日，逾期责任随之转移</p>
      </div>
      <el-button :icon="Refresh" @click="load">刷新</el-button>
    </div>

    <div class="ers-table-card">
      <div class="ers-toolbar">
        <el-radio-group v-model="box" @change="search">
          <el-radio-button value="incoming">待我确认（{{ pendingCount }}）</el-radio-button>
          <el-radio-button value="outgoing">我发起的</el-radio-button>
        </el-radio-group>
        <el-select v-model="status" placeholder="状态" clearable style="width: 140px" @change="search">
          <el-option v-for="(item, key) in TRANSFER_STATUS" :key="key" :label="item.label" :value="key" />
        </el-select>
      </div>

      <el-table :data="rows" v-loading="loading" style="width: 100%">
        <el-table-column label="资产" min-width="180">
          <template #default="{ row }">
            <div class="asset-cell" @click="$router.push(`/assets/${row.assetId}`)">
              <span class="asset-name">{{ row.assetName }}</span>
              <span class="ers-muted tabular">{{ row.assetCode }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="持有者 → 申请人" min-width="170">
          <template #default="{ row }">
            <span>{{ row.fromUserName }}</span>
            <el-icon class="arrow"><Right /></el-icon>
            <span>{{ row.toUserName }}</span>
          </template>
        </el-table-column>
        <el-table-column label="申请说明" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ row.applyReason || '—' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="dictType(TRANSFER_STATUS, row.status)" effect="light" size="small">
              {{ dictLabel(TRANSFER_STATUS, row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="申请时间" width="150">
          <template #default="{ row }"><span class="tabular">{{ formatDateTime(row.createdAt) }}</span></template>
        </el-table-column>
        <el-table-column label="结果" width="160">
          <template #default="{ row }">
            <span v-if="row.status === 'REJECTED'" class="ers-muted">驳回理由：{{ row.rejectReason || '未填写' }}</span>
            <span v-else-if="row.decidedAt" class="tabular ers-muted">{{ formatDateTime(row.decidedAt) }}</span>
            <span v-else class="ers-muted">{{ box === 'incoming' ? '等待你确认' : '等待对方确认' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 'PENDING' && box === 'incoming'">
              <el-button link type="primary" @click="approve(row)">同意</el-button>
              <el-button link type="danger" @click="reject(row)">驳回</el-button>
            </template>
            <span v-else class="ers-muted">无需操作</span>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty :description="box === 'incoming' ? '当前没有待你确认的接手申请' : '你还没有发起过接手申请'" :image-size="80" />
        </template>
      </el-table>

      <div class="ers-pager">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next"
          @current-change="load"
          @size-change="search"
        />
      </div>
    </div>

    <el-dialog v-model="approveVisible" title="确认对方接手" width="440px">
      <p class="dialog-text">
        <strong>{{ current?.toUserName }}</strong> 申请接手「{{ current?.assetName }}」。<br />
        同意后持有人变为 <strong>{{ current?.toUserName }}</strong>，<strong>应归还日不变</strong>，后续逾期责任由对方承担；你将不再持有该资产。
      </p>
      <template #footer>
        <el-button @click="approveVisible = false">取消</el-button>
        <el-button type="primary" :loading="acting" @click="confirmApprove">确认同意</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="rejectVisible" title="驳回接手申请" width="440px">
      <el-form label-position="top">
        <el-form-item label="驳回理由（必填，1-100 字）">
          <el-input v-model="rejectReason" type="textarea" :rows="3" maxlength="100" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button type="danger" :loading="acting" @click="confirmReject">确认驳回</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, Right } from '@element-plus/icons-vue'
import { transferApi } from '@/api'
import { TRANSFER_STATUS, dictLabel, dictType, formatDateTime } from '@/utils/dict'

const loading = ref(false)
const acting = ref(false)
const rows = ref([])
const total = ref(0)
const pendingCount = ref(0)
const page = ref(1)
const size = ref(10)
const box = ref('incoming')
const status = ref('')
const approveVisible = ref(false)
const rejectVisible = ref(false)
const current = ref(null)
const rejectReason = ref('')

async function load() {
  loading.value = true
  try {
    const data = await transferApi.list({
      box: box.value,
      status: status.value || undefined,
      page: page.value,
      size: size.value
    })
    rows.value = data.records || []
    total.value = data.total || 0
    pendingCount.value = data.pendingCount || 0
  } finally {
    loading.value = false
  }
}

function search() {
  page.value = 1
  load()
}

function approve(row) {
  current.value = row
  approveVisible.value = true
}

async function confirmApprove() {
  acting.value = true
  try {
    await transferApi.approve(current.value.id)
    ElMessage.success(`已同意，${current.value.toUserName} 成为新持有人`)
    approveVisible.value = false
    load()
  } finally {
    acting.value = false
  }
}

function reject(row) {
  current.value = row
  rejectReason.value = ''
  rejectVisible.value = true
}

async function confirmReject() {
  if (!rejectReason.value.trim()) {
    ElMessage.warning('请填写驳回理由')
    return
  }
  acting.value = true
  try {
    await transferApi.reject(current.value.id, { rejectReason: rejectReason.value.trim() })
    ElMessage.success('已驳回')
    rejectVisible.value = false
    load()
  } finally {
    acting.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.asset-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
  cursor: pointer;
}

.asset-name {
  font-weight: 500;
}

.arrow {
  margin: 0 6px;
  color: var(--el-text-color-secondary);
}

.dialog-text {
  margin: 0;
  line-height: 1.8;
  color: var(--el-text-color-regular);
  font-size: 13.5px;
}
</style>
