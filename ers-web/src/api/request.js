import axios from 'axios'
import { ElMessage } from 'element-plus'

/**
 * 统一请求层：
 * - 自动携带 Authorization: Bearer <token>
 * - 业务码 code !== 0 时统一提示并 reject（页面只处理成功分支）
 * - 401 清空令牌并跳登录，带 redirect 回跳
 */
const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '/api',
  timeout: 15000
})

request.interceptors.request.use((config) => {
  const token = localStorage.getItem('ers_token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

request.interceptors.response.use(
  (response) => {
    // 文件下载直接返回 blob
    if (response.config.responseType === 'blob') return response
    const body = response.data
    if (body && typeof body === 'object' && 'code' in body) {
      if (body.code === 0) return body.data
      ElMessage.error(body.message || '操作失败')
      return Promise.reject(new Error(body.message || '操作失败'))
    }
    return body
  },
  (error) => {
    const status = error.response?.status
    const body = error.response?.data
    const message = body?.message || error.message || '网络异常'

    if (status === 401) {
      localStorage.removeItem('ers_token')
      localStorage.removeItem('ers_user')
      const current = window.location.hash.replace(/^#/, '') || '/'
      if (!current.startsWith('/login')) {
        window.location.hash = `/login?redirect=${encodeURIComponent(current)}`
      }
      ElMessage.warning(message || '登录已过期，请重新登录')
    } else if (body?.data && typeof body.data === 'object') {
      // 参数校验失败：拼出首个字段错误
      const first = Object.values(body.data)[0]
      ElMessage.error(`${message}：${first}`)
    } else {
      ElMessage.error(message)
    }
    return Promise.reject(error)
  }
)

/** 触发浏览器下载（blob 响应） */
export async function downloadFile(url, params, fallbackName) {
  const response = await request.get(url, { params, responseType: 'blob' })
  const blob = response.data
  const disposition = response.headers?.['content-disposition'] || ''
  let filename = fallbackName
  const match = /filename\*?=(?:UTF-8'')?([^;]+)/i.exec(disposition)
  if (match) filename = decodeURIComponent(match[1].replace(/"/g, ''))
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = filename
  document.body.appendChild(link)
  link.click()
  URL.revokeObjectURL(link.href)
  link.remove()
}

export default request
