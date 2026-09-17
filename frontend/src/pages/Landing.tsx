import { useNavigate } from 'react-router-dom'

export default function Landing() {
  const navigate = useNavigate()

  return (
    <div className="landing-page">
      <div className="landing-content">
        <div className="landing-logo">M</div>
        <h1>MSB AI Customer Assistant</h1>
        <p className="landing-subtitle">sup-sale — Hệ thống tư vấn khách hàng AI</p>

        <div className="role-cards">
          <div className="role-card customer" onClick={() => navigate('/customer')}>
            <div className="role-icon">👤</div>
            <h2>Tôi là khách hàng</h2>
            <p>Chat với tư vấn viên AI, tư vấn sản phẩm vay, mở thẻ</p>
          </div>

          <div className="role-card staff" onClick={() => navigate('/login')}>
            <div className="role-icon">💼</div>
            <h2>Tôi là nhân viên</h2>
            <p>Đăng nhập hệ thống nội bộ, tra cứu CIC, quản lý khách hàng</p>
          </div>
        </div>
      </div>
    </div>
  )
}
