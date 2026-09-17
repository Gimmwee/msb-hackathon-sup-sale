import { BrowserRouter, Routes, Route } from 'react-router-dom'
import { AuthProvider } from './contexts/AuthContext'
import Landing from './pages/Landing'
import Login from './pages/Login'
import CustomerView from './pages/CustomerView'
import StaffDashboard from './pages/StaffDashboard'
import AdminUsers from './pages/AdminUsers'
import ProtectedRoute from './components/ProtectedRoute'
import ConversationHistory from './pages/ConversationHistory'

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<Landing />} />
          <Route path="/customer" element={<CustomerView />} />
          <Route path="/login" element={<Login />} />
          <Route path="/staff/dashboard" element={
            <ProtectedRoute>
              <StaffDashboard />
            </ProtectedRoute>
          } />
          <Route path="/staff/history" element={
            <ProtectedRoute>
              <ConversationHistory />
            </ProtectedRoute>
          } />
          <Route path="/admin/users" element={
            <ProtectedRoute requiredRole="ADMIN">
              <AdminUsers />
            </ProtectedRoute>
          } />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  )
}
