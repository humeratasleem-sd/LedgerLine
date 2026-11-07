import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from './useAuth.js'

export function ProtectedRoute() {
  const { user } = useAuth()
  const location = useLocation()
  return user
    ? <Outlet />
    : <Navigate to="/login" replace state={{ from: location.pathname }} />
}

export function RoleRoute({ roles }) {
  const { user } = useAuth()
  return user && roles.includes(user.role)
    ? <Outlet />
    : <Navigate to="/" replace />
}

export function PublicOnlyRoute({ children }) {
  const { user } = useAuth()
  return user ? <Navigate to="/" replace /> : children
}
