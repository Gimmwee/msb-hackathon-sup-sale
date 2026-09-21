import { Outlet, NavLink, useNavigate } from 'react-router-dom'
import { useAuth } from '../../contexts/AuthContext'
import ChatWidget from '../../components/ChatWidget'

const NAV_ITEMS = [
  { to: '/sale/upsale', icon: '📊', label: 'Data UpSale' },
  { to: '/sale/transactions', icon: '💳', label: 'Lịch sử giao dịch' },
  { to: '/sale/lookup', icon: '🔍', label: 'Tra cứu khách hàng' },
  { to: '/sale/dashboard', icon: '📈', label: 'Dashboard KPI' },
]

export default function SaleLayout() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const staffSessionId = `staff-${user?.fullName?.replace(/\s/g, '')}-${Date.now()}`

  return (
    <div className="sale-layout">
      <aside className="sale-sidebar">
        <div className="sale-sidebar-header">
          <h2>MSB Sale</h2>
          <p>{user?.fullName}</p>
        </div>
        <nav>
          {NAV_ITEMS.map(item => (
            <NavLink key={item.to} to={item.to} className={({ isActive }) =>
              `sale-nav-item ${isActive ? 'active' : ''}`
            }>
              <span style={{ fontSize: '18px' }}>{item.icon}</span>
              <span>{item.label}</span>
            </NavLink>
          ))}
        </nav>
        <div className="sale-sidebar-footer">
          <button className="header-btn" style={{ margin: 0 }} onClick={() => navigate('/')}>← Trang chủ</button>
          <button className="header-btn" style={{ margin: 0 }} onClick={() => { logout(); navigate('/login') }}>Đăng xuất</button>
        </div>
      </aside>
      <main className="sale-content">
        <Outlet />
      </main>
      <ChatWidget sessionId={staffSessionId} mode="staff" />
    </div>
  )
}
