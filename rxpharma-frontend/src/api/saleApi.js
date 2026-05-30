import api from './axios'

export const saleApi = {
  getAll: (params) => api.get('/api/sales', { params }),
  getById: (id) => api.get(`/api/sales/${id}`),
  search: (params) => api.get('/api/sales/search', { params }),
  create: (data) => api.post('/api/sales', data),
  getItems: (id) => api.get(`/api/sales/${id}/items`),
  getInvoice: (id) => api.get(`/api/sales/${id}/invoice`),
}