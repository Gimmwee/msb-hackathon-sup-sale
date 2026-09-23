import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import ChatWidget from '../components/ChatWidget'

export default function CustomerView() {
  const navigate = useNavigate()
  const [chatOpen, setChatOpen] = useState(false)
  const sessionId = sessionStorage.getItem('customer_session_id') || (() => {
    const id = `web-${Date.now()}`
    sessionStorage.setItem('customer_session_id', id)
    return id
  })()

  const hour = new Date().getHours()
  const greeting = hour < 12 ? 'Chào buổi sáng' : hour < 18 ? 'Chào buổi chiều' : 'Chào buổi tối'
  const productGroups = [
    {
      id: 'loans', icon: '🏦', title: 'Vay vốn', desc: 'Giải pháp tài chính linh hoạt cho kế hoạch lớn.',
      items: ['Vay tín chấp từ 20 triệu', 'Vay mua nhà, mua xe', 'Tư vấn hồ sơ cùng trợ lý AI']
    },
    {
      id: 'cards', icon: '💳', title: 'Thẻ tín dụng', desc: 'Chi tiêu chủ động, ưu đãi thiết thực mỗi ngày.',
      items: ['Miễn phí thường niên năm đầu', 'Ưu đãi hoàn tiền và mua sắm', 'Hỗ trợ mở thẻ nhanh chóng']
    },
    {
      id: 'savings', icon: '💰', title: 'Tiết kiệm', desc: 'Tích lũy an tâm với kỳ hạn phù hợp mục tiêu.',
      items: ['Lãi suất cạnh tranh', 'Kỳ hạn linh hoạt từ 1 tháng', 'Theo dõi khoản gửi trên mBank']
    },
    {
      id: 'digital', icon: '📱', title: 'Ngân hàng số', desc: 'Mọi giao dịch trong tầm tay với MSB mBank.',
      items: ['Chuyển tiền 24/7', 'Thanh toán hóa đơn tiện lợi', 'Quản lý tài chính cá nhân']
    },
  ]

  const scrollToProduct = (id: string) => document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'start' })

  return (
    <div className="customer-home">
      <nav className="msb-nav">
        <img src="/assets/logologinchatbot.png" alt="MSB" className="msb-nav-logo-img" />
        <div className="msb-nav-items">
          <button onClick={() => scrollToProduct('loans')}>Cá nhân</button>
          <button onClick={() => scrollToProduct('digital')}>Doanh nghiệp</button>
          <button onClick={() => scrollToProduct('savings')}>Khách hàng ưu tiên</button>
        </div>
        <button className="msb-nav-login" onClick={() => navigate('/login')}>Đăng nhập</button>
      </nav>

      <section className="msb-hero">
        <div className="msb-hero-overlay"></div>
        <div className="msb-hero-content">
          <h1>{greeting},<br/>khách hàng MSB!</h1>
          <p>Trợ lý AI sup-sale — tư vấn tài chính 24/7.<br/>Vay vốn, thẻ tín dụng, tiết kiệm.</p>
          <button className="msb-hero-cta" onClick={() => setChatOpen(true)}>
            💬 Liên hệ và tư vấn
          </button>
        </div>
        <button className="msb-hero-arrow" aria-label="Next">›</button>

        <div className="msb-categories">
          {[
            { icon: '🏦', title: 'Vay vốn', desc: 'Tín chấp, mua nhà, mua xe' },
            { icon: '💳', title: 'Thẻ tín dụng', desc: 'Mastercard, Visa' },
            { icon: '💰', title: 'Tiết kiệm', desc: 'Kỳ hạn linh hoạt' },
            { icon: '📱', title: 'Ngân hàng số', desc: 'MSB mBank 24/7' },
          ].map(cat => (
            <button key={cat.title} className="msb-category-item" onClick={() => scrollToProduct(productGroups.find(group => group.title === cat.title)?.id || '')}>
              <span className="msb-category-icon">{cat.icon}</span>
              <div>
                <h3>{cat.title}</h3>
                <p>{cat.desc}</p>
              </div>
            </button>
          ))}
        </div>
      </section>

      <section className="msb-products" aria-label="Sản phẩm nổi bật">
        <div className="msb-products-heading">
          <p className="msb-eyebrow">MSB đồng hành cùng bạn</p>
          <h2>Chọn giải pháp phù hợp hôm nay</h2>
          <p>Khám phá các sản phẩm tài chính được thiết kế đơn giản, minh bạch và dễ tiếp cận.</p>
        </div>
        <div className="msb-product-grid">
          {productGroups.map(product => (
            <article key={product.id} id={product.id} className="msb-product-card">
              <div className="msb-product-icon">{product.icon}</div>
              <h3>{product.title}</h3>
              <p>{product.desc}</p>
              <ul>{product.items.map(item => <li key={item}>{item}</li>)}</ul>
              <button onClick={() => setChatOpen(true)}>Tư vấn ngay <span>→</span></button>
            </article>
          ))}
        </div>
      </section>

      <ChatWidget sessionId={sessionId} onLeadCaptured={() => {}} open={chatOpen} onToggle={setChatOpen} />
    </div>
  )
}
