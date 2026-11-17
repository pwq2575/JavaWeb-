<template>
  <div class="ers-page">
    <div class="ers-page-head">
      <div>
        <h1 class="ers-page-title">用户管理</h1>
        <p class="ers-page-sub">管理员可新建、编辑、停用、重置密码与删除用户；不能停用/删除自己，也不能移除最后一个管理员</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreate">新建用户</el-button>
    </div>

    <div class="ers-table-card">
      <div class="ers-toolbar">
        <el-input v-model="query.keyword" placeholder="账号 / 姓名" clearable style="width: 220px" @keyup.enter="search" />
        <el-select v-model="query.role" placeholder="角色" clearable style="width: 130px">
          <el-option label="管理员" value="ADMIN" />
          <el-option label="普通用户" value="USER" />
        </el-select>
        <el-select v-model="query.status" placeholder="状态" clearable style="width: 130px">
          <el-option label="启用" :value="1" />
          <el-option label="停用" :value="0" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="search">查询</el-button>
        <el-button :icon="RefreshRight" @click="reset">重置</el-button>
      </div>

      <el-table :data="rows" v-loading="loading" style="width: 100%">
        <el-table-column prop="id" label="ID" width="70">
          <template #default="{ row }"><span class="tabular">{{ row.id }}</span></template>
        </el-table-column>
        <el-table-column prop="username" label="账号" width="150" />
        <el-table-column prop="realName" label="姓名" width="130" />
        <el-table-column label="角色" width="110">
          <template #default="{ row }">
            <el-tag :type="row.role === 'ADMIN' ? 'warning' : 'info'" effect="light" size="small">
              {{ ROLE[row.role] || row.role }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="信誉分" width="110">
          <template #default="{ row }">
            <span class="tabular" :class="{ danger: row.creditScore < 60 }">{{ row.creditScore }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" effect="light" size="small">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="注册时间" width="170">
          <template #default="{ row }"><span class="tabular">{{ formatDateTime(row.createdAt) }}</span></template>
        </el-table-column>
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="primary" @click="openReset(row)">重置密码</el-button>
            <el-button v-if="row.status === 1" link type="warning" @click="toggle(row, false)">停用</el-button>
            <el-button v-else link type="success" @click="toggle(row, true)">启用</el-button>
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

    <el-dialog v-model="visible" :title="editingId ? '编辑用户' : '新建用户'" width="460px">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="账号" prop="username">
          <el-input v-model="form.username" :disabled="!!editingId" maxlength="32" placeholder="登录账号，创建后不可改" />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="form.realName" maxlength="32" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-radio-group v-model="form.role">
            <el-radio value="USER">普通用户</el-radio>
            <el-radio value="ADMIN">管理员</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="!editingId" label="初始密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="8-32 位，含字母与数字" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="acting" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="resetVisible" title="重置密码" width="420px">
      <el-form label-position="top">
        <el-form-item :label="`为「${resetTarget?.realName || ''}」设置新密码`">
          <el-input v-model="newPassword" type="password" show-password placeholder="8-32 位，含字母与数字" />
        </el-form-item>
        <p class="ers-muted">重置后请线下告知本人，旧密码立即失效。</p>
      </el-form>
      <template #footer>
        <el-button @click="resetVisible = false">取消</el-button>
        <el-button type="primary" :loading="acting" @click="resetPassword">确认重置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, RefreshRight, Search } from '@element-plus/icons-vue'
import { adminApi } from '@/api'
import { ROLE, formatDateTime } from '@/utils/dict'

const PWD_RULE = /^(?=.*[A-Za-z])(?=.*\d)\S{8,32}$/

const loading = ref(false)
const acting = ref(false)
const rows = ref([])
const total = ref(0)
const visible = ref(false)
const editingId = ref(null)
const formRef = ref(null)
const resetVisible = ref(false)
const resetTarget = ref(null)
const newPassword = ref('')

const query = reactive({ page: 1, size: 10, keyword: '', role: '', status: null })
const form = reactive({ username: '', realName: '', role: 'USER', password: '', status: 1 })

const rules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
  password: [
    { required: true, message: '请输入初始密码', trigger: 'blur' },
    { pattern: PWD_RULE, message: '8-32 位，至少包含字母与数字', trigger: 'blur' }
  ]
}

async function load() {
  loading.value = true
  try {
    const data = await adminApi.users({
      page: query.page,
      size: query.size,
      keyword: query.keyword || undefined,
      role: query.role || undefined,
      status: query.status === '' || query.status === null ? undefined : query.status
    })
    rows.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

function search() {
  query.page = 1
  load()
}

function reset() {
  Object.assign(query, { page: 1, keyword: '', role: '', status: null })
  load()
}

function openCreate() {
  editingId.value = null
  Object.assign(form, { username: '', realName: '', role: 'USER', password: '', status: 1 })
  visible.value = true
}

function openEdit(row) {
  editingId.value = row.id
  Object.assign(form, {
    username: row.username,
    realName: row.realName,
    role: row.role,
    password: '',
    status: row.status
  })
  visible.value = true
}

async function save() {
  await formRef.value.validate()
  acting.value = true
  try {
    if (editingId.value) {
      await adminApi.updateUser(editingId.value, {
        realName: form.realName,
        role: form.role,
        status: form.status
      })
    } else {
      await adminApi.createUser({
        username: form.username,
        realName: form.realName,
        role: form.role,
        password: form.password
      })
    }
    ElMessage.success(editingId.value ? '已保存' : '已创建')
    visible.value = false
    load()
  } finally {
    acting.value = false
  }
}

async function toggle(row, enable) {
  await ElMessageBox.confirm(
    enable ? `确认启用「${row.realName}」？` : `确认停用「${row.realName}」？停用后无法登录。`,
    enable ? '启用确认' : '停用确认'
  )
  await adminApi.updateUser(row.id, { realName: row.realName, role: row.role, status: enable ? 1 : 0 })
  ElMessage.success(enable ? '已启用' : '已停用')
  load()
}

function openReset(row) {
  resetTarget.value = row
  newPassword.value = ''
  resetVisible.value = true
}

async function resetPassword() {
  if (!PWD_RULE.test(newPassword.value)) {
    ElMessage.warning('8-32 位，至少包含字母与数字')
    return
  }
  acting.value = true
  try {
    await adminApi.resetPassword(resetTarget.value.id, { newPassword: newPassword.value })
    ElMessage.success('密码已重置')
    resetVisible.value = false
  } finally {
    acting.value = false
  }
}

async function remove(row) {
  await ElMessageBox.confirm(`确认删除「${row.realName}」？有业务数据的用户无法删除，请改为停用。`, '删除确认', {
    type: 'warning'
  })
  await adminApi.removeUser(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>

<style scoped>
.danger {
  color: var(--el-color-danger);
  font-weight: 600;
}
</style>
