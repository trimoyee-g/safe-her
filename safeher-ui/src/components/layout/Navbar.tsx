import { Link, NavLink, useNavigate } from 'react-router-dom'
import clsx from 'clsx'
import { useAuthStore } from '@/store/auth.store'
import { authApi } from '@/api/auth.api'
import { Avatar } from '@/components/ui'
import { useState } from 'react'

export function Navbar() {
  const { isAuthenticated, username, role, logout, refreshToken } = useAuthStore()
  const navigate = useNavigate()
  const [menuOpen, setMenuOpen] = useState(false)

  const handleLogout = async () => {
    try {
      await authApi.logout(refreshToken ?? undefined)
    } finally {
      logout()
      navigate('/login')
    }
  }

  const navLink = ({ isActive }: { isActive: boolean }) =>
    clsx('text-sm font-medium transition-colors', isActive ? 'text-brand-200' : 'text-gray-400 hover:text-gray-100')

  return (
    <header className="sticky top-0 z-50 bg-gray-950/90 backdrop-blur border-b border-gray-800">
      <div className="max-w-7xl mx-auto px-4 h-14 flex items-center gap-6">
        {/* Logo */}
        <Link to="/" className="flex items-center gap-2 flex-shrink-0">
          <div className="w-7 h-7 bg-brand-400 rounded-lg flex items-center justify-center">
            <svg className="w-4 h-4 text-brand-950" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <path strokeLinecap="round" strokeLinejoin="round"
                d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
            </svg>
          </div>
          <span className="font-semibold text-gray-100 text-sm">SafeHer</span>
        </Link>

        {/* Nav links */}
        <nav className="hidden sm:flex items-center gap-5 flex-1">
          {isAuthenticated && (
            <NavLink to="/chat" className={navLink}>
              <span className="flex items-center gap-1.5">
                <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                  <path strokeLinecap="round" strokeLinejoin="round" d="M8 10h.01M12 10h.01M16 10h.01M9 16H5a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v8a2 2 0 01-2 2h-5l-5 5v-5z" />
                </svg>
                Ask
              </span>
            </NavLink>
          )}
          <NavLink to="/search" className={navLink}>Map</NavLink>
          {isAuthenticated && (
            <NavLink to="/add-place" className={navLink}>Add place</NavLink>
          )}
          {(role === 'ADMIN' || role === 'MODERATOR') && (
            <NavLink to="/admin" className={navLink}>Admin</NavLink>
          )}
        </nav>

        {/* Right side */}
        <div className="ml-auto flex items-center gap-2">
          {isAuthenticated ? (
            <div className="relative">
              <button
                onClick={() => setMenuOpen(!menuOpen)}
                className="flex items-center gap-2 px-2 py-1 rounded-lg hover:bg-gray-900 transition-colors"
              >
                <Avatar name={username ?? 'U'} size="sm" />
                <span className="hidden sm:block text-sm font-medium text-gray-300 max-w-24 truncate">
                  {username}
                </span>
              </button>
              {menuOpen && (
                <div className="absolute right-0 top-full mt-1 w-44 bg-gray-900 border border-gray-800 rounded-xl shadow-lg py-1 z-50">
                  <Link to="/profile" onClick={() => setMenuOpen(false)}
                    className="flex items-center gap-2 px-4 py-2 text-sm text-gray-300 hover:bg-gray-800">
                    Profile
                  </Link>
                  <Link to="/profile/reviews" onClick={() => setMenuOpen(false)}
                    className="flex items-center gap-2 px-4 py-2 text-sm text-gray-300 hover:bg-gray-800">
                    My reviews
                  </Link>
                  <Link to="/settings" onClick={() => setMenuOpen(false)}
                    className="flex items-center gap-2 px-4 py-2 text-sm text-gray-300 hover:bg-gray-800">
                    Settings
                  </Link>
                  <div className="border-t border-gray-800 my-1" />
                  <button onClick={handleLogout}
                    className="w-full text-left px-4 py-2 text-sm text-red-400 hover:bg-red-500/10">
                    Sign out
                  </button>
                </div>
              )}
            </div>
          ) : (
            <div className="flex items-center gap-2">
              <Link to="/login"
                className="text-sm font-medium text-gray-400 hover:text-gray-100 px-3 py-1.5">
                Sign in
              </Link>
              <Link to="/register"
                className="text-sm font-medium bg-brand-400 text-brand-950 px-3 py-1.5 rounded-lg hover:bg-brand-200 transition-colors">
                Register
              </Link>
            </div>
          )}
        </div>
      </div>
    </header>
  )
}
