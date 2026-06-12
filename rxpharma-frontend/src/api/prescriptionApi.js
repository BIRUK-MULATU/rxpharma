import api from './axios'

export const prescriptionApi = {
  getAll: (params) => api.get('/api/prescriptions', { params }),
  getById: (id) => api.get(`/api/prescriptions/${id}`),
  search: (params) => api.get('/api/prescriptions/search', { params }),
  create: (data) => api.post('/api/prescriptions', data),
  addDrug: (id, data) => api.post(`/api/prescriptions/${id}/drugs`, data),
  getDrugs: (id) => api.get(`/api/prescriptions/${id}/drugs`),
  dispense: (id) => api.patch(`/api/prescriptions/${id}/dispense`),
  cancel: (id) => api.patch(`/api/prescriptions/${id}/cancel`),
  getDispensed: (params) => api.get('/api/prescriptions', { params: { ...params, status: 'DISPENSED' } }),
}