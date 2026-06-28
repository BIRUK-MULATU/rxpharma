import { useState, useEffect, useCallback } from 'react'
import DashboardLayout from '../../components/layout/DashboardLayout'
import { supplierApi } from '../../api/supplierApi'
import { useAuth } from '../../context/AuthContext'

const Badge = ({ children, color }) => {
  const colors = {
    green: 'bg-green-100 text-green-700',
    red: 'bg-red-100 text-red-700',
    blue: 'bg-accent-100 text-accent-600',
    orange: 'bg-orange-100 text-orange-700',
    purple: 'bg-purple-100 text-purple-700',
  }
  return (
    <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${colors[color]}`}>
      {children}
    </span>
  )
}

export default function SuppliersPage() {
  const { hasRole } = useAuth()
  const [suppliers, setSuppliers] = useState([])
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState('')
  const [filterType, setFilterType] = useState('')
  const [showModal, setShowModal] = useState(false)
  const [editSupplier, setEditSupplier] = useState(null)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  const [form, setForm] = useState({
    companyName: '',
    contactPerson: '',
    email: '',
    phone: '',
    status: 'ACTIVE',
    supplierType: 'WHOLESALER',
    address: ''
  })

  const fetchSuppliers = useCallback(async (type = '') => {
    setLoading(true)
    try {
      const res = await supplierApi.getAll(type || undefined)
      setSuppliers(res.data)
    } catch { setError('Failed to load suppliers') }
    finally { setLoading(false) }
  }, [])

  useEffect(() => { fetchSuppliers(filterType) }, [filterType, fetchSuppliers])

  const openCreate = () => {
    setEditSupplier(null)
    setForm({
      companyName: '', contactPerson: '', email: '',
      phone: '', status: 'ACTIVE',
      supplierType: 'WHOLESALER', address: ''
    })
    setError('')
    setShowModal(true)
  }

  const openEdit = (supplier) => {
    setEditSupplier(supplier)
    setForm({
      companyName: supplier.companyName,
      contactPerson: supplier.contactPerson,
      email: supplier.email,
      phone: supplier.phone,
      status: supplier.status,
      supplierType: supplier.supplierType || 'WHOLESALER',
      address: supplier.address || ''
    })
    setError('')
    setShowModal(true)
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    try {
      if (editSupplier) {
        await supplierApi.update(editSupplier.id, form)
        setSuccess('Supplier updated successfully')
      } else {
        await supplierApi.create(form)
        setSuccess('Supplier created successfully')
      }
      setShowModal(false)
      fetchSuppliers(filterType)
    } catch (err) {
      setError(err.response?.data?.message || 'Operation failed')
    }
  }

  const handleDelete = async (id) => {
    if (!confirm('Delete this supplier?')) return
    try {
      await supplierApi.delete(id)
      setSuccess('Supplier deleted')
      fetchSuppliers(filterType)
    } catch { setError('Delete failed') }
  }

  const filtered = suppliers.filter(s =>
    s.companyName.toLowerCase().includes(search.toLowerCase()) ||
    s.email.toLowerCase().includes(search.toLowerCase()) ||
    s.contactPerson.toLowerCase().includes(search.toLowerCase())
  )

  const typeColor = (type) => {
    if (type === 'IMPORTER') return 'purple'
    return 'orange'
  }

  const stats = {
    total: suppliers.length,
    active: suppliers.filter(s => s.status === 'ACTIVE').length,
    wholesalers: suppliers.filter(s => (s.supplierType || 'WHOLESALER') === 'WHOLESALER').length,
    importers: suppliers.filter(s => s.supplierType === 'IMPORTER').length,
  }

  return (
    <DashboardLayout title="Supplier Management">
      {error && (
        <div className="mb-4 bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg text-sm flex justify-between">
          <span>{error}</span>
          <button onClick={() => setError('')}>✕</button>
        </div>
      )}
      {success && (
        <div className="mb-4 bg-green-50 border border-green-200 text-green-700 px-4 py-3 rounded-lg text-sm flex justify-between">
          <span>{success}</span>
          <button onClick={() => setSuccess('')}>✕</button>
        </div>
      )}

      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-6">
        <div>
          <h2 className="text-xl font-bold text-gray-900">Suppliers</h2>
          <p className="text-sm text-gray-500">{suppliers.length} total suppliers</p>
        </div>
        {hasRole('ADMIN', 'SUPPLIER_MANAGER') && (
          <button onClick={openCreate}
            className="flex items-center gap-2 bg-gradient-to-r from-accent-600 to-accent-500 hover:from-accent-700 hover:to-accent-600 text-white px-4 py-2 rounded-lg text-sm font-medium shadow-md">
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4"/>
            </svg>
            Add Supplier
          </button>
        )}
      </div>

      {/* Stats Row */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
        {[
          { label: 'Total', value: stats.total, color: 'from-primary-600 to-primary-500' },
          { label: 'Active', value: stats.active, color: 'from-emerald-600 to-emerald-500' },
          { label: 'Wholesalers', value: stats.wholesalers, color: 'from-amber-600 to-amber-500' },
          { label: 'Importers', value: stats.importers, color: 'from-purple-600 to-purple-500' },
        ].map(stat => (
          <div key={stat.label}
            className={`bg-gradient-to-br ${stat.color} rounded-xl p-4 text-white`}>
            <p className="text-2xl font-bold">{stat.value}</p>
            <p className="text-white text-opacity-80 text-xs mt-1">{stat.label}</p>
          </div>
        ))}
      </div>

      {/* Search + Filter */}
      <div className="flex flex-col sm:flex-row gap-3 mb-6">
        <input
          type="text"
          placeholder="Search by company, email or contact..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          className="flex-1 px-4 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-accent-500"
        />
        <select
          value={filterType}
          onChange={e => setFilterType(e.target.value)}
          className="px-4 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-accent-500">
          <option value="">All Types</option>
          <option value="WHOLESALER">Wholesaler</option>
          <option value="IMPORTER">Importer</option>
        </select>
      </div>

      {/* Cards Grid */}
      {loading ? (
        <div className="flex items-center justify-center h-48">
          <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-blue-600"/>
        </div>
      ) : filtered.length === 0 ? (
        <div className="text-center py-12 text-gray-400">No suppliers found</div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {filtered.map(supplier => (
            <div key={supplier.id}
              className="bg-white rounded-xl shadow-sm border border-primary-100 p-5 hover:shadow-md transition-shadow">

              {/* Card Header */}
              <div className="flex items-start justify-between mb-3">
                <div className="flex items-center gap-3">
                  <div className="w-11 h-11 bg-gradient-to-br from-primary-600 to-primary-500 rounded-xl flex items-center justify-center flex-shrink-0">
                    <span className="text-white font-bold text-sm">
                      {supplier.companyName.charAt(0)}
                    </span>
                  </div>
                  <div>
                    <h3 className="font-semibold text-gray-900 text-sm">{supplier.companyName}</h3>
                    <p className="text-xs text-gray-400">{supplier.contactPerson}</p>
                  </div>
                </div>
                <Badge color={supplier.status === 'ACTIVE' ? 'green' : 'red'}>
                  {supplier.status}
                </Badge>
              </div>

              {/* Supplier Type Badge */}
              <div className="mb-3">
                <Badge color={typeColor(supplier.supplierType)}>
                  {supplier.supplierType || 'WHOLESALER'}
                </Badge>
              </div>

              {/* Info */}
              <div className="space-y-1.5 mb-4">
                <div className="flex items-center gap-2 text-xs text-gray-500">
                  <svg className="w-3.5 h-3.5 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z"/>
                  </svg>
                  <span className="truncate">{supplier.email}</span>
                </div>
                <div className="flex items-center gap-2 text-xs text-gray-500">
                  <svg className="w-3.5 h-3.5 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 5a2 2 0 012-2h3.28a1 1 0 01.948.684l1.498 4.493a1 1 0 01-.502 1.21l-2.257 1.13a11.042 11.042 0 005.516 5.516l1.13-2.257a1 1 0 011.21-.502l4.493 1.498a1 1 0 01.684.949V19a2 2 0 01-2 2h-1C9.716 21 3 14.284 3 6V5z"/>
                  </svg>
                  {supplier.phone}
                </div>
                {supplier.address && (
                  <div className="flex items-center gap-2 text-xs text-gray-500">
                    <svg className="w-3.5 h-3.5 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z"/>
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 11a3 3 0 11-6 0 3 3 0 016 0z"/>
                    </svg>
                    <span className="truncate">{supplier.address}</span>
                  </div>
                )}
              </div>

              {/* Actions */}
              {hasRole('ADMIN', 'SUPPLIER_MANAGER') && (
                <div className="flex gap-2 pt-3 border-t border-gray-50">
                  <button onClick={() => openEdit(supplier)}
                    className="flex-1 text-xs px-3 py-1.5 bg-gray-50 text-gray-600 rounded-lg hover:bg-gray-100 font-medium">
                    Edit
                  </button>
                  {hasRole('ADMIN') && (
                    <button onClick={() => handleDelete(supplier.id)}
                      className="flex-1 text-xs px-3 py-1.5 bg-red-50 text-red-600 rounded-lg hover:bg-red-100 font-medium">
                      Delete
                    </button>
                  )}
                </div>
              )}
            </div>
          ))}
        </div>
      )}

      {/* Modal */}
      {showModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between px-6 py-4 border-b border-primary-100 sticky top-0 bg-white">
              <h3 className="font-semibold text-gray-900">
                {editSupplier ? 'Edit Supplier' : 'Add New Supplier'}
              </h3>
              <button onClick={() => setShowModal(false)} className="text-gray-400 hover:text-gray-600">✕</button>
            </div>

            <form onSubmit={handleSubmit} className="p-6 space-y-4">
              {error && (
                <p className="text-red-600 text-sm bg-red-50 px-3 py-2 rounded-lg">{error}</p>
              )}

              <div>
                <label className="block text-xs font-medium text-gray-700 mb-1">Company Name</label>
                <input required value={form.companyName}
                  onChange={e => setForm({...form, companyName: e.target.value})}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-accent-500"
                  placeholder="MediSupply Ethiopia"/>
              </div>

              <div>
                <label className="block text-xs font-medium text-gray-700 mb-1">Contact Person</label>
                <input required value={form.contactPerson}
                  onChange={e => setForm({...form, contactPerson: e.target.value})}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-accent-500"
                  placeholder="Abebe Kebede"/>
              </div>

              <div>
                <label className="block text-xs font-medium text-gray-700 mb-1">Email</label>
                <input required type="email" value={form.email}
                  onChange={e => setForm({...form, email: e.target.value})}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-accent-500"
                  placeholder="contact@supplier.com"/>
              </div>

              <div>
                <label className="block text-xs font-medium text-gray-700 mb-1">Phone</label>
                <input required value={form.phone}
                  onChange={e => setForm({...form, phone: e.target.value})}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-accent-500"
                  placeholder="+251911234567"/>
              </div>

              <div>
                <label className="block text-xs font-medium text-gray-700 mb-1">Supplier Type</label>
                <select value={form.supplierType}
                  onChange={e => setForm({...form, supplierType: e.target.value})}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-accent-500">
                  <option value="WHOLESALER">Wholesaler</option>
                  <option value="IMPORTER">Importer</option>
                </select>
              </div>

              <div>
                <label className="block text-xs font-medium text-gray-700 mb-1">Location / Address</label>
                <input value={form.address}
                  onChange={e => setForm({...form, address: e.target.value})}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-accent-500"
                  placeholder="Addis Ababa, Bole Sub City"/>
              </div>

              {editSupplier && (
                <div>
                  <label className="block text-xs font-medium text-gray-700 mb-1">Status</label>
                  <select value={form.status}
                    onChange={e => setForm({...form, status: e.target.value})}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-accent-500">
                    <option value="ACTIVE">ACTIVE</option>
                    <option value="ON_HOLD">ON_HOLD</option>
                  </select>
                </div>
              )}

              <div className="flex gap-3 pt-2">
                <button type="button" onClick={() => setShowModal(false)}
                  className="flex-1 px-4 py-2 border border-gray-300 rounded-lg text-sm text-gray-700 hover:bg-gray-50">
                  Cancel
                </button>
                <button type="submit"
                  className="flex-1 px-4 py-2 bg-gradient-to-r from-accent-600 to-accent-500 hover:from-accent-700 hover:to-accent-600 text-white rounded-lg text-sm font-medium shadow-md">
                  {editSupplier ? 'Update' : 'Create'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </DashboardLayout>
  )
}