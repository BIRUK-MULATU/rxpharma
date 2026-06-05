import { useState, useEffect } from 'react'
import DashboardLayout from '../../components/layout/DashboardLayout'
import { dashboardApi } from '../../api/dashboardApi'
import { useAuth } from '../../context/AuthContext'
import { useNavigate } from 'react-router-dom'

const GradientCard = ({ title, value, subtitle, gradient, icon, onClick, animate }) => (
  <div
    onClick={onClick}
    className={`relative overflow-hidden rounded-2xl p-6 cursor-pointer transform transition-all duration-300 hover:scale-105 hover:shadow-xl ${gradient}`}
    style={{ animationDelay: animate }}
  >
    <div className="absolute top-0 right-0 w-32 h-32 rounded-full opacity-10 bg-white transform translate-x-8 -translate-y-8"/>
    <div className="absolute bottom-0 left-0 w-20 h-20 rounded-full opacity-10 bg-white transform -translate-x-6 translate-y-6"/>
    <div className="relative z-10">
      <div className="flex items-center justify-between mb-4">
        <div className="w-12 h-12 bg-white bg-opacity-20 rounded-xl flex items-center justify-center">
          {icon}
        </div>
        <div className="text-white text-opacity-70 text-xs font-medium uppercase tracking-wider">
          {subtitle}
        </div>
      </div>
      <p className="text-4xl font-bold text-white mb-1">{value ?? '—'}</p>
      <p className="text-white text-opacity-80 text-sm font-medium">{title}</p>
    </div>
  </div>
)

const AlertCard = ({ title, value, subtitle, bgColor, textColor, borderColor, icon }) => (
  <div className={`rounded-2xl p-5 border-2 ${bgColor} ${borderColor} transform transition-all duration-300 hover:scale-105`}>
    <div className="flex items-center justify-between">
      <div>
        <p className={`text-3xl font-bold ${textColor}`}>{value ?? '—'}</p>
        <p className="text-gray-700 font-semibold text-sm mt-1">{title}</p>
        <p className="text-gray-500 text-xs mt-0.5">{subtitle}</p>
      </div>
      <div className={`w-14 h-14 rounded-2xl flex items-center justify-center ${textColor} bg-white bg-opacity-60`}>
        {icon}
      </div>
    </div>
  </div>
)

export default function DashboardPage() {
  const { user } = useAuth()
  const navigate = useNavigate()
  const [stats, setStats] = useState(null)
  const [loading, setLoading] = useState(true)
  const [time, setTime] = useState(new Date())

  useEffect(() => {
    dashboardApi.getStats()
      .then(res => setStats(res.data))
      .catch(console.error)
      .finally(() => setLoading(false))
  }, [])

  useEffect(() => {
    const timer = setInterval(() => setTime(new Date()), 1000)
    return () => clearInterval(timer)
  }, [])

  const greeting = () => {
    const h = time.getHours()
    if (h < 12) return 'Good Morning'
    if (h < 17) return 'Good Afternoon'
    return 'Good Evening'
  }

  if (loading) {
    return (
      <DashboardLayout title="Dashboard">
        <div className="flex items-center justify-center h-64">
          <div className="flex flex-col items-center gap-4">
            <div className="animate-spin rounded-full h-12 w-12 border-4 border-blue-200 border-t-blue-600"/>
            <p className="text-gray-500 text-sm animate-pulse">Loading dashboard...</p>
          </div>
        </div>
      </DashboardLayout>
    )
  }

  return (
    <DashboardLayout title="Dashboard">
      {/* Welcome Banner */}
      <div className="relative overflow-hidden rounded-2xl mb-8 p-6"
        style={{ background: 'linear-gradient(135deg, #1e3a8a 0%, #2563eb 50%, #0ea5e9 100%)' }}>
        <div className="absolute top-0 right-0 w-64 h-64 rounded-full opacity-10 bg-white transform translate-x-16 -translate-y-16"/>
        <div className="absolute bottom-0 left-1/3 w-40 h-40 rounded-full opacity-5 bg-white transform translate-y-10"/>
        <div className="relative z-10 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div>
            <p className="text-blue-200 text-sm font-medium">{greeting()}, 👋</p>
            <h2 className="text-2xl font-bold text-white mt-1">{user?.email?.split('@')[0]}</h2>
            <p className="text-blue-200 text-sm mt-1 capitalize">{user?.role?.replace('_', ' ')}</p>
          </div>
          <div className="text-right">
            <p className="text-white text-3xl font-bold tabular-nums">
              {time.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' })}
            </p>
            <p className="text-blue-200 text-sm mt-1">
              {time.toLocaleDateString([], { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' })}
            </p>
          </div>
        </div>
      </div>

      {/* Main Stats */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5 mb-8">
        <GradientCard
          title="Total Users"
          value={stats?.totalUsers}
          subtitle="System Users"
          gradient="bg-gradient-to-br from-violet-600 to-purple-700"
          animate="0ms"
          onClick={() => navigate('/users')}
          icon={<svg className="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z"/></svg>}
        />
        <GradientCard
          title="Total Drugs"
          value={stats?.totalDrugs}
          subtitle="In Inventory"
          gradient="bg-gradient-to-br from-blue-500 to-cyan-600"
          animate="100ms"
          onClick={() => navigate('/drugs')}
          icon={<svg className="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19.428 15.428a2 2 0 00-1.022-.547l-2.387-.477a6 6 0 00-3.86.517l-.318.158a6 6 0 01-3.86.517L6.05 15.21a2 2 0 00-1.806.547M8 4h8l-1 1v5.172a2 2 0 00.586 1.414l5 5c1.26 1.26.367 3.414-1.415 3.414H4.828c-1.782 0-2.674-2.154-1.414-3.414l5-5A2 2 0 009 10.172V5L8 4z"/></svg>}
        />
        <GradientCard
          title="Total Sales"
          value={stats?.totalSales}
          subtitle="Transactions"
          gradient="bg-gradient-to-br from-emerald-500 to-teal-600"
          animate="200ms"
          onClick={() => navigate('/sales')}
          icon={<svg className="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 3h2l.4 2M7 13h10l4-8H5.4M7 13L5.4 5M7 13l-2.293 2.293c-.63.63-.184 1.707.707 1.707H17m0 0a2 2 0 100 4 2 2 0 000-4zm-8 2a2 2 0 11-4 0 2 2 0 014 0z"/></svg>}
        />
        <GradientCard
          title="Total Suppliers"
          value={stats?.totalSuppliers}
          subtitle="Active Partners"
          gradient="bg-gradient-to-br from-orange-500 to-red-500"
          animate="300ms"
          onClick={() => navigate('/suppliers')}
          icon={<svg className="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4"/></svg>}
        />
      </div>

      {/* Secondary Stats */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5 mb-8">
        <GradientCard
          title="Prescriptions"
          value={stats?.totalPrescriptions}
          subtitle="All Time"
          gradient="bg-gradient-to-br from-pink-500 to-rose-600"
          animate="400ms"
          onClick={() => navigate('/prescriptions')}
          icon={<svg className="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/></svg>}
        />
        <GradientCard
          title="Purchase Orders"
          value={stats?.totalPurchaseOrders}
          subtitle="All Orders"
          gradient="bg-gradient-to-br from-indigo-500 to-blue-700"
          animate="500ms"
          onClick={() => navigate('/purchase-orders')}
          icon={<svg className="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2"/></svg>}
        />
        <GradientCard
          title="Pending Orders"
          value={stats?.pendingOrders}
          subtitle="Draft Status"
          gradient="bg-gradient-to-br from-yellow-500 to-amber-600"
          animate="600ms"
          onClick={() => navigate('/purchase-orders')}
          icon={<svg className="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"/></svg>}
        />
        <GradientCard
          title="Pending Prescriptions"
          value={stats?.pendingPrescriptions}
          subtitle="Awaiting Dispense"
          gradient="bg-gradient-to-br from-teal-500 to-cyan-700"
          animate="700ms"
          onClick={() => navigate('/prescriptions')}
          icon={<svg className="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9"/></svg>}
        />
      </div>

      {/* Alert Cards */}
      <h3 className="text-lg font-bold text-gray-900 mb-4">⚠️ Alerts</h3>
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-5 mb-8">
        <AlertCard
          title="Low Stock Drugs"
          value={stats?.lowStockCount}
          subtitle="Drugs below minimum threshold"
          bgColor="bg-red-50"
          borderColor="border-red-200"
          textColor="text-red-600"
          icon={<svg className="w-7 h-7" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"/></svg>}
        />
        <AlertCard
          title="Expiring Soon"
          value={stats?.expiringSoonCount}
          subtitle="Drugs expiring within 30 days"
          bgColor="bg-yellow-50"
          borderColor="border-yellow-200"
          textColor="text-yellow-600"
          icon={<svg className="w-7 h-7" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"/></svg>}
        />
      </div>

      {/* Quick Actions */}
      <h3 className="text-lg font-bold text-gray-900 mb-4">⚡ Quick Actions</h3>
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
        {[
          { label: 'New Sale', path: '/sales', color: 'from-emerald-400 to-teal-500', icon: '🛒' },
          { label: 'New Prescription', path: '/prescriptions', color: 'from-pink-400 to-rose-500', icon: '📋' },
          { label: 'Add Drug', path: '/drugs', color: 'from-blue-400 to-cyan-500', icon: '💊' },
          { label: 'New Order', path: '/purchase-orders', color: 'from-orange-400 to-red-500', icon: '📦' },
        ].map(action => (
          <button key={action.label} onClick={() => navigate(action.path)}
            className={`bg-gradient-to-br ${action.color} rounded-2xl p-4 text-white text-center transform transition-all duration-200 hover:scale-105 hover:shadow-lg`}>
            <span className="text-2xl block mb-2">{action.icon}</span>
            <span className="text-sm font-semibold">{action.label}</span>
          </button>
        ))}
      </div>
    </DashboardLayout>
  )
}