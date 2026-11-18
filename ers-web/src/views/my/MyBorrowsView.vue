<template>
  <div class="ers-page">
    <div class="ers-page-head">
      <div>
        <h1 class="ers-page-title">我的借用</h1>
        <p class="ers-page-sub">借期 3 天，可自助续借 3 次（每次 +1 天）；逾期每日扣 5 分</p>
      </div>
      <div class="head-actions">
        <el-button :icon="Download" @click="exportExcel">导出记录</el-button>
        <el-button :icon="Refresh" @click="load">刷新</el-button>
      </div>
    </div>

    <div class="ers-table-card">
      <el-table :data="rows" v-loading="loading" style="width: 100%">
        <el-table-column prop="code" label="编号" width="120">
          <template #default="{ row }"><span class="tabular">{{ row.code }}</span></template>
        </el-table-column>
        <el-table-column label="资产名称" min-width="180">
          <template #default="{ row }">
            <el-link type="primary" :underline="false" @click="$router.push(`/assets/${row.id}`)">{{ row.name }}</el-link>
          </template>
        </el-table-column>
        <el-table-column prop="category" label="分类" width="110" />
        <el-table-column label="借出时间" width="150">
          <template #default="{ row }"><span class="tabular">{{ formatDateTime(row.borrowAt) }}</span></template>
        </el-table-column>
        <el-table-column label="应归还日" width="140">
          <template #default="{ row }">
            <span class="tabular" :class="{ danger: row.overdue }">{{ row.dueDate }}</span>
          </template>
        </el-table-column>
        <el-table-column label="剩余/逾期" width="120">
          <template #default="{ row }">
            <el-tag v-if="row.overdue" type="danger" effect="light" size="small">已逾期</el-tag>
            <span v-else class="tabular">{{ row.daysLeft }} 天</span>
          </template>
        </el-table-column>
        <el-table-column label="已续借" width="100">
          <template #default="{ row }"><span class="tabular">{{ row.renewCount || 0 }}/3</span></template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.canRenew" link type="primary" @click="renew(row)">续借</el-button>
            <el-button v-if="row.canReturn" link type="warning" @click="returnAsset(row)">归还</el-button>
            <span v-if="row.pendingTransferId" class="ers-muted">有接手申请待处理</span>
          </template>
        </el-table-column>
        <template #empty><el-empty description="当前没有在借资产" :image-size="80" /></template>
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
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Download, Refresh } from '@element-plus/icons-vue'
import { assetApi, exportApi, myApi } from '@/api'
import { formatDateTime } from '@/utils/dict'

const loading = ref(false)
const rows = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)

async function load() {
  loading.value = true
  try {
    const data = await myApi.borrows({ page: page.value, size: size.value })
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

async function renew(row) {
  await ElMessageBox.confirm(`确认续借「${row.name}」？应归还日 +1 天。`, '续借确认', {
    confirmButtonText: '确认续借',
    cancelButtonText: '取消'
  })
  await assetApi.renew(row.id)
  ElMessage.success('续借成功')
  load()
}

async function returnAsset(row) {
  await ElMessageBox.confirm(`确认归还「${row.name}」？`, '归还确认', { confirmButtonText: '确认归还', cancelButtonText: '取消' })
  await assetApi.returnAsset(row.id)
  ElMessage.success('归还成功')
  load()
}

async function exportExcel() {
  await exportApi.borrows()
  ElMessage.success('已开始下载')
}

onMounted(load)
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
