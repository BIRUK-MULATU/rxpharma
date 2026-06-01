import { Routes, Route, Navigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import ProtectedRoute from './ProtectedRoute'
import LoginPage from '../pages/auth/LoginPage'
import ForgotPasswordPage from '../pages/auth/ForgotPasswordPage'
import DashboardPage from '../pages/dashboard/DashboardPage'
import DrugsPage from '../pages/drugs/DrugsPage'
import SuppliersPage from '../pages/suppliers/SuppliersPage'

export default function AppRouter() {
  const { user } = useAuth()

  return (
    <Routes>
      <Route path="/login" element={!user ? <LoginPage /> : <Navigate to="/dashboard" />} />
      <Route path="/forgot-password" element={<ForgotPasswordPage />} />
      <Route path="/" element={<Navigate to={user ? '/dashboard' : '/login'} />} />
      <Route path="/dashboard" element={
        <ProtectedRoute><DashboardPage /></ProtectedRoute>
      } />
      <Route path="/drugs" element={
        <ProtectedRoute roles={['ADMIN', 'PHARMACIST', 'CASHIER']}>
          <DrugsPage />
        </ProtectedRoute>
      } />
      <Route path="/suppliers" element={
        <ProtectedRoute roles={['ADMIN', 'SUPPLIER_MANAGER']}>
          <SuppliersPage />
        </ProtectedRoute>
      } />
      <Route path="*" element={<Navigate to="/" />} />
    </Routes>
  )
}