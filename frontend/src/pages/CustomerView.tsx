import { useNavigate } from 'react-router-dom'
import ChatWidget from '../components/ChatWidget'

export default function CustomerView() {
  const navigate = useNavigate()
  const sessionId = localStorage.getItem('customer_session_id') || (() => {
    const id = `web-${Date.now()}`
    localStorage.setItem('customer_session_id', id)
    return id
  })()

  return (
    <div className="customer-home">
      <nav className="msb-nav">
        <div className="msb-nav-logo">MSB</div>
        <div className="msb-nav-items">
          <span>Cá nhân</span>
          <span>Doanh nghiệp</span>
          <span>Khách hàng ưu tiên</span>
          <span>Về chúng tôi</span>
        </div>
        <button className="msb-nav-login" onClick={() => navigate('/login')}>Đăng nhập nội bộ</button>
      </nav>

      <section className="msb-hero">
        <div className="msb-hero-content">
          <h1>Tư vấn tài chính thông minh cùng MSB</h1>
          <p>sup-sale — Trợ lý AI giúp bạn tìm giải pháp vay vốn, thẻ tín dụng và tiết kiệm phù hợp. Chat ngay để được hỗ trợ.</p>
          <button className="msb-hero-cta" onClick={() => document.querySelector('.chat-fab')?.click()}>
            💬 Bắt đầu chat
          </button>
        </div>
        <div className="msb-hero-visual">
          <span className="msb-hero-visual-icon">🏦</span>
        </div>
      </section>

      <section className="msb-categories">
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
      </section>

      <ChatWidget sessionId={sessionId} onLeadCaptured={() => {}} />
    </div>
  )
}
