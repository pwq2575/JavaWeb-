<template>
  <div class="ers-page">
    <div class="ers-page-head">
      <div>
        <h1 class="ers-page-title">场地管理</h1>
        <p class="ers-page-sub">场地停用后不可新预约，不影响已有预约；仍有未来预约的场地不能删除</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreate">新增场地</el-button>
    </div>

    <div class="ers-table-card">
      <el-table :data="rows" v-loading="loading" style="width: 100%">
        <el-table-column prop="id" label="ID" width="70">
          <template #default="{ row }"><span class="tabular">{{ row.id }}</span></template>
        </el-table-column>
        <el-table-column prop="name" label="场地名称" min-width="180" />
        <el-table-column prop="location" label="位置" min-width="160">
          <template #default="{ row }">{{ row.location || '—' }}</template>
        </el-table-column>
        <el-table-column label="容量" width="100">
          <template #default="{ row }"><span class="tabular">{{ row.capacity }} 人</span></template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" effect="light" size="small">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button v-if="row.status === 1" link type="warning" @click="save(row, { status: 0 })">停用</el-button>
            <el-button v-else link type="success" @click="save(row, { status: 1 })">启用</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty><el-empty description="暂无场地，请先新增" :image-size="80" /></template>
      </el-table>
    </div>

    <el-dialog v-model="visible" :title="editingId ? '编辑场地' : '新增场地'" width="460px">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="场地名称" prop="name">
          <el-input v-model="form.name" maxlength="32" placeholder="如 A303 会议室" />
        </el-form-item>
        <el-form-item label="位置" prop="location">
          <el-input v-model="form.location" maxlength="64" placeholder="如 3 号楼 3 层" />
        </el-form-item>
        <el-form-item label="容量（人）" prop="capacity">
          <el-input-number v-model="form.capacity" :min="1" :max="500" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <p class="ers-muted">预约开放 09:00-18:00，按 30 分钟切片，可约未来 14 天。</p>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="acting" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { adminApi } from '@/api'

const loading = ref(false)
const acting = ref(false)
const rows = ref([])
const visible = ref(false)
const editingId = ref(null)
const formRef = ref(null)

const form = reactive({ name: '', location: '', capacity: 10, status: 1 })
const rules = {
  name: [{ required: true, message: '请输入场地名称', trigger: 'blur' }]
}

async function load() {
  loading.value = true
  try {
    rows.value = await adminApi.rooms()
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  Object.assign(form, { name: '', location: '', capacity: 10, status: 1 })
  visible.value = true
}

function openEdit(row) {
  editingId.value = row.id
  Object.assign(form, {
    name: row.name,
    location: row.location || '',
    capacity: row.capacity || 1,
    status: row.status
  })
  visible.value = true
}

async function submit() {
  await formRef.value.validate()
  acting.value = true
  try {
    if (editingId.value) await adminApi.updateRoom(editingId.value, { ...form })
    else await adminApi.createRoom({ ...form })
    ElMessage.success(editingId.value ? '已保存' : '已新增')
    visible.value = false
    load()
  } finally {
    acting.value = false
  }
}

async function save(row, patch) {
  await adminApi.updateRoom(row.id, {
    name: row.name,
    location: row.location,
    capacity: row.capacity,
    status: patch.status
  })
  ElMessage.success(patch.status === 1 ? '已启用' : '已停用')
  load()
}

async function remove(row) {
  await ElMessageBox.confirm(`确认删除「${row.name}」？仍有未来预约时无法删除。`, '删除确认', { type: 'warning' })
  await adminApi.removeRoom(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>
