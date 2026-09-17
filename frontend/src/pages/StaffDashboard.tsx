import { useAuth } from '../contexts/AuthContext'
import { useNavigate } from 'react-router-dom'
import StaffChatPanel from '../components/StaffChatPanel'
import Dashboard from '../components/Dashboard'
import { useState } from 'react'

export default function StaffDashboard() {
  const { user } = useAuth()
  const navigate = useNavigate()
  const [refreshKey, setRefreshKey] = useState(0)

  return (
    <div className="app">
      <header className="header">
        <div className="header-left">
          <div className="logo">M</div>
          <div>
            <h1>MSB Staff Dashboard</h1>
            <p className="subtitle">{user?.fullName} ({user?.role})</p>
          </div>
        </div>
        <div className="header-right">
          <button className="header-btn" onClick={() => navigate('/staff/history')}>Lịch sử chat</button>
          {user?.role === 'ADMIN' && (
            <button className="header-btn" onClick={() => navigate('/admin/users')}>Quản lý users</button>
          )}
          <button className="header-btn" onClick={() => navigate('/')}>← Trang chủ</button>
        </div>
      </header>
      <div className="main-content">
        <StaffChatPanel />
        <Dashboard refreshKey={refreshKey} />
      </div>
    </div>
  )
}
