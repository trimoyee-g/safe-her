import { Navigate, useLocation } from 'react-router-dom'
import { useAuthStore } from '@/store/auth.store'

interface ProtectedRouteProps {
  children: React.ReactNode
  requireRole?: 'ADMIN' | 'MODERATOR'
}

export function ProtectedRoute({ children, requireRole }: ProtectedRouteProps) {
  const { isAuthenticated, role } = useAuthStore()
  const location = useLocation()

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location.pathname }} replace />
  }

  if (requireRole && role !== requireRole && role !== 'ADMIN') {
    return <Navigate to="/" replace />
  }

  return <>{children}</>
}
