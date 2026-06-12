import { useState, useEffect } from 'react'
import DashboardLayout from '../../components/layout/DashboardLayout'
import { saleApi } from '../../api/saleApi'
import { drugApi } from '../../api/drugApi'
import { useAuth } from '../../context/AuthContext'

const Badge = ({ children, color }) => {
  const colors = {
    green: 'bg-green-100 text-green-700',
    blue: 'bg-blue-100 text-blue-700',
    purple: 'bg-purple-100 text-purple-700',
    teal: 'bg-teal-100 text-teal-700',
    orange: 'bg-orange-100 text-orange-700',
  }
  return <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${colors[color]}`}>{children}</span>
}

const paymentColor = (method) => {
  if (method === 'CASH') return 'green'
  if (method === 'CARD') return 'blue'
  return 'purple'
}

export default function SalesPage() {
  const { user } = useAuth()
  const [sales, setSales] = useState([])
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [showNewSale, setShowNewSale] = useState(false)
  const [showInvoice, setShowInvoice] = useState(false)
  const [selectedSale, setSelectedSale] = useState(null)
  const [invoice, setInvoice] = useState(null)
  const [drugs, setDrugs] = useState([])

  // Fix 3 & 4: track which sale IDs have already had their invoice generated
  const [generatedInvoices, setGeneratedInvoices] = useState({})

  const [saleItems, setSaleItems] = useState([{ drugId: '', quantity: 1 }])
  const [saleForm, setSaleForm] = useState({
    patientName: '', paymentMethod: 'CASH'
  })

  const fetchSales = async () => {
    setLoading(true)
    try {
      const res = await saleApi.getAll({ page, size: 10 })
      setSales(res.data.content)
      setTotalPages(res.data.totalPages)
    } catch { setError('Failed to load sales') }
    finally { setLoading(false) }
  }

  useEffect(() => { fetchSales() }, [page])

  useEffect(() => {
    drugApi.search({ page: 0, size: 100 })
      .then(res => setDrugs(res.data.content))
      .catch(() => {})
  }, [])

  useEffect(() => {
    const t = setTimeout(async () => {
      if (!search.trim()) { fetchSales(); return }
      try {
        const res = await saleApi.search({ patientName: search, page: 0, size: 10 })
        setSales(res.data.content)
        setTotalPages(res.data.totalPages)
      } catch {}
    }, 400)
    return () => clearTimeout(t)
  }, [search])

  const viewInvoice = async (sale) => {
    try {
      const res = await saleApi.getInvoice(sale.id)
      setInvoice(res.data)
      setSelectedSale(sale)
      // Fix 3 & 4: mark this sale's invoice as generated
      setGeneratedInvoices(prev => ({ ...prev, [sale.id]: true }))
      setShowInvoice(true)
    } catch { setError('Failed to load invoice') }
  }

  const addItem = () => setSaleItems([...saleItems, { drugId: '', quantity: 1 }])
  const removeItem = (i) => setSaleItems(saleItems.filter((_, idx) => idx !== i))
  const updateItem = (i, field, value) => {
    const updated = [...saleItems]
    updated[i][field] = value
    setSaleItems(updated)
  }

  const getDrugPrice = (drugId) => {
    const drug = drugs.find(d => d.id === parseInt(drugId))
    return drug ? parseFloat(drug.price) : 0
  }

  const calcSubtotal = () => saleItems.reduce((sum, item) => {
    return sum + (getDrugPrice(item.drugId) * (parseInt(item.quantity) || 0))
  }, 0)
  const calcTax = () => calcSubtotal() * 0.15
  const calcTotal = () => calcSubtotal() + calcTax()

  const handleCreateSale = async (e) => {
    e.preventDefault()
    setError('')
    const validItems = saleItems.filter(i => i.drugId && i.quantity > 0)
    if (validItems.length === 0) {
      setError('Add at least one drug item')
      return
    }
    try {
      const res = await saleApi.create({
        cashierId: user?.id,
        patientName: saleForm.patientName,
        paymentMethod: saleForm.paymentMethod,
        items: validItems.map(i => ({
          drugId: parseInt(i.drugId),
          quantity: parseInt(i.quantity)
        }))
      })
      setSuccess('Sale created successfully')
      setShowNewSale(false)
      setSaleItems([{ drugId: '', quantity: 1 }])
      setSaleForm({ patientName: '', paymentMethod: 'CASH' })
      fetchSales()
      // Automatically open the invoice after creating the sale
      const invoiceRes = await saleApi.getInvoice(res.data.id)
      setInvoice(invoiceRes.data)
      setSelectedSale(res.data)
      setGeneratedInvoices(prev => ({ ...prev, [res.data.id]: true }))
      setShowInvoice(true)
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to create sale')
    }
  }

  return (
    <DashboardLayout title="Sales Management">
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
          <h2 className="text-xl font-bold text-gray-900">Sales</h2>
          <p className="text-sm text-gray-500">Point of Sale & Invoice Management</p>
        </div>
        <button onClick={() => setShowNewSale(true)}
          className="flex items-center gap-2 bg-teal-600 hover:bg-teal-700 text-white px-4 py-2 rounded-lg text-sm font-medium">
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4"/>
          </svg>
          New Sale
        </button>
      </div>

      {/* Search */}
      <div className="mb-6">
        <input type="text" placeholder="Search by patient name..."
          value={search} onChange={e => setSearch(e.target.value)}
          className="w-full sm:w-96 px-4 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-teal-500"/>
      </div>

      {/* Table */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead className="bg-gray-50 border-b border-gray-100">
              <tr>
                {['Invoice', 'Patient', 'Cashier', 'Payment', 'Subtotal', 'Tax', 'Total', 'Date', 'Actions'].map(h => (
                  <th key={h} className="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-50">
              {loading ? (
                <tr><td colSpan={9} className="px-4 py-8 text-center text-gray-400">Loading...</td></tr>
              ) : sales.length === 0 ? (
                <tr><td colSpan={9} className="px-4 py-8 text-center text-gray-400">No sales found</td></tr>
              ) : sales.map(sale => (
                <tr key={sale.id} className="hover:bg-gray-50 transition-colors">
                  <td className="px-4 py-3 font-mono text-xs text-teal-700 font-medium">{sale.invoiceNumber}</td>
                  <td className="px-4 py-3 font-medium text-gray-900">{sale.patientName}</td>
                  <td className="px-4 py-3 text-gray-500">{sale.cashierName || '—'}</td>
                  <td className="px-4 py-3">
                    <Badge color={paymentColor(sale.paymentMethod)}>{sale.paymentMethod}</Badge>
                  </td>
                  <td className="px-4 py-3 text-gray-700">
                    ETB {(parseFloat(sale.totalAmount) - parseFloat(sale.taxAmount)).toFixed(2)}
                  </td>
                  <td className="px-4 py-3 text-gray-500">
                    ETB {parseFloat(sale.taxAmount).toFixed(2)}
                  </td>
                  <td className="px-4 py-3 font-semibold text-gray-900">
                    ETB {parseFloat(sale.totalAmount).toFixed(2)}
                  </td>
                  <td className="px-4 py-3 text-gray-400 text-xs">
                    {new Date(sale.saleDate).toLocaleDateString()}
                  </td>
                  <td className="px-4 py-3">
                    {/* Fix 3 & 4: show "View Receipt" if already generated, else "Invoice" */}
                    {generatedInvoices[sale.id] ? (
                      <button onClick={() => viewInvoice(sale)}
                        className="text-xs px-2 py-1 bg-teal-50 text-teal-700 rounded hover:bg-teal-100 font-medium">
                        View Receipt
                      </button>
                    ) : (
                      <button onClick={() => viewInvoice(sale)}
                        className="text-xs px-2 py-1 bg-blue-50 text-blue-600 rounded hover:bg-blue-100">
                        Invoice
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        {totalPages > 1 && (
          <div className="px-4 py-3 border-t border-gray-100 flex items-center justify-between">
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

      {/* New Sale Modal */}
      {showNewSale && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-2xl max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between px-6 py-4 border-b border-gray-100 sticky top-0 bg-white">
              <h3 className="font-semibold text-gray-900">New Sale — Point of Sale</h3>
              <button onClick={() => setShowNewSale(false)} className="text-gray-400 hover:text-gray-600">✕</button>
            </div>
            <form onSubmit={handleCreateSale} className="p-6 space-y-4">
              {error && <p className="text-red-600 text-sm bg-red-50 px-3 py-2 rounded-lg">{error}</p>}

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-medium text-gray-700 mb-1">Patient Name</label>
                  <input required value={saleForm.patientName}
                    onChange={e => setSaleForm({...saleForm, patientName: e.target.value})}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-teal-500"
                    placeholder="Abebe Girma"/>
                </div>
                <div>
                  <label className="block text-xs font-medium text-gray-700 mb-1">Payment Method</label>
                  <select value={saleForm.paymentMethod}
                    onChange={e => setSaleForm({...saleForm, paymentMethod: e.target.value})}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-teal-500">
                    <option value="CASH">Cash</option>
                    <option value="CARD">Card</option>
                    <option value="MOBILE_MONEY">Mobile Money</option>
                  </select>
                </div>
              </div>

              {/* Drug Items */}
              <div>
                <div className="flex items-center justify-between mb-2">
                  <label className="text-xs font-medium text-gray-700">Drug Items</label>
                  <button type="button" onClick={addItem}
                    className="text-xs text-teal-600 hover:underline">+ Add Item</button>
                </div>
                <div className="space-y-2">
                  {saleItems.map((item, i) => (
                    <div key={i} className="flex items-center gap-2">
                      <select value={item.drugId}
                        onChange={e => updateItem(i, 'drugId', e.target.value)}
                        className="flex-1 px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-teal-500">
                        <option value="">Select drug</option>
                        {drugs.map(d => (
                          <option key={d.id} value={d.id}>
                            {d.name} — ETB {d.price} (Stock: {d.stockQty})
                          </option>
                        ))}
                      </select>
                      <input type="number" min="1" value={item.quantity}
                        onChange={e => updateItem(i, 'quantity', e.target.value)}
                        className="w-20 px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-teal-500"
                        placeholder="Qty"/>
                      <span className="text-sm text-gray-500 w-24 text-right">
                        ETB {(getDrugPrice(item.drugId) * (parseInt(item.quantity) || 0)).toFixed(2)}
                      </span>
                      {saleItems.length > 1 && (
                        <button type="button" onClick={() => removeItem(i)}
                          className="text-red-400 hover:text-red-600 p-1">✕</button>
                      )}
                    </div>
                  ))}
                </div>
              </div>

              {/* Totals */}
              <div className="bg-teal-50 rounded-xl p-4 space-y-2 border border-teal-100">
                <div className="flex justify-between text-sm">
                  <span className="text-gray-500">Subtotal</span>
                  <span className="font-medium">ETB {calcSubtotal().toFixed(2)}</span>
                </div>
                <div className="flex justify-between text-sm">
                  <span className="text-gray-500">Tax (15%)</span>
                  <span className="font-medium">ETB {calcTax().toFixed(2)}</span>
                </div>
                <div className="flex justify-between text-base font-bold border-t border-teal-200 pt-2">
                  <span>Total</span>
                  <span className="text-teal-700">ETB {calcTotal().toFixed(2)}</span>
                </div>
              </div>

              <div className="flex gap-3">
                <button type="button" onClick={() => setShowNewSale(false)}
                  className="flex-1 px-4 py-2 border border-gray-300 rounded-lg text-sm text-gray-700 hover:bg-gray-50">Cancel</button>
                <button type="submit"
                  className="flex-1 px-4 py-2 bg-teal-600 hover:bg-teal-700 text-white rounded-lg text-sm font-medium">
                  Complete Sale & Generate Receipt
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Invoice / Receipt Modal — Fix 5: professional pharmacy colors */}
      {showInvoice && invoice && (
        <div className="fixed inset-0 bg-black bg-opacity-60 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md overflow-hidden">

            {/* Fix 5: colored header bar */}
            <div className="bg-gradient-to-r from-teal-700 to-teal-500 px-6 py-5 text-white">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 bg-white bg-opacity-20 rounded-xl flex items-center justify-center">
                    <svg className="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
                    </svg>
                  </div>
                  <div>
                    <h2 className="font-bold text-lg">RxPharma</h2>
                    <p className="text-teal-100 text-xs">Official Pharmacy Receipt</p>
                  </div>
                </div>
                <button onClick={() => setShowInvoice(false)}
                  className="text-white text-opacity-70 hover:text-opacity-100 text-xl">✕</button>
              </div>

              {/* Fix 5: invoice number + PAID status badge */}
              <div className="mt-4 flex items-center justify-between">
                <div>
                  <p className="text-teal-200 text-xs">Invoice Number</p>
                  <p className="font-mono font-bold text-white">{invoice.invoiceNumber}</p>
                </div>
                <div className="flex flex-col items-end gap-1">
                  <span className="bg-green-400 text-green-900 text-xs font-bold px-3 py-1 rounded-full">
                    ✓ PAID
                  </span>
                  <span className="bg-white bg-opacity-20 text-white text-xs px-3 py-1 rounded-full">
                    {invoice.paymentMethod}
                  </span>
                </div>
              </div>
            </div>

            <div className="p-6">
              {/* Info Grid */}
              <div className="grid grid-cols-2 gap-3 text-xs mb-5">
                <div className="bg-gray-50 rounded-lg p-3">
                  <p className="text-gray-400 mb-0.5">Patient</p>
                  <p className="font-semibold text-gray-800">{invoice.patientName}</p>
                </div>
                <div className="bg-gray-50 rounded-lg p-3">
                  <p className="text-gray-400 mb-0.5">Cashier</p>
                  <p className="font-semibold text-gray-800">{invoice.cashierName}</p>
                </div>
                <div className="bg-gray-50 rounded-lg p-3 col-span-2">
                  <p className="text-gray-400 mb-0.5">Date & Time</p>
                  <p className="font-semibold text-gray-800">
                    {new Date(invoice.saleDate).toLocaleString()}
                  </p>
                </div>
              </div>

              {/* Items */}
              <div className="border border-dashed border-teal-200 rounded-xl p-4 mb-4">
                <p className="text-xs font-bold text-teal-700 uppercase mb-3 tracking-wider">Dispensed Items</p>
                <div className="space-y-3">
                  {invoice.items?.map(item => (
                    <div key={item.id} className="flex items-center justify-between">
                      <div>
                        <p className="text-sm font-medium text-gray-900">{item.drugName}</p>
                        <p className="text-xs text-gray-400">×{item.quantity} @ ETB {parseFloat(item.unitPrice).toFixed(2)}</p>
                      </div>
                      <p className="text-sm font-semibold text-gray-800">
                        ETB {parseFloat(item.subtotal).toFixed(2)}
                      </p>
                    </div>
                  ))}
                </div>
              </div>

              {/* Totals */}
              <div className="space-y-2 mb-5">
                <div className="flex justify-between text-sm text-gray-500">
                  <span>Subtotal</span>
                  <span>ETB {parseFloat(invoice.subtotal).toFixed(2)}</span>
                </div>
                <div className="flex justify-between text-sm text-gray-500">
                  <span>Tax (15%)</span>
                  <span>ETB {parseFloat(invoice.taxAmount).toFixed(2)}</span>
                </div>
                <div className="flex justify-between text-base font-bold pt-2 border-t-2 border-teal-500 text-teal-700">
                  <span>Total Paid</span>
                  <span>ETB {parseFloat(invoice.totalAmount).toFixed(2)}</span>
                </div>
              </div>

              {/* Footer */}
              <div className="bg-teal-50 rounded-xl p-3 text-center border border-teal-100">
                <p className="text-xs text-teal-700 font-medium">Thank you for choosing RxPharma</p>
                <p className="text-xs text-teal-500 mt-0.5">Keep this receipt for your records</p>
              </div>

              {/* Fix 3 & 4: only show View Receipt button — no re-generate option */}
              <button onClick={() => setShowInvoice(false)}
                className="mt-4 w-full px-4 py-2 bg-teal-600 hover:bg-teal-700 text-white rounded-lg text-sm font-medium">
                Close Receipt
              </button>
            </div>
          </div>
        </div>
      )}
    </DashboardLayout>
  )
}