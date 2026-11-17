<template>
  <div class="nf">
    <div class="code">404</div>
    <p>页面不存在或已被移动</p>
    <el-button type="primary" @click="$router.replace('/home')">回到工作台</el-button>
  </div>
</template>

<style scoped>
.nf {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 70vh;
  gap: 12px;
  color: var(--el-text-color-secondary);
}

.code {
  font-size: 64px;
  font-weight: 700;
  color: var(--el-text-color-primary);
  letter-spacing: -0.03em;
}
</style>
