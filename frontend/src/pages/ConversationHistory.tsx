import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import { getConversations } from '../services/api'

interface ConvMessage { role: string; content: string; createdAt: string }
interface Conv { id: string; sessionId: string; platform: string; createdAt: string; messages: ConvMessage[] }

export default function ConversationHistory() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const [conversations, setConversations] = useState<Conv[]>([])
  const [selected, setSelected] = useState<Conv | null>(null)
  const [error, setError] = useState('')

  useEffect(() => {
    const fetchConvs = async () => {
      try {
        const data = await getConversations() as Conv[]
        const sorted = data.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
        setConversations(sorted)
      } catch { setError('Không tải được lịch sử chat') }
    }
    fetchConvs()
  }, [])

  return (
    <div className="app">
      <header className="header">
        <div className="header-left">
          <img src="/assets/logologinchatbot.png" alt="MSB" className="logo-image" />
          <div>
            <h1>Lịch sử hội thoại</h1>
            <p className="subtitle">{user?.fullName} ({user?.role})</p>
          </div>
        </div>
        <div className="header-right">
          <button className="header-btn" onClick={() => navigate('/sale/upsale')}>← Sale Dashboard</button>
          <button className="header-btn" onClick={() => { logout(); navigate('/') }}>Đăng xuất</button>
        </div>
      </header>
      <div style={{ display: 'flex', gap: '20px', padding: '20px', flex: 1, maxHeight: 'calc(100vh - 80px)' }}>
        <div style={{ flex: '0 0 35%', overflowY: 'auto', background: 'white', borderRadius: '16px', padding: '16px' }}>
          <h3 style={{ marginBottom: '12px' }}>Danh sách phiên ({conversations.length})</h3>
          {error && <div className="error-msg">{error}</div>}
          {conversations.map(conv => (
            <div key={conv.id}
                 onClick={() => setSelected(conv)}
                 style={{
                   padding: '12px', borderRadius: '10px', cursor: 'pointer', marginBottom: '8px',
                   background: selected?.id === conv.id ? '#E31837' : '#f8f9ff',
                   color: selected?.id === conv.id ? 'white' : '#1a1a2e',
                 }}>
              <div style={{ fontWeight: 600, fontSize: '13px' }}>{conv.sessionId}</div>
              <div style={{ fontSize: '11px', opacity: 0.7 }}>
                {conv.platform} • {conv.messages.length} tin nhắn • {new Date(conv.createdAt).toLocaleString('vi-VN')}
              </div>
            </div>
          ))}
        </div>
        <div style={{ flex: 1, overflowY: 'auto', background: 'white', borderRadius: '16px', padding: '20px' }}>
          {selected ? (
            <>
              <h3 style={{ marginBottom: '16px' }}>Chi tiết: {selected.sessionId}</h3>
              {selected.messages.map((msg, i) => (
                <div key={i} className={`message ${msg.role}`} style={{ marginBottom: '12px' }}>
                  <div style={{ fontSize: '11px', color: '#999', marginBottom: '2px' }}>
                    {msg.role === 'user' ? '👤 Khách hàng' : '🤖 AI'} • {new Date(msg.createdAt).toLocaleTimeString('vi-VN')}
                  </div>
                  <div className="message-bubble">{msg.content}</div>
                </div>
              ))}
            </>
          ) : (
            <div style={{ textAlign: 'center', color: '#999', padding: '40px' }}>Chọn một phiên để xem chi tiết</div>
          )}
        </div>
      </div>
    </div>
  )
}
