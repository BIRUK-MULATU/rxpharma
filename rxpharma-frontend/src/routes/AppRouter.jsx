import { Routes, Route, Navigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import ProtectedRoute from './ProtectedRoute'
import LoginPage from '../pages/auth/LoginPage'
import ForgotPasswordPage from '../pages/auth/ForgotPasswordPage'
import DashboardPage from '../pages/dashboard/DashboardPage'
import DrugsPage from '../pages/drugs/DrugsPage'
import SuppliersPage from '../pages/suppliers/SuppliersPage'
import PrescriptionsPage from '../pages/prescriptions/PrescriptionsPage'
import SalesPage from '../pages/sales/SalesPage'
import PurchaseOrdersPage from '../pages/purchaseOrders/PurchaseOrdersPage'
import UsersPage from '../pages/users/UsersPage'

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
      <Route path="/prescriptions" element={
        <ProtectedRoute roles={['ADMIN', 'PHARMACIST']}>
          <PrescriptionsPage />
        </ProtectedRoute>
      } />
      <Route path="/sales" element={
        <ProtectedRoute roles={['ADMIN', 'CASHIER']}>
          <SalesPage />
        </ProtectedRoute>
      } />
      <Route path="/purchase-orders" element={
        <ProtectedRoute roles={['ADMIN', 'SUPPLIER_MANAGER']}>
          <PurchaseOrdersPage />
        </ProtectedRoute>
      } />
      <Route path="/users" element={
        <ProtectedRoute roles={['ADMIN']}>
          <UsersPage />
        </ProtectedRoute>
      } />
      <Route path="*" element={<Navigate to="/" />} />
    </Routes>
  )
}