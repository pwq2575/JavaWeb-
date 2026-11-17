<template>
  <div class="auth-page">
    <div class="auth-card">
      <h2>注册账号</h2>
      <p class="sub">注册后即为普通用户，可借用资产、预约场地与参与投票</p>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="账号" prop="username">
          <el-input v-model="form.username" size="large" placeholder="4-32 位字母数字下划线" />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="form.realName" size="large" placeholder="真实姓名，用于借用与转交展示" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password size="large" placeholder="8-32 位，需含字母与数字" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirm">
          <el-input v-model="form.confirm" type="password" show-password size="large" placeholder="再次输入密码" />
        </el-form-item>
        <el-button type="primary" size="large" class="submit" :loading="loading" @click="onSubmit">注册并登录</el-button>
      </el-form>

      <div class="auth-foot">已有账号？<router-link to="/login">返回登录</router-link></div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()
const formRef = ref()
const loading = ref(false)

const form = reactive({ username: '', realName: '', password: '', confirm: '' })

const rules = {
  username: [
    { required: true, message: '请输入账号', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9_]{4,32}$/, message: '4-32 位字母、数字或下划线', trigger: 'blur' }
  ],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { pattern: /^(?=.*[A-Za-z])(?=.*\d)[\S]{8,32}$/, message: '8-32 位，需同时包含字母与数字', trigger: 'blur' }
  ],
  confirm: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value !== form.password) callback(new Error('两次输入的密码不一致'))
        else callback()
      },
      trigger: 'blur'
    }
  ]
}

async function onSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    const data = await auth.register({ username: form.username, realName: form.realName, password: form.password })
    ElMessage.success(`注册成功，欢迎 ${data.user.realName || data.user.username}`)
    router.replace('/home')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: var(--el-bg-color-page);
  padding: 40px 20px;
}

.auth-card {
  width: 100%;
  max-width: 420px;
  padding: 36px 32px 28px;
  background: #fff;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 16px;
  box-shadow: var(--ers-shadow-card);
}

.auth-card h2 {
  margin: 0;
  font-size: 24px;
}

.sub {
  margin: 6px 0 22px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.submit {
  width: 100%;
  margin-top: 4px;
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
</style>
