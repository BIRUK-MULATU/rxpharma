export const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8083'

export const ROLES = {
  ADMIN: 'ADMIN',
  PHARMACIST: 'PHARMACIST',
  CASHIER: 'CASHIER',
  SUPPLIER_MANAGER: 'SUPPLIER_MANAGER'
}

export const ROUTES = {
  LOGIN: '/login',
  FORGOT_PASSWORD: '/forgot-password',
  DASHBOARD: '/dashboard',
  DRUGS: '/drugs',
  SUPPLIERS: '/suppliers',
  PRESCRIPTIONS: '/prescriptions',
  SALES: '/sales',
  PURCHASE_ORDERS: '/purchase-orders',
  USERS: '/users',
}

export const DEFAULT_CATEGORIES = [
  'Antibiotic',
  'Analgesic',
  'Antiviral',
  'Antifungal',
  'Supplement',
  'Antihistamine',
  'Antidiabetic',
  'Antihypertensive',
  'Antidepressant',
  'Antiparasitic',
  'Cardiovascular',
  'Dermatological',
  'Gastrointestinal',
  'Hormonal',
  'Respiratory',
  'Vaccine',
  'Vitamins & Minerals',
]

export const STORAGE_KEY_CATEGORIES = 'rxpharma_drug_categories'

export const getCategories = () => {
  try {
    const stored = localStorage.getItem(STORAGE_KEY_CATEGORIES)
    if (stored) return JSON.parse(stored)
    return DEFAULT_CATEGORIES
  } catch {
    return DEFAULT_CATEGORIES
  }
}

export const saveCategories = (categories) => {
  localStorage.setItem(STORAGE_KEY_CATEGORIES, JSON.stringify(categories))
}
