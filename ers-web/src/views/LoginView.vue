<template>
  <div class="auth-page">
    <section class="auth-aside">
      <div class="aside-inner">
        <div class="logo">ERS</div>
        <h1>企业资源调度<br />与协同治理平台</h1>
        <p class="lead">
          资产借还、场地预约、采购审批与文化投票，<br />
          在同一套台账里留下可追溯的每一次流转。
        </p>
        <ul class="points">
          <li><el-icon><Box /></el-icon> 资产流转全程留痕，信用分约束高价值借用</li>
          <li><el-icon><OfficeBuilding /></el-icon> 30 分钟切片预约，自动识别爽约</li>
          <li><el-icon><TrendCharts /></el-icon> 管理端看板与台账导出，数据一屏掌控</li>
        </ul>
      </div>
    </section>

    <section class="auth-main">
      <div class="auth-card">
        <h2>登录</h2>
        <p class="sub">使用公司账号进入平台</p>

        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="onSubmit">
          <el-form-item label="账号" prop="username">
            <el-input v-model="form.username" placeholder="请输入账号" size="large" autocomplete="username" />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input
              v-model="form.password"
              type="password"
              show-password
              placeholder="请输入密码"
              size="large"
              autocomplete="current-password"
              @keyup.enter="onSubmit"
            />
          </el-form-item>
          <el-button type="primary" size="large" class="submit" :loading="loading" @click="onSubmit">
            登录
          </el-button>
        </el-form>

        <div class="auth-foot">
          还没有账号？<router-link to="/register">立即注册</router-link>
        </div>

        <div class="demo">
          <div class="demo-title">演示账号（密码均为 123456）</div>
          <div class="demo-list">
            <el-tag
              v-for="item in demos"
              :key="item.username"
              size="small"
              effect="plain"
              class="demo-tag"
              @click="fill(item.username)"
            >
              {{ item.label }}
            </el-tag>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const formRef = ref()
const loading = ref(false)
const form = reactive({ username: '', password: '' })

const rules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const demos = [
  { username: 'admin', label: 'admin · 管理员' },
  { username: 'zhangsan', label: 'zhangsan · 张三' },
  { username: 'lisi', label: 'lisi · 李四' },
  { username: 'wangwu', label: 'wangwu · 低信誉' }
]

function fill(username) {
  form.username = username
  form.password = '123456'
}

async function onSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    const data = await auth.login(form)
    ElMessage.success(`欢迎回来，${data.user.realName || data.user.username}`)
    const redirect = route.query.redirect
    router.replace(typeof redirect === 'string' && redirect.startsWith('/') ? redirect : '/home')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-page {
  display: grid;
  grid-template-columns: 1.05fr 1fr;
  min-height: 100vh;
}

.auth-aside {
  background: linear-gradient(160deg, #101a2e 0%, #16233d 55%, #1c2f56 100%);
  color: #eef2f9;
  display: flex;
  align-items: center;
  padding: 64px;
}

.aside-inner {
  max-width: 460px;
}

.logo {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: linear-gradient(135deg, #2b5ce6, #5c82ee);
  font-weight: 700;
  letter-spacing: 0.05em;
  margin-bottom: 28px;
}

.auth-aside h1 {
  font-size: 34px;
  line-height: 1.28;
  margin: 0 0 18px;
  letter-spacing: -0.02em;
}

.lead {
  color: #b9c4d8;
  font-size: 14px;
  line-height: 1.8;
  margin: 0 0 32px;
}

.points {
  list-style: none;
  padding: 0;
  margin: 0;
  display: grid;
  gap: 14px;
}

.points li {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13.5px;
  color: #cdd6e6;
}

.points .el-icon {
  color: #6f9bff;
}

.auth-main {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
  background: var(--el-bg-color-page);
}

.auth-card {
  width: 100%;
  max-width: 400px;
  padding: 36px 32px 28px;
  background: #fff;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 16px;
  box-shadow: var(--ers-shadow-card);
}

.auth-card h2 {
  margin: 0;
  font-size: 24px;
  letter-spacing: -0.01em;
}

.sub {
  margin: 6px 0 24px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.submit {
  width: 100%;
  margin-top: 6px;
}

.auth-foot {
  margin-top: 16px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.auth-foot a {
  color: var(--el-color-primary);
  text-decoration: none;
  font-weight: 500;
}

.demo {
  margin-top: 22px;
  padding-top: 18px;
  border-top: 1px dashed var(--el-border-color-light);
}

.demo-title {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-bottom: 10px;
}

.demo-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.demo-tag {
  cursor: pointer;
}

@media (max-width: 900px) {
  .auth-page {
    grid-template-columns: 1fr;
  }
  .auth-aside {
    display: none;
  }
}
</style>
