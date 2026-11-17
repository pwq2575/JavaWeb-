<template>
  <div class="ers-page">
    <div class="ers-page-head">
      <div>
        <h1 class="ers-page-title">文化投票</h1>
        <p class="ers-page-sub">每人每个活动只能投一次且不可修改；截止后自动关闭并公开结果</p>
      </div>
      <div class="head-actions">
        <el-button :icon="Refresh" @click="load">刷新</el-button>
        <el-button v-if="auth.isAdmin" type="primary" :icon="Plus" @click="openCreate">发布投票</el-button>
      </div>
    </div>

    <div v-loading="loading" class="vote-grid">
      <article v-for="item in rows" :key="item.id" class="vote-card" @click="$router.push(`/votes/${item.id}`)">
        <header class="vote-head">
          <el-tag :type="dictType(VOTE_STATUS, item.status)" effect="light" size="small">
            {{ dictLabel(VOTE_STATUS, item.status) }}
          </el-tag>
          <span class="ers-muted tabular">截止 {{ formatDateTime(item.deadline) }}</span>
        </header>
        <h2 class="vote-title">{{ item.title }}</h2>
        <div class="vote-meta">
          <span><el-icon><User /></el-icon> {{ item.totalVotes || 0 }} 人已投</span>
          <el-tag v-if="item.voted" type="success" effect="plain" size="small">已投票</el-tag>
        </div>
        <footer v-if="auth.isAdmin && item.status === 'DRAFT'" class="vote-admin" @click.stop>
          <el-button size="small" @click="openEdit(item)">编辑</el-button>
          <el-button size="small" type="primary" plain @click="publish(item)">发布</el-button>
          <el-button size="small" type="danger" plain @click="remove(item)">删除</el-button>
        </footer>
        <footer v-else-if="auth.isAdmin && item.status === 'PUBLISHED'" class="vote-admin" @click.stop>
          <el-button size="small" @click="close(item)">提前关闭</el-button>
        </footer>
      </article>

      <el-empty v-if="!loading && !rows.length" description="暂无投票活动" />
    </div>

    <el-dialog v-model="editorVisible" :title="editingId ? '编辑投票活动' : '发布投票活动'" width="560px">
      <el-form :model="form" label-position="top">
        <el-form-item label="活动标题">
          <el-input v-model="form.title" maxlength="64" show-word-limit placeholder="如：2026 年度团建目的地投票" />
        </el-form-item>
        <el-form-item label="活动说明">
          <el-input v-model="form.description" type="textarea" :rows="2" maxlength="500" show-word-limit />
        </el-form-item>
        <el-form-item label="截止时间">
          <el-date-picker
            v-model="form.deadline"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
            placeholder="选择截止时间"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="选项（2-8 个）">
          <div class="option-list">
            <div v-for="(opt, index) in form.options" :key="index" class="option-row">
              <el-input v-model="form.options[index]" placeholder="选项内容" maxlength="64" />
              <el-button :icon="Delete" circle plain :disabled="form.options.length <= 2" @click="form.options.splice(index, 1)" />
            </div>
            <el-button :icon="Plus" plain :disabled="form.options.length >= 8" @click="form.options.push('')">添加选项</el-button>
          </div>
        </el-form-item>
        <p class="ers-muted">草稿状态可编辑与删除；发布后仅可提前关闭，不能修改选项。</p>
      </el-form>
      <template #footer>
        <el-button @click="editorVisible = false">取消</el-button>
        <el-button type="primary" :loading="acting" @click="save">{{ editingId ? '保存草稿' : '创建草稿' }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Plus, Refresh, User } from '@element-plus/icons-vue'
import { voteApi } from '@/api'
import { VOTE_STATUS, dictLabel, dictType, formatDateTime } from '@/utils/dict'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const loading = ref(false)
const acting = ref(false)
const rows = ref([])
const editorVisible = ref(false)
const editingId = ref(null)

const form = reactive({ title: '', description: '', deadline: '', options: ['', ''] })

async function load() {
  loading.value = true
  try {
    const data = await voteApi.list({ status: auth.isAdmin ? undefined : undefined })
    rows.value = data.records || data || []
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  Object.assign(form, { title: '', description: '', deadline: '', options: ['', ''] })
  editorVisible.value = true
}

function openEdit(item) {
  editingId.value = item.id
  Object.assign(form, {
    title: item.title,
    description: item.description || '',
    deadline: item.deadline,
    options: (item.options || []).map((o) => o.label || o)
  })
  editorVisible.value = true
}

async function save() {
  if (!form.title.trim()) {
    ElMessage.warning('请填写活动标题')
    return
  }
  const options = form.options.map((o) => o.trim()).filter(Boolean)
  if (options.length < 2) {
    ElMessage.warning('至少需要 2 个选项')
    return
  }
  if (!form.deadline) {
    ElMessage.warning('请选择截止时间')
    return
  }
  const payload = { title: form.title.trim(), description: form.description || undefined, deadline: form.deadline, options }
  acting.value = true
  try {
    if (editingId.value) await voteApi.update(editingId.value, payload)
    else await voteApi.create(payload)
    ElMessage.success(editingId.value ? '草稿已保存' : '草稿已创建，发布后员工可见')
    editorVisible.value = false
    load()
  } finally {
    acting.value = false
  }
}

async function publish(item) {
  await ElMessageBox.confirm(`确认发布「${item.title}」？发布后选项不可修改。`, '发布确认', { confirmButtonText: '发布', cancelButtonText: '取消' })
  await voteApi.publish(item.id)
  ElMessage.success('已发布')
  load()
}

async function close(item) {
  await ElMessageBox.confirm(`确认提前关闭「${item.title}」？关闭后不能再投票。`, '关闭确认', {
    confirmButtonText: '关闭投票',
    cancelButtonText: '取消'
  })
  await voteApi.close(item.id)
  ElMessage.success('已关闭')
  load()
}

async function remove(item) {
  await ElMessageBox.confirm(`确认删除草稿「${item.title}」？`, '删除确认', { type: 'warning' })
  await voteApi.remove(item.id)
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

.vote-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 14px;
}

.vote-card {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 18px 20px;
  background: #fff;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: var(--ers-radius-card);
  cursor: pointer;
  transition: all var(--ers-dur-standard) var(--ers-ease);
}

.vote-card:hover {
  border-color: var(--el-color-primary-light-5);
  transform: translateY(-2px);
  box-shadow: var(--ers-shadow-card);
}

.vote-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.vote-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  line-height: 1.45;
}

.vote-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: var(--el-text-color-secondary);
  font-size: 12.5px;
}

.vote-meta .el-icon {
  vertical-align: -2px;
  margin-right: 2px;
}

.vote-admin {
  display: flex;
  gap: 8px;
  padding-top: 10px;
  border-top: 1px dashed var(--el-border-color-lighter);
}

.option-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
}

.option-row {
  display: flex;
  gap: 8px;
}
</style>
