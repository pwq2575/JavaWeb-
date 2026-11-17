<template>
  <div class="ers-page">
    <div class="ers-page-head">
      <div>
        <h1 class="ers-page-title">采购申请</h1>
        <p class="ers-page-sub">{{ auth.isAdmin ? '审批员工提交的采购需求；本期通过后不自动入库' : '提交采购需求，等待管理员审批' }}</p>
      </div>
      <div class="head-actions">
        <el-button :icon="Refresh" @click="load">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="openCreate">提交申请</el-button>
      </div>
    </div>

    <div class="ers-table-card">
      <div class="ers-toolbar">
        <el-select v-model="status" placeholder="状态" clearable style="width: 140px" @change="search">
          <el-option v-for="(item, key) in PROCUREMENT_STATUS" :key="key" :label="item.label" :value="key" />
        </el-select>
        <el-input v-model="keyword" placeholder="搜索名称" clearable style="width: 200px" @keyup.enter="search" @clear="search" />
        <el-button :icon="Search" @click="search">查询</el-button>
      </div>

      <el-table :data="rows" v-loading="loading" style="width: 100%">
        <el-table-column prop="title" label="采购名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="reason" label="申请理由" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">{{ row.reason || '—' }}</template>
        </el-table-column>
        <el-table-column label="申请人" width="110">
          <template #default="{ row }">{{ row.applicantName || '—' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="dictType(PROCUREMENT_STATUS, row.status)" effect="light" size="small">
              {{ dictLabel(PROCUREMENT_STATUS, row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="提交时间" width="150">
          <template #default="{ row }"><span class="tabular">{{ formatDateTime(row.createdAt) }}</span></template>
        </el-table-column>
        <el-table-column label="审批信息" min-width="200">
          <template #default="{ row }">
            <span v-if="row.status === 'PENDING'" class="ers-muted">等待审批</span>
            <span v-else-if="row.status === 'REJECTED'" class="ers-muted">驳回理由：{{ row.approveNote || '未填写' }}</span>
            <span v-else class="ers-muted">{{ formatDateTime(row.decidedAt) }} 已同意</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <template v-if="auth.isAdmin && row.status === 'PENDING'">
              <el-button link type="primary" @click="approve(row)">同意入库</el-button>
              <el-button link type="danger" @click="openReject(row)">驳回</el-button>
            </template>
            <el-button
              v-else-if="!auth.isAdmin && row.status === 'REJECTED'"
              link
              type="danger"
              @click="remove(row)"
            >删除</el-button>
            <span v-else class="ers-muted">—</span>
          </template>
        </el-table-column>
        <template #empty><el-empty description="暂无采购申请" :image-size="80" /></template>
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

    <el-dialog v-model="createVisible" title="提交采购申请" width="460px">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="采购名称" prop="title">
          <el-input v-model="form.title" maxlength="64" show-word-limit placeholder="如：会议室白板笔（黑色）" />
        </el-form-item>
        <el-form-item label="采购理由" prop="reason">
          <el-input v-model="form.reason" type="textarea" :rows="3" maxlength="200" show-word-limit placeholder="说明用途与紧迫程度" />
        </el-form-item>
        <p class="ers-muted">说明：本期采购审批通过后不自动写入资产台账，请管理员后续在「资产管理」中登记。</p>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="acting" @click="submit">提交</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="rejectVisible" title="驳回采购申请" width="420px">
      <el-form label-position="top">
        <el-form-item label="驳回理由（必填）">
          <el-input v-model="rejectNote" type="textarea" :rows="3" maxlength="200" show-word-limit />
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
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import { procurementApi } from '@/api'
import { PROCUREMENT_STATUS, dictLabel, dictType, formatDateTime } from '@/utils/dict'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const loading = ref(false)
const acting = ref(false)
const rows = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const status = ref('')
const keyword = ref('')
const createVisible = ref(false)
const rejectVisible = ref(false)
const formRef = ref()
const current = ref(null)
const rejectNote = ref('')
const form = reactive({ title: '', reason: '' })

const rules = {
  title: [{ required: true, message: '请填写采购名称', trigger: 'blur' }],
  reason: [{ required: true, message: '请填写采购理由', trigger: 'blur' }]
}

async function load() {
  loading.value = true
  try {
    const data = await procurementApi.list({
      page: page.value,
      size: size.value,
      status: status.value || undefined,
      keyword: keyword.value || undefined
    })
    rows.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

function search() {
  page.value = 1
  load()
}

function openCreate() {
  Object.assign(form, { title: '', reason: '' })
  createVisible.value = true
}

async function submit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  acting.value = true
  try {
    await procurementApi.create({ title: form.title, reason: form.reason })
    ElMessage.success('已提交，等待管理员审批')
    createVisible.value = false
    search()
  } finally {
    acting.value = false
  }
}

async function approve(row) {
  await ElMessageBox.confirm(`确认同意「${row.title}」的采购申请？`, '审批确认', {
    confirmButtonText: '同意入库',
    cancelButtonText: '取消'
  })
  await procurementApi.approve(row.id)
  ElMessage.success('已同意')
  load()
}

function openReject(row) {
  current.value = row
  rejectNote.value = ''
  rejectVisible.value = true
}

async function confirmReject() {
  if (!rejectNote.value.trim()) {
    ElMessage.warning('请填写驳回理由')
    return
  }
  acting.value = true
  try {
    await procurementApi.reject(current.value.id, { approveNote: rejectNote.value.trim() })
    ElMessage.success('已驳回')
    rejectVisible.value = false
    load()
  } finally {
    acting.value = false
  }
}

async function remove(row) {
  await ElMessageBox.confirm(`确认删除已驳回的申请「${row.title}」？`, '删除确认', { type: 'warning' })
  await procurementApi.remove(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>

<style scoped>
.head-actions {
  display: flex;
  gap: 10px;
}
</style>
