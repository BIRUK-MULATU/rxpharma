import { useState } from 'react'
import { getCategories, saveCategories, DEFAULT_CATEGORIES } from '../../utils/constants'

export default function CategoriesManager({ onClose }) {
  const [categories, setCategories] = useState(getCategories())
  const [newCategory, setNewCategory] = useState('')
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  const handleAdd = () => {
    const trimmed = newCategory.trim()
    if (!trimmed) { setError('Category name is required'); return }
    if (categories.map(c => c.toLowerCase()).includes(trimmed.toLowerCase())) {
      setError('Category already exists')
      return
    }
    const updated = [...categories, trimmed].sort()
    setCategories(updated)
    saveCategories(updated)
    setNewCategory('')
    setSuccess(`"${trimmed}" added successfully`)
    setError('')
  }

  const handleDelete = (cat) => {
    if (!confirm(`Delete category "${cat}"?`)) return
    const updated = categories.filter(c => c !== cat)
    setCategories(updated)
    saveCategories(updated)
    setSuccess(`"${cat}" deleted`)
  }

  const handleReset = () => {
    if (!confirm('Reset to default categories? All custom categories will be lost.')) return
    setCategories(DEFAULT_CATEGORIES)
    saveCategories(DEFAULT_CATEGORIES)
    setSuccess('Categories reset to defaults')
  }

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md max-h-[85vh] flex flex-col">

        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-primary-100">
          <div>
            <h3 className="font-semibold text-gray-900">Manage Drug Categories</h3>
            <p className="text-xs text-gray-400 mt-0.5">{categories.length} categories</p>
          </div>
          <button onClick={onClose} className="text-gray-400 hover:text-gray-600">✕</button>
        </div>

        {/* Alerts */}
        <div className="px-6 pt-4">
          {error && (
            <div className="mb-3 bg-red-50 border border-red-200 text-red-700 px-3 py-2 rounded-lg text-xs flex justify-between">
              <span>{error}</span><button onClick={() => setError('')}>✕</button>
            </div>
          )}
          {success && (
            <div className="mb-3 bg-green-50 border border-green-200 text-green-700 px-3 py-2 rounded-lg text-xs flex justify-between">
              <span>{success}</span><button onClick={() => setSuccess('')}>✕</button>
            </div>
          )}
        </div>

        {/* Add New */}
        <div className="px-6 pb-4">
          <label className="block text-xs font-medium text-gray-700 mb-1">Add New Category</label>
          <div className="flex gap-2">
            <input
              value={newCategory}
              onChange={e => setNewCategory(e.target.value)}
              onKeyDown={e => e.key === 'Enter' && (e.preventDefault(), handleAdd())}
              className="flex-1 px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-accent-500"
              placeholder="e.g. Neurology"
            />
            <button onClick={handleAdd}
              className="px-4 py-2 bg-gradient-to-r from-accent-600 to-accent-500 hover:from-accent-700 hover:to-accent-600 text-white rounded-lg text-sm font-medium shadow-md">
              Add
            </button>
          </div>
        </div>

        {/* Categories List */}
        <div className="flex-1 overflow-y-auto px-6 pb-2">
          <p className="text-xs font-medium text-gray-500 uppercase tracking-wider mb-2">
            All Categories
          </p>
          <div className="space-y-1">
            {categories.map(cat => (
              <div key={cat}
                className="flex items-center justify-between px-3 py-2.5 bg-gray-50 rounded-lg hover:bg-gray-100 transition-colors">
                <div className="flex items-center gap-2">
                  <div className="w-2 h-2 bg-accent-500 rounded-full"/>
                  <span className="text-sm text-gray-700">{cat}</span>
                </div>
                <button onClick={() => handleDelete(cat)}
                  className="text-red-400 hover:text-red-600 text-xs px-2 py-0.5 rounded hover:bg-red-50">
                  Delete
                </button>
              </div>
            ))}
          </div>
        </div>

        {/* Footer */}
        <div className="px-6 py-4 border-t border-primary-100 flex gap-3">
          <button onClick={handleReset}
            className="flex-1 px-4 py-2 border border-gray-300 text-gray-600 rounded-lg text-sm hover:bg-gray-50">
            Reset to Defaults
          </button>
          <button onClick={onClose}
            className="flex-1 px-4 py-2 bg-gradient-to-r from-accent-600 to-accent-500 hover:from-accent-700 hover:to-accent-600 text-white rounded-lg text-sm font-medium shadow-md">
            Done
          </button>
        </div>
      </div>
    </div>
  )
}