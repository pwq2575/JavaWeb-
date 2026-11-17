/** 状态字典：后端存英文枚举，界面中文直出（IA A8 约定） */
export const ASSET_STATUS = {
  IN_STOCK: { label: '在库', type: 'success' },
  BORROWED: { label: '借出', type: 'warning' },
  DISABLED: { label: '停用', type: 'info' }
}

export const TRANSFER_STATUS = {
  PENDING: { label: '待审批', type: 'warning' },
  APPROVED: { label: '已同意', type: 'success' },
  REJECTED: { label: '已驳回', type: 'danger' },
  CANCELLED: { label: '已取消', type: 'info' }
}

export const RESERVATION_STATUS = {
  BOOKED: { label: '已预约', type: 'warning' },
  USED: { label: '已使用', type: 'success' },
  CANCELLED: { label: '已取消', type: 'info' },
  NO_SHOW: { label: '爽约', type: 'danger' }
}

export const PROCUREMENT_STATUS = {
  PENDING: { label: '待审批', type: 'warning' },
  APPROVED: { label: '已同意', type: 'success' },
  REJECTED: { label: '已驳回', type: 'danger' }
}

export const VOTE_STATUS = {
  DRAFT: { label: '草稿', type: 'info' },
  PUBLISHED: { label: '进行中', type: 'success' },
  CLOSED: { label: '已截止', type: 'info' }
}

export const ROLE = {
  ADMIN: '管理员',
  USER: '普通用户'
}

export const CREDIT_REASON = {
  OVERDUE: '资产逾期',
  NO_SHOW: '场地爽约',
  ADMIN_ADJUST: '管理员调整'
}

export function dictLabel(dict, key) {
  return dict[key]?.label || key || '-'
}

export function dictType(dict, key) {
  return dict[key]?.type || 'info'
}

export function formatDateTime(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 16)
}

export function formatMoney(value) {
  if (value === null || value === undefined || value === '') return '-'
  return `¥${Number(value).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
}

export function todayISO(offsetDays = 0) {
  const date = new Date()
  date.setDate(date.getDate() + offsetDays)
  return date.toISOString().slice(0, 10)
}
