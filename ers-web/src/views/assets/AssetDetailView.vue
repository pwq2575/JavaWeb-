<template>
  <div class="ers-page" v-loading="loading">
    <div class="ers-page-head">
      <div class="head-left">
        <el-button :icon="ArrowLeft" text @click="$router.back()">返回</el-button>
        <div>
          <h1 class="ers-page-title">{{ asset.name || '资产详情' }}</h1>
          <p class="ers-page-sub">
            <span class="tabular">{{ asset.code }}</span> · {{ asset.category }} · 价值
            <span class="tabular">{{ formatMoney(asset.value) }}</span>
          </p>
        </div>
      </div>
      <div class="head-actions">
        <el-tag :type="dictType(ASSET_STATUS, asset.status)" effect="light">
          {{ dictLabel(ASSET_STATUS, asset.status) }}
        </el-tag>
        <el-tag v-if="asset.highValue" type="danger" effect="plain">高价值</el-tag>
      </div>
    </div>

    <div v-if="pendingTransfer" class="ers-alert">
      <el-icon><InfoFilled /></el-icon>
      <span>
        {{ pendingTransfer.toUserName }} 申请接手这件资产（现持有者 {{ pendingTransfer.fromUserName }}），
        原因：{{ pendingTransfer.applyReason || '未填写' }}；等待 {{ pendingTransfer.fromUserName }} 确认
      </span>
    </div>

    <div class="detail-grid">
      <!-- 左：信息卡 + 流转时间线 -->
      <div class="left">
        <div class="ers-card section">
          <h2>借用状态</h2>
          <div class="kv-grid">
            <div class="kv"><span>持有者</span><strong>{{ asset.holderName || '在库' }}</strong></div>
            <div class="kv"><span>借出时间</span><strong class="tabular">{{ formatDateTime(asset.borrowAt) }}</strong></div>
            <div class="kv"><span>应归还日</span><strong class="tabular" :class="{ danger: asset.overdue }">{{ asset.dueDate || '—' }}</strong></div>
            <div class="kv"><span>已续借</span><strong class="tabular">{{ asset.renewCount || 0 }} / 3 次</strong></div>
            <div class="kv"><span>剩余天数</span><strong class="tabular" :class="{ danger: asset.overdue }">{{ asset.overdue ? '已逾期' : (asset.daysLeft ?? '—') }}</strong></div>
            <div class="kv"><span>登记时间</span><strong class="tabular">{{ formatDateTime(asset.createdAt) }}</strong></div>
          </div>
          <p v-if="asset.description" class="desc">{{ asset.description }}</p>
        </div>

        <div class="ers-card section">
          <h2>流转记录（{{ records.length }}）</h2>
          <el-empty v-if="!records.length" description="暂无流转记录" :image-size="64" />
          <el-timeline v-else class="timeline">
            <el-timeline-item
              v-for="item in records"
              :key="item.id"
              :timestamp="formatDateTime(item.happenedAt)"
              placement="top"
              :type="item.action === 'RETURN' ? 'success' : item.action === 'TRANSFER' ? 'warning' : 'primary'"
            >
              <div class="tl-title">
                {{ ACTION_LABEL[item.action] || item.action }}
                <span class="ers-muted">· {{ item.operatorName }}</span>
                <span v-if="item.targetName" class="ers-muted">→ {{ item.targetName }}</span>
              </div>
              <div v-if="item.remark" class="ers-muted">{{ item.remark }}</div>
            </el-timeline-item>
          </el-timeline>
        </div>
      </div>

      <!-- 右：动作栏 -->
      <div class="right">
        <div class="ers-card section actions">
          <h2>可执行操作</h2>
          <el-button
            v-if="asset.canBorrow"
            type="primary"
            size="large"
            class="action"
            :loading="acting"
            @click="borrow"
          >借出这件资产</el-button>
          <el-button
            v-if="asset.canRenew"
            type="success"
            plain
            size="large"
            class="action"
            :loading="acting"
            @click="renew"
          >自助续借（+1 天）</el-button>
          <el-button
            v-if="asset.canReturn"
            type="warning"
            plain
            size="large"
            class="action"
            :loading="acting"
            @click="returnAsset"
          >归还</el-button>

          <el-button
            v-if="asset.canApplyTransfer"
            plain
            size="large"
            class="action"
            @click="openApply"
          >申请接手</el-button>

          <p class="ers-muted hint">
            借期固定 3 天；续借每次 +1 天、最多 3 次；资产逾期每日扣 5 分，续借成功即停止扣分。
          </p>
        </div>

        <div class="ers-card section">
          <h2>流转规则</h2>
          <ul class="rules">
            <li>接手由你发起、当前持有者确认；同意后换人不换归还日，逾期责任随之转移。</li>
            <li>存在待确认申请时，持有者不能续借、不能重复申请接手。</li>
            <li>信誉分低于阈值时不可借出高价值资产（本件{{ asset.highValue ? '为' : '非' }}高价值）。</li>
          </ul>
        </div>
      </div>
    </div>

    <el-dialog v-model="transferVisible" title="申请接手" width="440px">
      <el-form :model="transferForm" label-position="top">
        <el-form-item label="申请说明（可选，最多 200 字）">
          <el-input
            v-model="transferForm.applyReason"
            type="textarea"
            :rows="3"
            maxlength="200"
            show-word-limit
            placeholder="例如：下周出差需要这台笔记本，麻烦你确认一下"
          />
        </el-form-item>
      </el-form>
      <p class="dialog-tip">
        提交后由当前持有者 <strong>{{ asset.holderName }}</strong> 确认；同意后你成为持有人，<strong>应归还日不变</strong>。
      </p>
      <template #footer>
        <el-button @click="transferVisible = false">取消</el-button>
        <el-button type="primary" :loading="acting" @click="submitTransfer">提交申请</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, InfoFilled } from '@element-plus/icons-vue'
import { assetApi, transferApi } from '@/api'
import { ASSET_STATUS, dictLabel, dictType, formatDateTime, formatMoney } from '@/utils/dict'

const route = useRoute()

const ACTION_LABEL = { BORROW: '借出', RETURN: '归还', TRANSFER: '转交' }

const loading = ref(false)
const acting = ref(false)
const asset = ref({})
const records = ref([])
const pendingTransfer = ref(null)
const transferVisible = ref(false)
const transferForm = reactive({ applyReason: '' })

async function load() {
  loading.value = true
  try {
    const data = await assetApi.detail(route.params.id)
    asset.value = data.asset || {}
    records.value = data.records || []
    pendingTransfer.value = data.pendingTransfer || null
  } finally {
    loading.value = false
  }
}

async function borrow() {
  await ElMessageBox.confirm('确认借出？借期 3 天。', '借出确认', { confirmButtonText: '确认', cancelButtonText: '取消' })
  acting.value = true
  try {
    await assetApi.borrow(asset.value.id)
    ElMessage.success('借出成功')
    load()
  } finally {
    acting.value = false
  }
}

async function renew() {
  await ElMessageBox.confirm('确认续借？应归还日 +1 天，最多 3 次。', '续借确认', { confirmButtonText: '确认', cancelButtonText: '取消' })
  acting.value = true
  try {
    await assetApi.renew(asset.value.id)
    ElMessage.success('续借成功')
    load()
  } finally {
    acting.value = false
  }
}

async function returnAsset() {
  await ElMessageBox.confirm('确认归还这件资产？', '归还确认', { confirmButtonText: '确认', cancelButtonText: '取消' })
  acting.value = true
  try {
    await assetApi.returnAsset(asset.value.id)
    ElMessage.success('归还成功')
    load()
  } finally {
    acting.value = false
  }
}

async function openApply() {
  transferVisible.value = true
}

async function submitTransfer() {
  acting.value = true
  try {
    await transferApi.create({
      assetId: asset.value.id,
      applyReason: transferForm.applyReason || undefined
    })
    ElMessage.success('申请已提交，等待当前持有者确认')
    transferVisible.value = false
    transferForm.applyReason = ''
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

.head-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.detail-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.6fr) minmax(280px, 0.9fr);
  gap: 14px;
  align-items: start;
}

.left,
.right {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.section {
  padding: 18px 20px;
}

.section h2 {
  margin: 0 0 14px;
  font-size: 15px;
  font-weight: 600;
}

.kv-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 14px;
}

.kv {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.kv span {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.kv strong {
  font-size: 14px;
  font-weight: 600;
}

.danger {
  color: var(--el-color-danger);
}

.desc {
  margin: 16px 0 0;
  color: var(--el-text-color-regular);
  font-size: 13px;
  line-height: 1.7;
}

.timeline {
  padding-left: 2px;
}

.tl-title {
  font-size: 13.5px;
  font-weight: 500;
}

.actions .action {
  width: 100%;
  margin: 0 0 10px;
}

.hint {
  margin: 8px 0 0;
  line-height: 1.7;
}

.rules {
  margin: 0;
  padding-left: 18px;
  color: var(--el-text-color-regular);
  font-size: 13px;
  line-height: 1.9;
}

.dialog-tip {
  margin: 4px 0 0;
  color: var(--el-text-color-regular);
  font-size: 13px;
  line-height: 1.7;
}

.ers-alert {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  margin-bottom: 14px;
  border: 1px solid #f3d9b0;
  background: #fdf6ec;
  color: #8a5a00;
  border-radius: 10px;
  font-size: 13px;
}

@media (max-width: 960px) {
  .detail-grid {
    grid-template-columns: 1fr;
  }
}
</style>
