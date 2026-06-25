export default function Navbar({ onMenuClick, title }) {
  return (
    <header className="h-16 bg-white border-b border-primary-100 flex items-center px-4 gap-4 shadow-sm">
      {/* Mobile menu button */}
      <button
        onClick={onMenuClick}
        className="lg:hidden p-2 rounded-lg text-primary-500 hover:bg-primary-50 transition-colors"
      >
        <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" />
        </svg>
      </button>

      <h2 className="text-lg font-semibold text-primary-700">{title}</h2>

      <div className="ml-auto flex items-center gap-3">
        <div className="text-xs text-primary-300 hidden sm:block">
          Backend: <span className="text-emerald-500 font-medium">● Online</span>
        </div>
      </div>
    </header>
  )
}