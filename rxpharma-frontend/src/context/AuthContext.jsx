import { createContext, useContext, useState, useEffect } from 'react'
import { authApi } from '../api/authApi'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [token, setToken] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const savedToken = localStorage.getItem('token')
    const savedUser = localStorage.getItem('user')
    if (savedToken && savedUser) {
      setToken(savedToken)
      setUser(JSON.parse(savedUser))
    }
    setLoading(false)
  }, [])

  const login = async (email, password) => {
    const response = await authApi.login({ email, password })
    const { token, email: userEmail, role } = response.data
    const userData = { email: userEmail, role }
    localStorage.setItem('token', token)
    localStorage.setItem('user', JSON.stringify(userData))
    setToken(token)
    setUser(userData)
    return userData
  }

  const logout = async () => {
    try { await authApi.logout() } catch {}
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
      login, logout,
      isAdmin, isPharmacist, isCashier, isSupplierManager, hasRole
    }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) throw new Error('useAuth must be used within AuthProvider')
  return context
}