import { useTheme } from '../../context/ThemeContext'

export default function Navbar({ onMenuClick, title }) {
  const { dark, toggle } = useTheme()

  return (
    <header className="h-16 bg-white dark:bg-primary-900 border-b border-primary-100 dark:border-primary-700 flex items-center px-4 gap-4 shadow-sm dark:shadow-none">
      {/* Mobile menu button */}
      <button
        onClick={onMenuClick}
        className="lg:hidden p-2 rounded-lg text-primary-500 dark:text-primary-300 hover:bg-primary-50 dark:hover:bg-primary-800 transition-colors"
      >
        <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" />
        </svg>
      </button>

      <h2 className="text-lg font-semibold text-primary-700 dark:text-primary-100">{title}</h2>

      <div className="ml-auto flex items-center gap-3">
        {/* Theme toggle */}
        <button
          onClick={toggle}
          className="p-2 rounded-lg text-primary-500 dark:text-primary-300 hover:bg-primary-50 dark:hover:bg-primary-800 transition-colors"
          title={dark ? 'Switch to light mode' : 'Switch to dark mode'}
        >
          {dark ? (
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 3v1m0 16v1m9-9h-1M4 12H3m15.364 6.364l-.707-.707M6.343 6.343l-.707-.707m12.728 0l-.707.707M6.343 17.657l-.707.707M16 12a4 4 0 11-8 0 4 4 0 018 0z" />
            </svg>
          ) : (
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M20.354 15.354A9 9 0 018.646 3.646 9.003 9.003 0 0012 21a9.003 9.003 0 008.354-5.646z" />
            </svg>
          )}
        </button>

        {/* <div className="text-xs text-primary-300 dark:text-primary-500 hidden sm:block">
          Backend: <span className="text-emerald-500 font-medium">● Online</span>
        </div> */}
      </div>
    </header>
  )
}