import { useState, useEffect, useCallback } from 'react'
import DashboardLayout from '../../components/layout/DashboardLayout'
import { prescriptionApi } from '../../api/prescriptionApi'
import { drugApi } from '../../api/drugApi'
import { useAuth } from '../../context/AuthContext'

const Badge = ({ children, color }) => {
  const colors = {
    yellow: 'bg-yellow-100 text-yellow-700',
    green: 'bg-green-100 text-green-700',
    red: 'bg-red-100 text-red-700',
    blue: 'bg-accent-100 text-accent-600',
  }
  return <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${colors[color]}`}>{children}</span>
}

const statusColor = (status) => {
  if (status === 'PENDING') return 'yellow'
  if (status === 'DISPENSED') return 'green'
  return 'red'
}

export default function PrescriptionsPage() {
  const { hasRole } = useAuth()
  const [prescriptions, setPrescriptions] = useState([])
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  const [showCreateModal, setShowCreateModal] = useState(false)
  const [showDetailModal, setShowDetailModal] = useState(false)
  const [showAddDrugModal, setShowAddDrugModal] = useState(false)
  const [selectedPrescription, setSelectedPrescription] = useState(null)
  const [prescriptionDrugs, setPrescriptionDrugs] = useState([])
  const [drugs, setDrugs] = useState([])

  const [form, setForm] = useState({
    patientName: '', doctorName: '', issuedDate: '', notes: ''
  })
  const [drugForm, setDrugForm] = useState({
    drugId: '', quantity: '', dosageInstructions: ''
  })

  const fetchPrescriptions = useCallback(async () => {
    setLoading(true)
    try {
      const res = await prescriptionApi.getAll({ page, size: 10 })
      setPrescriptions(res.data.content)
      setTotalPages(res.data.totalPages)
    } catch { setError('Failed to load prescriptions') }
    finally { setLoading(false) }
  }, [page])

  useEffect(() => { fetchPrescriptions() }, [fetchPrescriptions])

  useEffect(() => {
    drugApi.search({ page: 0, size: 100 })
      .then(res => setDrugs(res.data.content))
      .catch(() => { /* ignore */ })
  }, [])

  const handleSearch = useCallback(async () => {
    if (!search.trim()) { fetchPrescriptions(); return }
    try {
      const res = await prescriptionApi.search({ patientName: search, page: 0, size: 10 })
      setPrescriptions(res.data.content)
      setTotalPages(res.data.totalPages)
    } catch { setError('Search failed') }
  }, [search, fetchPrescriptions])

  useEffect(() => {
    const t = setTimeout(handleSearch, 400)
    return () => clearTimeout(t)
  }, [handleSearch])

  const openDetail = async (prescription) => {
    setSelectedPrescription(prescription)
    try {
      const res = await prescriptionApi.getDrugs(prescription.id)
      setPrescriptionDrugs(res.data)
    } catch { setPrescriptionDrugs([]) }
    setShowDetailModal(true)
  }

  const handleCreate = async (e) => {
    e.preventDefault()
    setError('')
    try {
      await prescriptionApi.create(form)
      setSuccess('Prescription created successfully')
      setShowCreateModal(false)
      setForm({ patientName: '', doctorName: '', issuedDate: '', notes: '' })
      fetchPrescriptions()
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to create prescription')
    }
  }

  const handleAddDrug = async (e) => {
    e.preventDefault()
    setError('')
    try {
      await prescriptionApi.addDrug(selectedPrescription.id, {
        ...drugForm,
        drugId: parseInt(drugForm.drugId),
        quantity: parseInt(drugForm.quantity)
      })
      setSuccess('Drug added to prescription')
      setShowAddDrugModal(false)
      setDrugForm({ drugId: '', quantity: '', dosageInstructions: '' })
      const res = await prescriptionApi.getDrugs(selectedPrescription.id)
      setPrescriptionDrugs(res.data)
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to add drug')
    }
  }

  const handleDispense = async (id) => {
    if (!confirm('Dispense this prescription? Stock will be deducted.')) return
    try {
      await prescriptionApi.dispense(id)
      setSuccess('Prescription dispensed successfully')
      setShowDetailModal(false)
      fetchPrescriptions()
    } catch (err) {
      setError(err.response?.data?.message || 'Dispense failed')
    }
  }

  const handleCancel = async (id) => {
    if (!confirm('Cancel this prescription?')) return
    try {
      await prescriptionApi.cancel(id)
      setSuccess('Prescription cancelled')
      setShowDetailModal(false)
      fetchPrescriptions()
    } catch (err) {
      setError(err.response?.data?.message || 'Cancel failed')
    }
  }
  const handleDelete = async (id) => {
  if (!confirm('Permanently delete this prescription?')) return
  try {
    await prescriptionApi.cancel(id)
    setSuccess('Prescription deleted')
    fetchPrescriptions()
  } catch (err) {
    setError(err.response?.data?.message || 'Delete failed')
  }
}

  return (
    <DashboardLayout title="Prescription Management">
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
          <h2 className="text-xl font-bold text-gray-900">Prescriptions</h2>
          <p className="text-sm text-gray-500">Manage patient prescriptions</p>
        </div>
        <button onClick={() => setShowCreateModal(true)}
          className="flex items-center gap-2 bg-gradient-to-r from-accent-600 to-accent-500 hover:from-accent-700 hover:to-accent-600 text-white px-4 py-2 rounded-lg text-sm font-medium shadow-md">
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4"/>
          </svg>
          New Prescription
        </button>
      </div>

      {/* Search */}
      <div className="mb-6">
        <input type="text" placeholder="Search by patient name..."
          value={search} onChange={e => setSearch(e.target.value)}
          className="w-full sm:w-96 px-4 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-accent-500"/>
      </div>

      {/* Table */}
      <div className="bg-white rounded-xl shadow-sm border border-primary-100 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead className="bg-primary-50/50 border-b border-primary-100">
              <tr>
                {['#', 'Patient', 'Doctor', 'Issued Date', 'Status', 'Dispensed By', 'Actions'].map(h => (
                  <th key={h} className="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-50">
              {loading ? (
                <tr><td colSpan={7} className="px-4 py-8 text-center text-gray-400">Loading...</td></tr>
              ) : prescriptions.length === 0 ? (
                <tr><td colSpan={7} className="px-4 py-8 text-center text-gray-400">No prescriptions found</td></tr>
              ) : prescriptions.map(p => (
                <tr key={p.id} className="hover:bg-gray-50 transition-colors">
                  <td className="px-4 py-3 text-gray-400 text-xs">#{p.id}</td>
                  <td className="px-4 py-3 font-medium text-gray-900">{p.patientName}</td>
                  <td className="px-4 py-3 text-gray-500">{p.doctorName}</td>
                  <td className="px-4 py-3 text-gray-500">{p.issuedDate}</td>
                  <td className="px-4 py-3">
                    <Badge color={statusColor(p.status)}>{p.status}</Badge>
                  </td>
                  <td className="px-4 py-3 text-gray-500">{p.dispensedBy || '—'}</td>
                  <td className="px-4 py-3">
                    <div className="flex items-center gap-2">
                        <button onClick={() => openDetail(p)}
                          className="text-xs px-2 py-1 bg-accent-50 text-accent-600 rounded hover:bg-accent-100">
                        View
                      </button>
                      {p.status === 'PENDING' && hasRole('ADMIN', 'PHARMACIST') && (
                        <>
                          <button onClick={() => { setSelectedPrescription(p); setShowAddDrugModal(true) }}
                            className="text-xs px-2 py-1 bg-green-50 text-green-600 rounded hover:bg-green-100">
                            Add Drug
                          </button>
                          {hasRole('PHARMACIST') && (
                            <button onClick={() => handleDispense(p.id)}
                              className="text-xs px-2 py-1 bg-teal-50 text-teal-600 rounded hover:bg-teal-100">
                              Dispense
                            </button>
                          )}
                          <button onClick={() => handleCancel(p.id)}
                            className="text-xs px-2 py-1 bg-red-50 text-red-600 rounded hover:bg-red-100">
                            Cancel
                          </button>
                          {hasRole('ADMIN') && (
                          <button onClick={() => handleDelete(p.id)}
                            className="text-xs px-2 py-1 bg-red-50 text-red-600 rounded hover:bg-red-100">
                            Delete
                          </button>
                        )}
                        </>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        {totalPages > 1 && (
          <div className="px-4 py-3 border-t border-primary-100 flex items-center justify-between">
            <p className="text-sm text-gray-500">Page {page + 1} of {totalPages}</p>
            <div className="flex gap-2">
              <button disabled={page === 0} onClick={() => setPage(p => p - 1)}
                className="px-3 py-1 text-sm border border-gray-300 rounded-lg disabled:opacity-40 hover:bg-gray-50">Previous</button>
              <button disabled={page === totalPages - 1} onClick={() => setPage(p => p + 1)}
                className="px-3 py-1 text-sm border border-gray-300 rounded-lg disabled:opacity-40 hover:bg-gray-50">Next</button>
            </div>
          </div>
        )}
      </div>

      {/* Create Modal */}
      {showCreateModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md">
            <div className="flex items-center justify-between px-6 py-4 border-b border-primary-100">
              <h3 className="font-semibold text-gray-900">New Prescription</h3>
              <button onClick={() => setShowCreateModal(false)} className="text-gray-400 hover:text-gray-600">✕</button>
            </div>
            <form onSubmit={handleCreate} className="p-6 space-y-4">
              <div>
                <label className="block text-xs font-medium text-gray-700 mb-1">Patient Name</label>
                <input required value={form.patientName}
                  onChange={e => setForm({...form, patientName: e.target.value})}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-accent-500"
                  placeholder="Selam Tesfaye"/>
              </div>
              <div>
                <label className="block text-xs font-medium text-gray-700 mb-1">Doctor Name</label>
                <input required value={form.doctorName}
                  onChange={e => setForm({...form, doctorName: e.target.value})}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-accent-500"
                  placeholder="Dr. Girma Haile"/>
              </div>
              <div>
                <label className="block text-xs font-medium text-gray-700 mb-1">Issued Date</label>
                <input required type="date" value={form.issuedDate}
                  onChange={e => setForm({...form, issuedDate: e.target.value})}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-accent-500"/>
              </div>
              <div>
                <label className="block text-xs font-medium text-gray-700 mb-1">Notes</label>
                <textarea value={form.notes} rows={2}
                  onChange={e => setForm({...form, notes: e.target.value})}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-accent-500"
                  placeholder="Take after meals..."/>
              </div>
              <div className="flex gap-3 pt-2">
                <button type="button" onClick={() => setShowCreateModal(false)}
                  className="flex-1 px-4 py-2 border border-gray-300 rounded-lg text-sm text-gray-700 hover:bg-gray-50">Cancel</button>
                <button type="submit"
                  className="flex-1 px-4 py-2 bg-gradient-to-r from-accent-600 to-accent-500 hover:from-accent-700 hover:to-accent-600 text-white rounded-lg text-sm font-medium shadow-md">Create</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Detail Modal */}
      {showDetailModal && selectedPrescription && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-lg">
            <div className="flex items-center justify-between px-6 py-4 border-b border-primary-100">
              <h3 className="font-semibold text-gray-900">Prescription #{selectedPrescription.id}</h3>
              <button onClick={() => setShowDetailModal(false)} className="text-gray-400 hover:text-gray-600">✕</button>
            </div>
            <div className="p-6 space-y-4">
              <div className="grid grid-cols-2 gap-4 text-sm">
                <div>
                  <p className="text-xs text-gray-400">Patient</p>
                  <p className="font-medium text-gray-900">{selectedPrescription.patientName}</p>
                </div>
                <div>
                  <p className="text-xs text-gray-400">Doctor</p>
                  <p className="font-medium text-gray-900">{selectedPrescription.doctorName}</p>
                </div>
                <div>
                  <p className="text-xs text-gray-400">Issued Date</p>
                  <p className="font-medium text-gray-900">{selectedPrescription.issuedDate}</p>
                </div>
                <div>
                  <p className="text-xs text-gray-400">Status</p>
                  <Badge color={statusColor(selectedPrescription.status)}>
                    {selectedPrescription.status}
                  </Badge>
                </div>
                {selectedPrescription.notes && (
                  <div className="col-span-2">
                    <p className="text-xs text-gray-400">Notes</p>
                    <p className="text-gray-700">{selectedPrescription.notes}</p>
                  </div>
                )}
              </div>

              <div>
                <h4 className="font-semibold text-gray-900 text-sm mb-3">Prescribed Drugs</h4>
                {prescriptionDrugs.length === 0 ? (
                  <p className="text-sm text-gray-400 text-center py-4 bg-gray-50 rounded-lg">No drugs added yet</p>
                ) : (
                  <div className="space-y-2">
                    {prescriptionDrugs.map(pd => (
                      <div key={pd.id} className="flex items-center justify-between bg-gray-50 px-4 py-3 rounded-lg">
                        <div>
                          <p className="text-sm font-medium text-gray-900">{pd.drugName}</p>
                          <p className="text-xs text-gray-400">{pd.dosageInstructions}</p>
                        </div>
                        <span className="text-sm font-semibold text-accent-600">×{pd.quantity}</span>
                      </div>
                    ))}
                  </div>
                )}
              </div>

              {selectedPrescription.status === 'PENDING' && (
                <div className="flex gap-3 pt-2">
                  {hasRole('PHARMACIST') && (
                    <button onClick={() => handleDispense(selectedPrescription.id)}
                  className="flex-1 px-4 py-2 bg-gradient-to-r from-emerald-600 to-emerald-500 hover:from-emerald-700 hover:to-emerald-600 text-white rounded-lg text-sm font-medium shadow-md">
                  Dispense
                </button>
              )}
              <button onClick={() => handleCancel(selectedPrescription.id)}
                className="flex-1 px-4 py-2 bg-red-50 hover:bg-red-100 text-red-600 rounded-lg text-sm font-medium">
                Cancel
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  )}


      {/* Add Drug Modal */}
      {showAddDrugModal && selectedPrescription && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-sm">
            <div className="flex items-center justify-between px-6 py-4 border-b border-primary-100">
              <h3 className="font-semibold text-gray-900">Add Drug to Prescription</h3>
              <button onClick={() => setShowAddDrugModal(false)} className="text-gray-400 hover:text-gray-600">✕</button>
            </div>
            <form onSubmit={handleAddDrug} className="p-6 space-y-4">
              <div>
                <label className="block text-xs font-medium text-gray-700 mb-1">Drug</label>
                <select required value={drugForm.drugId}
                  onChange={e => setDrugForm({...drugForm, drugId: e.target.value})}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-accent-500">
                  <option value="">Select drug</option>
                  {drugs.map(d => <option key={d.id} value={d.id}>{d.name} (Stock: {d.stockQty})</option>)}
                </select>
              </div>
              <div>
                <label className="block text-xs font-medium text-gray-700 mb-1">Quantity</label>
                <input required type="number" min="1" value={drugForm.quantity}
                  onChange={e => setDrugForm({...drugForm, quantity: e.target.value})}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-accent-500"
                  placeholder="2"/>
              </div>
              <div>
                <label className="block text-xs font-medium text-gray-700 mb-1">Dosage Instructions</label>
                <input value={drugForm.dosageInstructions}
                  onChange={e => setDrugForm({...drugForm, dosageInstructions: e.target.value})}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-accent-500"
                  placeholder="Take twice daily after meals"/>
              </div>
              <div className="flex gap-3 pt-2">
                <button type="button" onClick={() => setShowAddDrugModal(false)}
                  className="flex-1 px-4 py-2 border border-gray-300 rounded-lg text-sm text-gray-700 hover:bg-gray-50">Cancel</button>
                <button type="submit"
                  className="flex-1 px-4 py-2 bg-gradient-to-r from-accent-600 to-accent-500 hover:from-accent-700 hover:to-accent-600 text-white rounded-lg text-sm font-medium shadow-md">Add Drug</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </DashboardLayout>
  )
}