import api from './axios'

export const purchaseOrderApi = {
  getAll: (params) => api.get('/api/purchase-orders', { params }),
  getById: (id) => api.get(`/api/purchase-orders/${id}`),
  create: (data) => api.post('/api/purchase-orders', data),
  updateStatus: (id, status) => api.patch(`/api/purchase-orders/${id}/status`, null, { params: { status } }),
  deliver: (id, data) => api.patch(`/api/purchase-orders/${id}/deliver`, data),
  delete: (id) => api.delete(`/api/purchase-orders/${id}`),
}