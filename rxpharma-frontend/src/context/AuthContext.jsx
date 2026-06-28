import { createContext, useContext, useState } from 'react'
import { authApi } from '../api/authApi'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    try { const u = localStorage.getItem('user'); return u ? JSON.parse(u) : null }
    catch { return null }
  })
  const [token, setToken] = useState(() => localStorage.getItem('token'))
  const [loading] = useState(false)

  const login = async (email, password) => {
    const response = await authApi.login({ email, password })
    const { token, id, email: userEmail, fullName, role } = response.data
    const userData = { id, email: userEmail, fullName, role }
    localStorage.setItem('token', token)
    localStorage.setItem('user', JSON.stringify(userData))
    setToken(token)
    setUser(userData)
    return userData
  }

  const loginWithGoogle = async (credential) => {
    const response = await authApi.googleLogin(credential)
    const { token, id, email, fullName, role } = response.data
    const userData = { id, email, fullName, role }
    localStorage.setItem('token', token)
    localStorage.setItem('user', JSON.stringify(userData))
    setToken(token)
    setUser(userData)
    return userData
  }

  const logout = async () => {
    try { await authApi.logout() } catch { /* ignore */ }
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    setToken(null)
    setUser(null)
  }

  const isAdmin = () => user?.role === 'ADMIN'
  const isPharmacist = () => user?.role === 'PHARMACIST'
  const isCashier = () => user?.role === 'CASHIER'
  const isSupplierManager = () => user?.role === 'SUPPLIER_MANAGER'
  const hasRole = (...roles) => roles.includes(user?.role)

  return (
    <AuthContext.Provider value={{
      user, token, loading,
      login, loginWithGoogle, logout,
      isAdmin, isPharmacist, isCashier, isSupplierManager, hasRole
    }}>
      {children}
    </AuthContext.Provider>
  )
}

// eslint-disable-next-line react-refresh/only-export-components
export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) throw new Error('useAuth must be used within AuthProvider')
  return context
}