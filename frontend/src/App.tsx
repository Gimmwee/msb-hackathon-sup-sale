import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider } from './contexts/AuthContext'
import { ToastProvider } from './contexts/ToastContext'
import ProtectedRoute from './components/ProtectedRoute'
import CustomerView from './pages/CustomerView'
import Login from './pages/Login'
import ConversationHistory from './pages/ConversationHistory'
import AdminUsers from './pages/AdminUsers'
import SaleLayout from './pages/sale/SaleLayout'
import SaleUpSale from './pages/sale/SaleUpSale'
import SaleTransactions from './pages/sale/SaleTransactions'
import SaleLookup from './pages/sale/SaleLookup'
import SaleKpi from './pages/sale/SaleKpi'
import CcClaims from './pages/cc/CcClaims'

export default function App() {
  return (
    <AuthProvider>
      <ToastProvider>
        <BrowserRouter>
        <Routes>
          <Route path="/" element={<CustomerView />} />
          <Route path="/customer" element={<CustomerView />} />
          <Route path="/login" element={<Login />} />
          <Route path="/staff/dashboard" element={<Navigate to="/sale/upsale" replace />} />
          <Route path="/staff/history" element={<ProtectedRoute><ConversationHistory /></ProtectedRoute>} />
          <Route path="/admin/users" element={<ProtectedRoute requiredRole="ADMIN"><AdminUsers /></ProtectedRoute>} />
          <Route path="/sale" element={<ProtectedRoute roles={['SALE','ADMIN']}><SaleLayout /></ProtectedRoute>}>
            <Route path="upsale" element={<SaleUpSale />} />
            <Route path="transactions" element={<SaleTransactions />} />
            <Route path="lookup" element={<SaleLookup />} />
            <Route path="dashboard" element={<SaleKpi />} />
          </Route>
          <Route path="/cc" element={<ProtectedRoute roles={['CONTACT_CENTER','ADMIN']}><CcClaims /></ProtectedRoute>} />
        </Routes>
      </BrowserRouter>
      </ToastProvider>
    </AuthProvider>
  )
}
