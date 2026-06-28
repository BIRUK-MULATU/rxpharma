import { useState, useEffect, useCallback } from 'react'
import DashboardLayout from '../../components/layout/DashboardLayout'
import { prescriptionApi } from '../../api/prescriptionApi'
import { saleApi } from '../../api/saleApi'
import { useAuth } from '../../context/AuthContext'

const Badge = ({ children, color }) => {
  const colors = {
    green: 'bg-green-100 text-green-700',
    blue: 'bg-accent-100 text-accent-600',
    yellow: 'bg-yellow-100 text-yellow-700',
  }
  return <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${colors[color]}`}>{children}</span>
}

export default function PrescriptionInvoicePage() {
  const { user } = useAuth()
  const [prescriptions, setPrescriptions] = useState([])
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState('')
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [showInvoiceModal, setShowInvoiceModal] = useState(false)
  const [selectedPrescription, setSelectedPrescription] = useState(null)
  const [prescriptionDrugs, setPrescriptionDrugs] = useState([])
  const [invoice, setInvoice] = useState(null)
  const [paymentMethod, setPaymentMethod] = useState('CASH')
  const [processing, setProcessing] = useState(false)
  const [completedSale, setCompletedSale] = useState(null)

  const fetchDispensedPrescriptions = useCallback(async () => {
    setLoading(true)
    try {
      const res = await prescriptionApi.getAll({ page: 0, size: 50 })
      const dispensed = res.data.content.filter(p => p.status === 'DISPENSED')
      setPrescriptions(dispensed)
    } catch { setError('Failed to load prescriptions') }
    finally { setLoading(false) }
  }, [])

  useEffect(() => { fetchDispensedPrescriptions() }, [fetchDispensedPrescriptions])

  const openInvoiceModal = async (prescription) => {
    setSelectedPrescription(prescription)
    setCompletedSale(null)
    setInvoice(null)
    try {
      const res = await prescriptionApi.getDrugs(prescription.id)
      setPrescriptionDrugs(res.data)
    } catch { setPrescriptionDrugs([]) }
    setShowInvoiceModal(true)
  }

  const handleGenerateInvoice = async () => {
    if (prescriptionDrugs.length === 0) {
      setError('No drugs in this prescription')
      return
    }
    setProcessing(true)
    setError('')
    try {
      const saleData = {
        cashierId: user?.id || 3,
        patientName: selectedPrescription.patientName,
        paymentMethod,
        items: prescriptionDrugs.map(pd => ({
          drugId: pd.drugId,
          quantity: pd.quantity
        }))
      }
      const saleRes = await saleApi.create(saleData)
      const invoiceRes = await saleApi.getInvoice(saleRes.data.id)
      setInvoice(invoiceRes.data)
      setCompletedSale(saleRes.data)
      setSuccess('Invoice generated successfully!')
      fetchDispensedPrescriptions()
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to generate invoice')
    } finally {
      setProcessing(false)
    }
  }

  const filtered = prescriptions.filter(p =>
    p.patientName.toLowerCase().includes(search.toLowerCase()) ||
    p.doctorName.toLowerCase().includes(search.toLowerCase())
  )

  return (
    <DashboardLayout title="Prescription Invoices">
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
      <div className="mb-6">
        <h2 className="text-xl font-bold text-gray-900">Prescription Invoices</h2>
        <p className="text-sm text-gray-500 mt-1">
          Generate invoices for dispensed prescriptions
        </p>
      </div>

      {/* Info Banner */}
      <div className="bg-primary-50 border border-primary-200 rounded-xl p-4 mb-6 flex items-start gap-3">
        <svg className="w-5 h-5 text-primary-500 flex-shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
        </svg>
        <div>
          <p className="text-sm font-medium text-primary-700">Workflow</p>
          <p className="text-xs text-primary-600 mt-0.5">
            Only <strong>DISPENSED</strong> prescriptions appear here.
            Select a prescription → choose payment method → generate invoice.
            Stock is automatically deducted when the sale is processed.
          </p>
        </div>
      </div>

      {/* Search */}
      <div className="mb-6">
        <input type="text" placeholder="Search by patient or doctor name..."
          value={search} onChange={e => setSearch(e.target.value)}
          className="w-full sm:w-96 px-4 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-accent-500"/>
      </div>

      {/* Prescriptions List */}
      {loading ? (
        <div className="flex items-center justify-center h-48">
          <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-blue-600"/>
        </div>
      ) : filtered.length === 0 ? (
        <div className="text-center py-16 bg-white rounded-xl border border-primary-100">
          <svg className="w-12 h-12 text-gray-300 mx-auto mb-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
          </svg>
          <p className="text-gray-400 font-medium">No dispensed prescriptions</p>
          <p className="text-gray-300 text-sm mt-1">Dispensed prescriptions from pharmacists will appear here</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {filtered.map(p => (
            <div key={p.id}
              className="bg-white rounded-xl shadow-sm border border-primary-100 p-5 hover:shadow-md transition-all">
              <div className="flex items-start justify-between mb-3">
                <div>
                  <p className="font-semibold text-gray-900">{p.patientName}</p>
                  <p className="text-xs text-gray-400 mt-0.5">Dr. {p.doctorName}</p>
                </div>
                <Badge color="green">DISPENSED</Badge>
              </div>
              <div className="text-xs text-gray-500 mb-4 space-y-1">
                <p>Issued: {p.issuedDate}</p>
                <p>Dispensed by: {p.dispensedBy || '—'}</p>
              </div>
              <button onClick={() => openInvoiceModal(p)}
                className="w-full py-2 bg-gradient-to-r from-accent-600 to-accent-500 hover:from-accent-700 hover:to-accent-600 text-white rounded-lg text-sm font-medium shadow-md transition-all">
                Generate Invoice
              </button>
            </div>
          ))}
        </div>
      )}

      {/* Invoice Modal */}
      {showInvoiceModal && selectedPrescription && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-lg max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between px-6 py-4 border-b border-primary-100 sticky top-0 bg-white">
              <h3 className="font-semibold text-gray-900">
                {completedSale ? 'Invoice Receipt' : 'Generate Invoice'}
              </h3>
              <button onClick={() => setShowInvoiceModal(false)} className="text-gray-400 hover:text-gray-600">✕</button>
            </div>

            <div className="p-6">
              {!completedSale ? (
                <>
                  {/* Prescription Info */}
                  <div className="bg-primary-50 rounded-xl p-4 mb-4">
                    <p className="text-xs text-primary-500 font-medium uppercase mb-1">Prescription Details</p>
                    <p className="font-semibold text-gray-900">{selectedPrescription.patientName}</p>
                    <p className="text-sm text-gray-500">Dr. {selectedPrescription.doctorName}</p>
                    <p className="text-xs text-gray-400 mt-1">Issued: {selectedPrescription.issuedDate}</p>
                  </div>

                  {/* Drugs List */}
                  <div className="mb-4">
                    <p className="text-xs font-semibold text-gray-500 uppercase mb-2">Prescribed Medicines</p>
                    {prescriptionDrugs.length === 0 ? (
                      <p className="text-sm text-gray-400 text-center py-4 bg-gray-50 rounded-lg">
                        No drugs found in this prescription
                      </p>
                    ) : (
                      <div className="space-y-2">
                        {prescriptionDrugs.map(pd => (
                          <div key={pd.id} className="flex justify-between items-center bg-gray-50 px-4 py-3 rounded-lg">
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

                  {/* Payment Method */}
                  <div className="mb-5">
                    <label className="block text-xs font-medium text-gray-700 mb-1">Payment Method</label>
                    <select value={paymentMethod} onChange={e => setPaymentMethod(e.target.value)}
                      className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-accent-500">
                      <option value="CASH">Cash</option>
                      <option value="CARD">Card</option>
                      <option value="MOBILE_MONEY">Mobile Money</option>
                    </select>
                  </div>

                  {error && <p className="text-red-600 text-sm mb-3 bg-red-50 px-3 py-2 rounded-lg">{error}</p>}

                  <div className="flex gap-3">
                    <button onClick={() => setShowInvoiceModal(false)}
                      className="flex-1 px-4 py-2 border border-gray-300 rounded-lg text-sm text-gray-700 hover:bg-gray-50">
                      Cancel
                    </button>
                    <button onClick={handleGenerateInvoice} disabled={processing}
                      className="flex-1 px-4 py-2 bg-gradient-to-r from-accent-600 to-accent-500 hover:from-accent-700 hover:to-accent-600 disabled:opacity-60 text-white rounded-lg text-sm font-medium shadow-md">
                      {processing ? 'Processing...' : 'Generate Invoice'}
                    </button>
                  </div>
                </>
              ) : (
                <>
                  {/* Generated Invoice Receipt */}
                  <div className="text-center mb-6">
                    <div className="w-14 h-14 bg-green-100 rounded-full flex items-center justify-center mx-auto mb-3">
                      <svg className="w-7 h-7 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7"/>
                      </svg>
                    </div>
                    <h4 className="font-bold text-gray-900">Payment Complete!</h4>
                    <p className="text-xs text-gray-400 mt-1">Invoice generated successfully</p>
                    <p className="font-mono text-sm text-accent-600 mt-2">{invoice?.invoiceNumber}</p>
                  </div>

                  {/* Receipt Details */}
                  <div className="bg-gray-50 rounded-xl p-4 mb-4">
                    <div className="grid grid-cols-2 gap-2 text-xs mb-3">
                      <div><p className="text-gray-400">Patient</p><p className="font-medium">{invoice?.patientName}</p></div>
                      <div><p className="text-gray-400">Cashier</p><p className="font-medium">{invoice?.cashierName}</p></div>
                      <div><p className="text-gray-400">Payment</p><p className="font-medium">{invoice?.paymentMethod}</p></div>
                      <div><p className="text-gray-400">Date</p><p className="font-medium">{new Date(invoice?.saleDate).toLocaleDateString()}</p></div>
                    </div>

                    <div className="border-t border-dashed border-gray-300 pt-3 space-y-1">
                      {invoice?.items?.map(item => (
                        <div key={item.id} className="flex justify-between text-xs">
                          <span className="text-gray-600">{item.drugName} ×{item.quantity}</span>
                          <span className="font-medium">ETB {parseFloat(item.subtotal).toFixed(2)}</span>
                        </div>
                      ))}
                    </div>

                    <div className="border-t border-dashed border-gray-300 mt-3 pt-3 space-y-1">
                      <div className="flex justify-between text-xs">
                        <span className="text-gray-500">Subtotal</span>
                        <span>ETB {parseFloat(invoice?.subtotal || 0).toFixed(2)}</span>
                      </div>
                      <div className="flex justify-between text-xs">
                        <span className="text-gray-500">Tax (15%)</span>
                        <span>ETB {parseFloat(invoice?.taxAmount || 0).toFixed(2)}</span>
                      </div>
                      <div className="flex justify-between font-bold text-sm border-t border-gray-300 pt-2 mt-1">
                        <span>Total</span>
                        <span className="text-accent-600">ETB {parseFloat(invoice?.totalAmount || 0).toFixed(2)}</span>
                      </div>
                    </div>

                    <p className="text-center text-xs text-gray-400 mt-3">Thank you for your visit!</p>
                  </div>

                  <button onClick={() => setShowInvoiceModal(false)}
                    className="w-full px-4 py-2 bg-gradient-to-r from-accent-600 to-accent-500 hover:from-accent-700 hover:to-accent-600 text-white rounded-lg text-sm font-medium shadow-md">
                    Done
                  </button>
                </>
              )}
            </div>
          </div>
        </div>
      )}
    </DashboardLayout>
  )
}