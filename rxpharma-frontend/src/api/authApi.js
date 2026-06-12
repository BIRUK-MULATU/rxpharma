import api from './axios'

export const authApi = {
  login: (data) => api.post('/api/auth/login', data),
  register: (data) => api.post('/api/auth/register', data),
  logout: () => api.post('/api/auth/logout'),
  forgotPassword: (data) => api.post('/api/auth/forgot-password', data),
  verifyResetToken: (data) => api.post('/api/auth/verify-reset-token', data),
  resetPassword: (data) => api.post('/api/auth/reset-password', data),
  googleLogin: (credential) => api.post('/api/auth/google', { credential }),
}