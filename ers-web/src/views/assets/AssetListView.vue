<template>
  <div class="ers-page">
    <div class="ers-page-head">
      <div>
        <h1 class="ers-page-title">资产台账</h1>
        <p class="ers-page-sub">共 {{ total }} 件资产 · 点击行查看详情与流转记录</p>
      </div>
      <el-button :icon="Refresh" @click="load">刷新</el-button>
    </div>

    <!-- 四段式 1：筛选区（可折叠） -->
    <div class="ers-table-card">
      <div class="ers-toolbar">
        <el-input
          v-model="query.keyword"
          placeholder="搜索名称或编号"
          clearable
          style="width: 220px"
          :prefix-icon="Search"
          @keyup.enter="search"
          @clear="search"
        />
        <el-select v-model="query.status" placeholder="状态" clearable style="width: 130px" @change="search">
          <el-option v-for="(item, key) in ASSET_STATUS" :key="key" :label="item.label" :value="key" />
        </el-select>
        <el-select v-model="query.category" placeholder="分类" clearable style="width: 140px" @change="search">
          <el-option v-for="item in categories" :key="item" :label="item" :value="item" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="search">查询</el-button>
        <el-button :icon="RefreshLeft" @click="reset">重置</el-button>
        <div class="spacer"></div>
        <el-button v-if="auth.isAdmin" :icon="Plus" type="primary" plain @click="$router.push('/admin/assets')">
          资产维护
        </el-button>
      </div>

      <el-table :data="rows" v-loading="loading" style="width: 100%" @row-click="openDetail">
        <el-table-column prop="code" label="编号" width="120">
          <template #default="{ row }"><span class="tabular">{{ row.code }}</span></template>
        </el-table-column>
        <el-table-column prop="name" label="名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="category" label="分类" width="110" />
        <el-table-column label="价值" width="120" align="right">
          <template #default="{ row }"><span class="tabular">{{ formatMoney(row.value) }}</span></template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="dictType(ASSET_STATUS, row.status)" effect="light" size="small">
              {{ dictLabel(ASSET_STATUS, row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="持有者" width="120">
          <template #default="{ row }">{{ row.holderName || '—' }}</template>
        </el-table-column>
        <el-table-column label="应归还" width="140">
          <template #default="{ row }">
            <span v-if="row.dueDate" :class="{ 'is-overdue': row.overdue }" class="tabular">
              {{ row.dueDate }}<template v-if="row.overdue"> · 逾期</template>
            </span>
            <span v-else class="ers-muted">—</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click.stop="openDetail(row)">详情</el-button>
            <el-button v-if="row.canBorrow" link type="success" @click.stop="borrow(row)">借出</el-button>
            <el-button v-else-if="row.canReturn" link type="warning" @click.stop="returnAsset(row)">归还</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="没有符合条件的资产" :image-size="80" />
        </template>
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
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, RefreshLeft, Search } from '@element-plus/icons-vue'
import { assetApi } from '@/api'
import { ASSET_STATUS, dictLabel, dictType, formatMoney } from '@/utils/dict'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()

const loading = ref(false)
const rows = ref([])
const total = ref(0)
const categories = ['电子设备', '办公家具', '会议室设备', '工具', '其他']

const query = reactive({ page: 1, size: 10, keyword: '', status: '', category: '' })

async function load() {
  loading.value = true
  try {
    const data = await assetApi.list({
      page: query.page,
      size: query.size,
      keyword: query.keyword || undefined,
      status: query.status || undefined,
      category: query.category || undefined
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
  query.keyword = ''
  query.status = ''
  query.category = ''
  search()
}

function openDetail(row) {
  router.push(`/assets/${row.id}`)
}

async function borrow(row) {
  await ElMessageBox.confirm(
    `确认借出「${row.name}」？借期 3 天，应归还日 ${row.dueDate || '借出后 3 天'}。`,
    '借出确认',
    { confirmButtonText: '确认借出', cancelButtonText: '取消', type: 'info' }
  )
  await assetApi.borrow(row.id)
  ElMessage.success('借出成功')
  load()
}

async function returnAsset(row) {
  await ElMessageBox.confirm(`确认归还「${row.name}」？`, '归还确认', {
    confirmButtonText: '确认归还',
    cancelButtonText: '取消'
  })
  await assetApi.returnAsset(row.id)
  ElMessage.success('归还成功')
  load()
}

onMounted(load)
</script>

<style scoped>
.spacer {
  flex: 1;
}

.is-overdue {
  color: var(--el-color-danger);
  font-weight: 500;
}

:deep(.el-table__row) {
  cursor: pointer;
}
</style>
