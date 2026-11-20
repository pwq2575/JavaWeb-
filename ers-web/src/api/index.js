import request, { downloadFile } from './request'

export const authApi = {
  login: (data) => request.post('/auth/login', data),
  register: (data) => request.post('/auth/register', data),
  logout: () => request.post('/auth/logout'),
  me: () => request.get('/auth/me')
}

export const userApi = {
  options: () => request.get('/users/options')
}

export const assetApi = {
  list: (params) => request.get('/assets', { params }),
  detail: (id) => request.get(`/assets/${id}`),
  borrow: (id) => request.post(`/assets/${id}/borrow`),
  renew: (id) => request.post(`/assets/${id}/renew`),
  returnAsset: (id) => request.post(`/assets/${id}/return`)
}

export const transferApi = {
  create: (data) => request.post('/transfers', data),
  list: (params) => request.get('/transfers', { params }),
  approve: (id) => request.put(`/transfers/${id}/approve`),
  reject: (id, data) => request.put(`/transfers/${id}/reject`, data)
}

export const myApi = {
  borrows: (params) => request.get('/my/borrows', { params }),
  reservations: (params) => request.get('/my/reservations', { params }),
  credit: () => request.get('/my/credit'),
  workbench: () => request.get('/my/workbench')
}

export const roomApi = {
  list: () => request.get('/rooms'),
  slots: (id, date) => request.get(`/rooms/${id}/slots`, { params: { date } }),
  book: (data) => request.post('/reservations', data),
  cancel: (id) => request.put(`/reservations/${id}/cancel`),
  use: (id) => request.put(`/reservations/${id}/use`)
}

export const procurementApi = {
  list: (params) => request.get('/procurements', { params }),
  create: (data) => request.post('/procurements', data),
  approve: (id, data) => request.put(`/procurements/${id}/approve`, data),
  reject: (id, data) => request.put(`/procurements/${id}/reject`, data),
  remove: (id) => request.delete(`/procurements/${id}`)
}

export const voteApi = {
  list: (params) => request.get('/votes', { params }),
  detail: (id) => request.get(`/votes/${id}`),
  submit: (id, data) => request.post(`/votes/${id}/vote`, data),
  create: (data) => request.post('/admin/votes', data),
  update: (id, data) => request.put(`/admin/votes/${id}`, data),
  remove: (id) => request.delete(`/admin/votes/${id}`),
  publish: (id) => request.post(`/admin/votes/${id}/publish`),
  close: (id) => request.post(`/admin/votes/${id}/close`)
}

export const adminApi = {
  dashboard: () => request.get('/admin/dashboard'),
  users: (params) => request.get('/admin/users', { params }),
  createUser: (data) => request.post('/admin/users', data),
  updateUser: (id, data) => request.put(`/admin/users/${id}`, data),
  removeUser: (id) => request.delete(`/admin/users/${id}`),
  resetPassword: (id, data) => request.put(`/admin/users/${id}/password`, data),
  rooms: () => request.get('/admin/rooms'),
  createRoom: (data) => request.post('/admin/rooms', data),
  updateRoom: (id, data) => request.put(`/admin/rooms/${id}`, data),
  removeRoom: (id) => request.delete(`/admin/rooms/${id}`),
  config: () => request.get('/admin/config'),
  updateConfig: (data) => request.put('/admin/config', data),
  assets: (params) => request.get('/assets', { params }),
  createAsset: (data) => request.post('/admin/assets', data),
  updateAsset: (id, data) => request.put(`/admin/assets/${id}`, data),
  removeAsset: (id) => request.delete(`/admin/assets/${id}`),
  disableAsset: (id) => request.post(`/admin/assets/${id}/disable`),
  enableAsset: (id) => request.post(`/admin/assets/${id}/enable`)
}

export const exportApi = {
  assets: () => downloadFile('/exports/assets', {}, '资产台账.xlsx'),
  borrows: (userId) => downloadFile('/exports/borrows', userId ? { userId } : {}, '我的借用记录.xlsx')
}

export default { authApi, userApi, assetApi, transferApi, myApi, roomApi, procurementApi, voteApi, adminApi, exportApi }
