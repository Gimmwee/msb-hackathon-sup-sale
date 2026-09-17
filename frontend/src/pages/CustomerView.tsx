import Header from '../components/Header'
import ChatPanel from '../components/ChatPanel'
import { useNavigate } from 'react-router-dom'

function CustomerView() {
  const navigate = useNavigate()

  let sessionId = localStorage.getItem('customer_session_id')
  if (!sessionId) {
    sessionId = `web-${Date.now()}`
    localStorage.setItem('customer_session_id', sessionId)
  }

  return (
    <div className="app">
      <Header />
      <div className="main-content" style={{ flexDirection: 'column', alignItems: 'center', justifyContent: 'flex-start', paddingTop: '20px' }}>
        <div style={{ width: '100%', maxWidth: '600px' }}>
          <ChatPanel sessionId={sessionId} onLeadCaptured={() => {}} />
          <div style={{ textAlign: 'center', marginTop: '16px' }}>
            <button className="quick-btn" onClick={() => navigate('/')}>← Quay lại trang chủ</button>
          </div>
        </div>
      </div>
    </div>
  )
}

export default CustomerView
