import { useState, useEffect } from 'react'
import DashboardLayout from '../../components/layout/DashboardLayout'
import { userApi } from '../../api/userApi'
import { useAuth } from '../../context/AuthContext'

const Badge = ({ children, color }) => {
  const colors = {
    purple: 'bg-purple-100 text-purple-700',
    green: 'bg-green-100 text-green-700',
    blue: 'bg-blue-100 text-blue-700',
    orange: 'bg-orange-100 text-orange-700',
  }
  return <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${colors[color]}`}>{children}</span>
}

const roleColor = (role) => {
  if (role === 'ADMIN') return 'purple'
  if (role === 'PHARMACIST') return 'green'
  if (role === 'CASHIER') return 'blue'
  return 'orange'
}

export default function UsersPage() {
  const { user: currentUser } = useAuth()
  const [users, setUsers] = useState([])
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState('')
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [showRoleModal, setShowRoleModal] = useState(false)
  const [showPasswordModal, setShowPasswordModal] = useState(false)
  const [selectedUser, setSelectedUser] = useState(null)
  const [newRole, setNewRole] = useState('')
  const [newPassword, setNewPassword] = useState('')

  const fetchUsers = async () => {
    setLoading(true)
    try {
      const res = await userApi.getAll()
      setUsers(res.data)
    } catch { setError('Failed to load users') }
    finally { setLoading(false) }
  }

  useEffect(() => { fetchUsers() }, [])

  const handleUpdateRole = async (e) => {
    e.preventDefault()
    setError('')
    try {
      await userApi.updateRole(selectedUser.id, { role: newRole })
      setSuccess(`Role updated to ${newRole}`)
      setShowRoleModal(false)
      fetchUsers()
    } catch (err) {
      setError(err.response?.data?.message || 'Role update failed')
    }
  }

  const handleResetPassword = async (e) => {
    e.preventDefault()
    setError('')
    try {
      await userApi.resetPassword(selectedUser.id, { newPassword })
      setSuccess('Password reset successfully')
      setShowPasswordModal(false)
      setNewPassword('')
    } catch (err) {
      setError(err.response?.data?.message || 'Password reset failed')
    }
  }

  const handleDelete = async (id) => {
    if (id === currentUser?.id) {
      setError('You cannot delete your own account')
      return
    }
    if (!confirm('Delete this user?')) return
    try {
      await userApi.delete(id)
      setSuccess('User deleted successfully')
      fetchUsers()
    } catch { setError('Delete failed') }
  }

  const filtered = users.filter(u =>
    u.email.toLowerCase().includes(search.toLowerCase()) ||
    u.fullName?.toLowerCase().includes(search.toLowerCase()) ||
    u.role.toLowerCase().includes(search.toLowerCase())
  )

  const roleStats = {
    ADMIN: users.filter(u => u.role === 'ADMIN').length,
    PHARMACIST: users.filter(u => u.role === 'PHARMACIST').length,
    CASHIER: users.filter(u => u.role === 'CASHIER').length,
    SUPPLIER_MANAGER: users.filter(u => u.role === 'SUPPLIER_MANAGER').length,
  }

  return (
    <DashboardLayout title="User Management">
      {error && (
        <div className="mb-4 bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg text-sm flex justify-between">
          <span>{error}</span><button onClick={() => setError('')}>✕</button>
        </div>
      )}
      {success && (
        <div className="mb-4 bg-green-50 border border-green-200 text-green-700 px-4 py-3 rounded-lg text-sm flex justify-between">
          <span>{success}</span><button onClick={() => setSuccess('')}>✕</button>
        </div>
      )}

      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-6">
        <div>
          <h2 className="text-xl font-bold text-gray-900">Users</h2>
          <p className="text-sm text-gray-500">{users.length} total system users</p>
        </div>
      </div>

      {/* Role Stats */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
        {Object.entries(roleStats).map(([role, count]) => (
          <div key={role} className="bg-white rounded-xl p-4 shadow-sm border border-gray-100">
            <p className="text-2xl font-bold text-gray-900">{count}</p>
            <div className="mt-1">
              <Badge color={roleColor(role)}>{role.replace('_', ' ')}</Badge>
            </div>
          </div>
        ))}
      </div>

      {/* Search */}
      <div className="mb-6">
        <input type="text" placeholder="Search by email, name or role..."
          value={search} onChange={e => setSearch(e.target.value)}
          className="w-full sm:w-96 px-4 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"/>
      </div>

      {/* Table */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead className="bg-gray-50 border-b border-gray-100">
              <tr>
                {['#', 'User', 'Email', 'Role', 'Joined', 'Actions'].map(h => (
                  <th key={h} className="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-50">
              {loading ? (
                <tr><td colSpan={6} className="px-4 py-8 text-center text-gray-400">Loading...</td></tr>
              ) : filtered.length === 0 ? (
                <tr><td colSpan={6} className="px-4 py-8 text-center text-gray-400">No users found</td></tr>
              ) : filtered.map(u => (
                <tr key={u.id} className="hover:bg-gray-50 transition-colors">
                  <td className="px-4 py-3 text-gray-400 text-xs">#{u.id}</td>
                  <td className="px-4 py-3">
                    <div className="flex items-center gap-3">
                      <div className="w-8 h-8 bg-blue-100 rounded-full flex items-center justify-center flex-shrink-0">
                        <span className="text-blue-600 font-semibold text-xs">
                          {u.email?.charAt(0).toUpperCase()}
                        </span>
                      </div>
                      <div>
                        <p className="font-medium text-gray-900">{u.fullName || '—'}</p>
                        {u.id === currentUser?.id && (
                          <span className="text-xs text-blue-500">(You)</span>
                        )}
                      </div>
                    </div>
                  </td>
                  <td className="px-4 py-3 text-gray-500">{u.email}</td>
                  <td className="px-4 py-3">
                    <Badge color={roleColor(u.role)}>{u.role.replace('_', ' ')}</Badge>
                  </td>
                  <td className="px-4 py-3 text-gray-400 text-xs">
                    {u.createdAt ? new Date(u.createdAt).toLocaleDateString() : '—'}
                  </td>
                  <td className="px-4 py-3">
                    <div className="flex items-center gap-2">
                      <button onClick={() => {
                        setSelectedUser(u)
                        setNewRole(u.role)
                        setShowRoleModal(true)
                      }}
                        className="text-xs px-2 py-1 bg-purple-50 text-purple-600 rounded hover:bg-purple-100">
                        Role
                      </button>
                      <button onClick={() => {
                        setSelectedUser(u)
                        setNewPassword('')
                        setShowPasswordModal(true)
                      }}
                        className="text-xs px-2 py-1 bg-yellow-50 text-yellow-600 rounded hover:bg-yellow-100">
                        Password
                      </button>
                      {u.id !== currentUser?.id && (
                        <button onClick={() => handleDelete(u.id)}
                          className="text-xs px-2 py-1 bg-red-50 text-red-600 rounded hover:bg-red-100">
                          Delete
                        </button>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Update Role Modal */}
      {showRoleModal && selectedUser && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-sm">
            <div className="flex items-center justify-between px-6 py-4 border-b border-gray-100">
              <h3 className="font-semibold text-gray-900">Update Role</h3>
              <button onClick={() => setShowRoleModal(false)} className="text-gray-400 hover:text-gray-600">✕</button>
            </div>
            <form onSubmit={handleUpdateRole} className="p-6 space-y-4">
              <div className="bg-gray-50 rounded-lg p-3 text-sm">
                <p className="text-gray-500 text-xs">User</p>
                <p className="font-medium text-gray-900">{selectedUser.email}</p>
                <p className="text-xs text-gray-400 mt-1">
                  Current role: <Badge color={roleColor(selectedUser.role)}>{selectedUser.role}</Badge>
                </p>
              </div>
              <div>
                <label className="block text-xs font-medium text-gray-700 mb-1">New Role</label>
                <select value={newRole} onChange={e => setNewRole(e.target.value)}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500">
                  <option value="ADMIN">ADMIN</option>
                  <option value="PHARMACIST">PHARMACIST</option>
                  <option value="CASHIER">CASHIER</option>
                  <option value="SUPPLIER_MANAGER">SUPPLIER MANAGER</option>
                </select>
              </div>
              <div className="flex gap-3">
                <button type="button" onClick={() => setShowRoleModal(false)}
                  className="flex-1 px-4 py-2 border border-gray-300 rounded-lg text-sm text-gray-700 hover:bg-gray-50">Cancel</button>
                <button type="submit"
                  className="flex-1 px-4 py-2 bg-purple-600 hover:bg-purple-700 text-white rounded-lg text-sm font-medium">Update Role</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Reset Password Modal */}
      {showPasswordModal && selectedUser && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-sm">
            <div className="flex items-center justify-between px-6 py-4 border-b border-gray-100">
              <h3 className="font-semibold text-gray-900">Reset Password</h3>
              <button onClick={() => setShowPasswordModal(false)} className="text-gray-400 hover:text-gray-600">✕</button>
            </div>
            <form onSubmit={handleResetPassword} className="p-6 space-y-4">
              <div className="bg-gray-50 rounded-lg p-3 text-sm">
                <p className="text-gray-500 text-xs">Resetting password for</p>
                <p className="font-medium text-gray-900">{selectedUser.email}</p>
              </div>
              <div>
                <label className="block text-xs font-medium text-gray-700 mb-1">New Password</label>
                <input required type="password" value={newPassword}
                  onChange={e => setNewPassword(e.target.value)}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                  placeholder="Enter new password"/>
              </div>
              <div className="flex gap-3">
                <button type="button" onClick={() => setShowPasswordModal(false)}
                  className="flex-1 px-4 py-2 border border-gray-300 rounded-lg text-sm text-gray-700 hover:bg-gray-50">Cancel</button>
                <button type="submit"
                  className="flex-1 px-4 py-2 bg-yellow-500 hover:bg-yellow-600 text-white rounded-lg text-sm font-medium">Reset Password</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </DashboardLayout>
  )
}