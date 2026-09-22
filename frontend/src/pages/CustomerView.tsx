import { useNavigate } from 'react-router-dom'
import ChatWidget from '../components/ChatWidget'

export default function CustomerView() {
  const navigate = useNavigate()
  const sessionId = localStorage.getItem('customer_session_id') || (() => {
    const id = `web-${Date.now()}`
    localStorage.setItem('customer_session_id', id)
    return id
  })()

  const hour = new Date().getHours()
  const greeting = hour < 12 ? 'Chào buổi sáng' : hour < 18 ? 'Chào buổi chiều' : 'Chào buổi tối'

  return (
    <div className="customer-home">
      <nav className="msb-nav">
        <div className="msb-nav-logo">MSB</div>
        <div className="msb-nav-items">
          <span>Cá nhân</span>
          <span>Doanh nghiệp</span>
          <span>Khách hàng ưu tiên</span>
          <span>Liên hệ & Hỗ trợ</span>
        </div>
        <button className="msb-nav-login" onClick={() => navigate('/login')}>Đăng nhập</button>
      </nav>

      <section className="msb-hero">
        <div className="msb-hero-overlay"></div>
        <div className="msb-hero-content">
          <h1>{greeting},<br/>khách hàng MSB!</h1>
          <p>Trợ lý AI sup-sale — tư vấn tài chính 24/7.<br/>Vay vốn, thẻ tín dụng, tiết kiệm.</p>
          <div className="msb-hero-search">
            <span className="msb-search-icon">🔍</span>
            <input type="text" placeholder="Tìm sản phẩm, dịch vụ..." readOnly />
          </div>
        </div>
        <button className="msb-hero-arrow" aria-label="Next">›</button>

        <div className="msb-categories">
          {[
            { icon: '🏦', title: 'Vay vốn', desc: 'Tín chấp, mua nhà, mua xe' },
            { icon: '💳', title: 'Thẻ tín dụng', desc: 'Mastercard, Visa' },
            { icon: '💰', title: 'Tiết kiệm', desc: 'Kỳ hạn linh hoạt' },
            { icon: '📱', title: 'Ngân hàng số', desc: 'MSB mBank 24/7' },
          ].map(cat => (
            <div key={cat.title} className="msb-category-item">
              <span className="msb-category-icon">{cat.icon}</span>
              <div>
                <h3>{cat.title}</h3>
                <p>{cat.desc}</p>
              </div>
            </div>
          ))}
        </div>
      </section>

      <ChatWidget sessionId={sessionId} onLeadCaptured={() => {}} />
    </div>
  )
}
