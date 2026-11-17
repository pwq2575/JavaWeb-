<template>
  <div class="layout">
    <aside class="sidebar">
      <div class="brand">
        <span class="brand-mark">ERS</span>
        <span class="brand-text">资源调度平台</span>
      </div>

      <el-menu
        :default-active="activePath"
        router
        class="side-menu"
        background-color="transparent"
        text-color="#c8d0de"
        active-text-color="#ffffff"
      >
        <template v-for="item in employeeMenus" :key="item.path">
          <el-menu-item :index="item.path">
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.title }}</span>
          </el-menu-item>
        </template>

        <el-sub-menu v-if="auth.isAdmin" index="admin">
          <template #title>
            <el-icon><Setting /></el-icon>
            <span>管理</span>
          </template>
          <el-menu-item v-for="item in adminMenus" :key="item.path" :index="item.path">
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.title }}</span>
          </el-menu-item>
        </el-sub-menu>
      </el-menu>

      <div class="sidebar-foot">
        <div class="credit-chip" :class="{ warn: auth.isAdmin }">
          <span class="dot"></span>
          <span v-if="auth.isAdmin">管理员</span>
          <span v-else>信誉 {{ auth.creditScore }} 分</span>
        </div>
      </div>
    </aside>

    <div class="main">
      <header class="topbar">
        <div class="crumb">
          <el-icon><Location /></el-icon>
          <span>{{ route.meta.title || '' }}</span>
        </div>
        <div class="topbar-right">
          <el-tag v-if="!auth.isAdmin && auth.creditScore < 60" type="warning" effect="light" size="small">
            信誉偏低 · 高价值资产受限
          </el-tag>
          <el-dropdown trigger="click" @command="onCommand">
            <span class="user-chip">
              <el-avatar :size="28" class="avatar">{{ avatarText }}</el-avatar>
              <span class="user-name">{{ auth.displayName }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="credit">我的信誉</el-dropdown-item>
                <el-dropdown-item command="borrows">我的借用</el-dropdown-item>
                <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <main class="content">
        <router-view v-slot="{ Component }">
          <transition name="fade-slide" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const activePath = computed(() => route.path)

const employeeMenus = [
  { path: '/home', title: '工作台', icon: 'HomeFilled' },
  { path: '/assets', title: '资产台账', icon: 'Box' },
  { path: '/transfers', title: '转交审批', icon: 'Switch' },
  { path: '/rooms', title: '场地预约', icon: 'OfficeBuilding' },
  { path: '/procurements', title: '采购申请', icon: 'ShoppingCart' },
  { path: '/votes', title: '文化投票', icon: 'DataAnalysis' }
]

const adminMenus = [
  { path: '/admin/dashboard', title: '数据看板', icon: 'TrendCharts' },
  { path: '/admin/assets', title: '资产管理', icon: 'Management' },
  { path: '/admin/users', title: '用户管理', icon: 'UserFilled' },
  { path: '/admin/rooms', title: '场地管理', icon: 'Coordinate' },
  { path: '/admin/config', title: '规则配置', icon: 'Setting' }
]

const avatarText = computed(() => (auth.displayName || 'U').slice(0, 1))

onMounted(() => {
  // 进入应用后刷新一次用户信息（信誉分与角色以服务端为准）
  auth.refreshMe().catch(() => {})
})

async function onCommand(command) {
  if (command === 'logout') {
    await auth.logout()
    router.replace('/login')
    return
  }
  const map = { credit: '/my/credit', borrows: '/my/borrows' }
  if (map[command]) router.push(map[command])
}
</script>

<style scoped>
.layout {
  display: flex;
  min-height: 100vh;
}

.sidebar {
  display: flex;
  flex-direction: column;
  width: var(--ers-sidebar-w);
  flex: 0 0 var(--ers-sidebar-w);
  background: var(--ers-sidebar-bg);
  color: var(--ers-sidebar-text);
  position: sticky;
  top: 0;
  height: 100vh;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  height: var(--ers-header-h);
  padding: 0 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}

.brand-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border-radius: 9px;
  background: linear-gradient(135deg, #2b5ce6, #5c82ee);
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.04em;
}

.brand-text {
  font-size: 14px;
  font-weight: 600;
  color: #eef2f9;
}

.side-menu {
  border-right: none;
  padding: 10px 8px;
  flex: 1;
  overflow-y: auto;
}

.side-menu :deep(.el-menu-item),
.side-menu :deep(.el-sub-menu__title) {
  height: 42px;
  line-height: 42px;
  border-radius: 8px;
  margin-bottom: 2px;
  font-size: 13.5px;
}

.side-menu :deep(.el-menu-item.is-active) {
  background: var(--ers-sidebar-active) !important;
  color: #fff;
}

.side-menu :deep(.el-menu-item:hover),
.side-menu :deep(.el-sub-menu__title:hover) {
  background: rgba(255, 255, 255, 0.06);
}

.sidebar-foot {
  padding: 12px 16px 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
}

.credit-chip {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(10, 122, 87, 0.16);
  color: #64d3ae;
  font-size: 12px;
}

.credit-chip.warn {
  background: rgba(43, 92, 230, 0.18);
  color: #9db9ff;
}

.credit-chip .dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
}

.main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.topbar {
  position: sticky;
  top: 0;
  z-index: 10;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: var(--ers-header-h);
  padding: 0 24px;
  background: rgba(255, 255, 255, 0.86);
  backdrop-filter: blur(8px);
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.crumb {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--el-text-color-regular);
  font-size: 13px;
}

.topbar-right {
  display: flex;
  align-items: center;
  gap: 14px;
}

.user-chip {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  outline: none;
}

.avatar {
  background: #2b5ce6;
  color: #fff;
  font-size: 13px;
}

.user-name {
  font-size: 13px;
  color: var(--el-text-color-primary);
}

.content {
  flex: 1;
  min-width: 0;
}

.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: opacity var(--ers-dur-standard) var(--ers-ease), transform var(--ers-dur-standard) var(--ers-ease);
}

.fade-slide-enter-from {
  opacity: 0;
  transform: translateY(6px);
}

.fade-slide-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}
</style>
