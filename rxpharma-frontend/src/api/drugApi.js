import api from './axios'

export const drugApi = {
  search: (params) => api.get('/api/drugs', { params }),
  getById: (id) => api.get(`/api/drugs/${id}`),
  create: (data) => api.post('/api/drugs', data),
  update: (id, data) => api.put(`/api/drugs/${id}`, data),
  adjustStock: (id, data) => api.patch(`/api/drugs/${id}/stock`, data),
  delete: (id) => api.delete(`/api/drugs/${id}`),
  getLowStock: (params) => api.get('/api/drugs/alerts/low-stock', { params }),
  getExpiringSoon: (params) => api.get('/api/drugs/alerts/expiring-soon', { params }),
  getExpired: () => api.get('/api/drugs/alerts/expired'),
}