<template>
  <div class="ers-page">
    <div class="ers-page-head">
      <div>
        <h1 class="ers-page-title">资产台账管理</h1>
        <p class="ers-page-sub">增删改查 + 停用/启用；已借出资产需归还后才能修改或停用；有流转记录的资产只能停用</p>
      </div>
      <div class="head-actions">
        <el-button :icon="Download" @click="exportExcel">导出台账</el-button>
        <el-button type="primary" :icon="Plus" @click="openCreate">新增资产</el-button>
      </div>
    </div>

    <div class="ers-table-card">
      <div class="ers-toolbar">
        <el-input v-model="query.keyword" placeholder="编号 / 名称" clearable style="width: 220px" @keyup.enter="search" />
        <el-select v-model="query.status" placeholder="状态" clearable style="width: 140px">
          <el-option v-for="(label, key) in ASSET_STATUS" :key="key" :label="label" :value="key" />
        </el-select>
        <el-select v-model="query.category" placeholder="分类" clearable style="width: 140px">
          <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
        </el-select>
        <el-select v-model="query.holderId" placeholder="持有者" clearable filterable style="width: 150px">
          <el-option v-for="u in holders" :key="u.id" :label="u.realName || u.username" :value="u.id" />
        </el-select>
        <el-checkbox v-model="query.overdue" style="margin-right: 4px">仅看逾期</el-checkbox>
        <el-button type="primary" :icon="Search" @click="search">查询</el-button>
        <el-button :icon="RefreshRight" @click="reset">重置</el-button>
      </div>

      <el-table :data="rows" v-loading="loading" style="width: 100%">
        <el-table-column label="编号" width="120">
          <template #default="{ row }"><span class="tabular">{{ row.code }}</span></template>
        </el-table-column>
        <el-table-column prop="name" label="名称" min-width="160">
          <template #default="{ row }">
            <el-link type="primary" :underline="false" @click="$router.push(`/assets/${row.id}`)">{{ row.name }}</el-link>
          </template>
        </el-table-column>
        <el-table-column prop="category" label="分类" width="110" />
        <el-table-column label="价值" width="110" align="right">
          <template #default="{ row }"><span class="tabular">{{ formatMoney(row.value) }}</span></template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="dictType(ASSET_STATUS, row.status)" effect="light" size="small">
              {{ dictLabel(ASSET_STATUS, row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="当前持有者" width="120">
          <template #default="{ row }">{{ row.holderName || '—' }}</template>
        </el-table-column>
        <el-table-column label="应归还日" width="130">
          <template #default="{ row }">
            <span class="tabular" :class="{ danger: row.overdue }">{{ row.dueDate || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="210" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button v-if="row.status === 'IN_STOCK'" link type="warning" @click="toggle(row, false)">停用</el-button>
            <el-button v-if="row.status === 'DISABLED'" link type="success" @click="toggle(row, true)">启用</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="ers-pager">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next"
          @current-change="load"
          @size-change="search"
        />
      </div>
    </div>

    <el-dialog v-model="visible" :title="editingId ? '编辑资产' : '新增资产'" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="资产编号" prop="code">
          <el-input v-model="form.code" maxlength="32" placeholder="如 A-0301" />
        </el-form-item>
        <el-form-item label="资产名称" prop="name">
          <el-input v-model="form.name" maxlength="64" />
        </el-form-item>
        <el-form-item label="分类" prop="category">
          <el-select v-model="form.category" filterable allow-create placeholder="选择或输入分类" style="width: 100%">
            <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item label="价值（元）" prop="value">
          <el-input-number v-model="form.value" :min="0" :precision="2" :step="100" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" maxlength="255" show-word-limit />
        </el-form-item>
        <p class="ers-muted">价值 ≥ 高价值阈值（默认 500 元）时，低信誉用户不可借用。</p>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="acting" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Download, Plus, RefreshRight, Search } from '@element-plus/icons-vue'
import { adminApi, exportApi } from '@/api'
import { ASSET_STATUS, dictLabel, dictType, formatMoney } from '@/utils/dict'

const loading = ref(false)
const acting = ref(false)
const rows = ref([])
const total = ref(0)
const categories = ref([])
const holders = ref([])
const visible = ref(false)
const editingId = ref(null)
const formRef = ref(null)

const query = reactive({ page: 1, size: 10, keyword: '', status: '', category: '', holderId: null, overdue: false })
const form = reactive({ code: '', name: '', category: '', value: 0, description: '' })

const rules = {
  code: [{ required: true, message: '请输入资产编号', trigger: 'blur' }],
  name: [{ required: true, message: '请输入资产名称', trigger: 'blur' }],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }],
  value: [{ required: true, message: '请输入价值', trigger: 'blur' }]
}

async function load() {
  loading.value = true
  try {
    const data = await adminApi.assets({
      page: query.page,
      size: query.size,
      keyword: query.keyword || undefined,
      status: query.status || undefined,
      category: query.category || undefined,
      holderId: query.holderId || undefined,
      overdue: query.overdue ? true : undefined
    })
    rows.value = data.records || []
    total.value = data.total || 0
    categories.value = [...new Set(rows.value.map((r) => r.category).filter(Boolean))]
  } finally {
    loading.value = false
  }
}

async function loadHolders() {
  const data = await adminApi.users({ page: 1, size: 100 })
  holders.value = data.records || []
}

function search() {
  query.page = 1
  load()
}

function reset() {
  Object.assign(query, { page: 1, keyword: '', status: '', category: '', holderId: null, overdue: false })
  load()
}

function openCreate() {
  editingId.value = null
  Object.assign(form, { code: '', name: '', category: '', value: 0, description: '' })
  visible.value = true
}

function openEdit(row) {
  editingId.value = row.id
  Object.assign(form, {
    code: row.code,
    name: row.name,
    category: row.category,
    value: Number(row.value || 0),
    description: row.description || ''
  })
  visible.value = true
}

async function save() {
  await formRef.value.validate()
  acting.value = true
  try {
    const payload = { ...form, value: Number(form.value) }
    if (editingId.value) await adminApi.updateAsset(editingId.value, payload)
    else await adminApi.createAsset(payload)
    ElMessage.success(editingId.value ? '已保存' : '已新增')
    visible.value = false
    load()
  } finally {
    acting.value = false
  }
}

async function toggle(row, enable) {
  await ElMessageBox.confirm(
    enable ? `确认启用「${row.name}」？` : `确认停用「${row.name}」？停用后不可借用。`,
    enable ? '启用确认' : '停用确认'
  )
  if (enable) await adminApi.enableAsset(row.id)
  else await adminApi.disableAsset(row.id)
  ElMessage.success(enable ? '已启用' : '已停用')
  load()
}

async function remove(row) {
  await ElMessageBox.confirm(`确认删除「${row.name}」？有流转记录的资产只能停用。`, '删除确认', { type: 'warning' })
  await adminApi.removeAsset(row.id)
  ElMessage.success('已删除')
  load()
}

async function exportExcel() {
  await exportApi.assets()
  ElMessage.success('已开始下载')
}

onMounted(async () => {
  await loadHolders()
  load()
})
</script>

<style scoped>
.head-actions {
  display: flex;
  gap: 10px;
}

.danger {
  color: var(--el-color-danger);
  font-weight: 500;
}
</style>
