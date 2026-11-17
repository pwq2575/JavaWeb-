import { createRouter, createWebHashHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import AppLayout from '@/layout/AppLayout.vue'

/**
 * 路由结构对齐 IA §4：
 * - 员工区 6 项 + 管理分组（管理员可见）
 * - meta.requiresAuth / meta.roles 控制可见性与访问
 */
const routes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/LoginView.vue'),
    meta: { public: true, title: '登录' }
  },
  {
    path: '/register',
    name: 'register',
    component: () => import('@/views/RegisterView.vue'),
    meta: { public: true, title: '注册' }
  },
  {
    path: '/',
    component: AppLayout,
    meta: { requiresAuth: true },
    children: [
      { path: '', redirect: '/home' },
      { path: 'home', name: 'home', component: () => import('@/views/HomeView.vue'), meta: { title: '工作台', icon: 'HomeFilled' } },
      { path: 'assets', name: 'assets', component: () => import('@/views/assets/AssetListView.vue'), meta: { title: '资产台账', icon: 'Box' } },
      { path: 'assets/:id', name: 'asset-detail', component: () => import('@/views/assets/AssetDetailView.vue'), meta: { title: '资产详情', hidden: true } },
      { path: 'transfers', name: 'transfers', component: () => import('@/views/transfers/TransferView.vue'), meta: { title: '转交审批', icon: 'Switch' } },
      { path: 'rooms', name: 'rooms', component: () => import('@/views/rooms/RoomView.vue'), meta: { title: '场地预约', icon: 'OfficeBuilding' } },
      { path: 'procurements', name: 'procurements', component: () => import('@/views/procurement/ProcurementView.vue'), meta: { title: '采购申请', icon: 'ShoppingCart' } },
      { path: 'votes', name: 'votes', component: () => import('@/views/votes/VoteListView.vue'), meta: { title: '文化投票', icon: 'DataAnalysis' } },
      { path: 'votes/:id', name: 'vote-detail', component: () => import('@/views/votes/VoteDetailView.vue'), meta: { title: '投票详情', hidden: true } },
      { path: 'my/borrows', name: 'my-borrows', component: () => import('@/views/my/MyBorrowsView.vue'), meta: { title: '我的借用', icon: 'Tickets' } },
      { path: 'my/credit', name: 'my-credit', component: () => import('@/views/my/MyCreditView.vue'), meta: { title: '我的信誉', icon: 'Medal' } },
      { path: 'admin/dashboard', name: 'admin-dashboard', component: () => import('@/views/admin/AdminDashboardView.vue'), meta: { title: '数据看板', icon: 'TrendCharts', roles: ['ADMIN'] } },
      { path: 'admin/assets', name: 'admin-assets', component: () => import('@/views/admin/AdminAssetsView.vue'), meta: { title: '资产管理', icon: 'Management', roles: ['ADMIN'] } },
      { path: 'admin/users', name: 'admin-users', component: () => import('@/views/admin/AdminUsersView.vue'), meta: { title: '用户管理', icon: 'UserFilled', roles: ['ADMIN'] } },
      { path: 'admin/rooms', name: 'admin-rooms', component: () => import('@/views/admin/AdminRoomsView.vue'), meta: { title: '场地管理', icon: 'Coordinate', roles: ['ADMIN'] } },
      { path: 'admin/config', name: 'admin-config', component: () => import('@/views/admin/AdminConfigView.vue'), meta: { title: '规则配置', icon: 'Setting', roles: ['ADMIN'] } }
    ]
  },
  { path: '/:pathMatch(.*)*', name: 'not-found', component: () => import('@/views/NotFoundView.vue'), meta: { public: true } }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  document.title = to.meta.title ? `${to.meta.title} · ERS` : '企业资源调度与协同治理平台'

  if (to.meta.public) {
    if (auth.isLogin && (to.name === 'login' || to.name === 'register')) return { path: '/home' }
    return true
  }
  if (!auth.isLogin) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  const roles = to.meta.roles
  if (roles && !roles.includes(auth.user?.role)) {
    return { path: '/home' }
  }
  return true
})

export default router
