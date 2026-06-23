import api from './axios'

export const userApi = {
  getAll: () => api.get('/api/users'),
  getById: (id) => api.get(`/api/users/${id}`),
  update: (id, params) => api.put(`/api/users/${id}`, null, { params }),
  updateRole: (id, data) => api.patch(`/api/users/${id}/role`, data),
  changePassword: (id, data) => api.patch(`/api/users/${id}/change-password`, data),
  resetPassword: (id, data) => api.patch(`/api/users/${id}/reset-password`, data),
  delete: (id) => api.delete(`/api/users/${id}`),
  getPending: () => api.get('/api/users/pending'),
  approve: (id) => api.patch(`/api/users/${id}/approve`),
}