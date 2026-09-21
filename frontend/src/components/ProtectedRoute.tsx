import { Navigate } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'

interface Props {
  children: React.ReactNode
  requiredRole?: string
  roles?: string[]
}

export default function ProtectedRoute({ children, requiredRole, roles }: Props) {
  const { user } = useAuth()

  if (!user) {
    return <Navigate to="/login" replace />
  }

  if (requiredRole && user.role !== requiredRole) {
    return <Navigate to="/login" replace state={{ error: 'Không đủ quyền truy cập' }} />
  }

  if (roles && !roles.includes(user.role)) {
    const dest = user.role === 'ADMIN' ? '/admin/users' : user.role === 'SALE' ? '/sale/upsale' : '/cc'
    return <Navigate to={dest} replace state={{ error: 'Không đủ quyền truy cập' }} />
  }

  return <>{children}</>
}
