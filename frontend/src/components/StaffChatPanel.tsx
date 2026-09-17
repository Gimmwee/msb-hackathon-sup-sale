import { useState, useRef, useEffect } from 'react'
import { useAuth } from '../contexts/AuthContext'
import { staffChat } from '../services/api'

interface StaffMessage {
  role: 'user' | 'assistant'
  content: string
  lookupType?: string
}

export default function StaffChatPanel() {
  const { user, logout } = useAuth()
  const [messages, setMessages] = useState<StaffMessage[]>([])
  const [input, setInput] = useState('')
  const [loading, setLoading] = useState(false)
  const scrollRef = useRef<HTMLDivElement>(null)
  const sessionId = `staff-${Date.now()}`

  useEffect(() => {
    scrollRef.current?.scrollTo({ top: scrollRef.current.scrollHeight, behavior: 'smooth' })
  }, [messages, loading])

  const handleSend = async (text: string) => {
    if (!text.trim() || loading) return
    setInput('')
    setMessages(prev => [...prev, { role: 'user', content: text }])
    setLoading(true)
    try {
      const res = await staffChat(sessionId, text)
      setMessages(prev => [...prev, {
        role: 'assistant',
        content: res.message,
        lookupType: res.lookupType,
      }])
    } catch {
      setMessages(prev => [...prev, { role: 'assistant', content: 'Lỗi kết nối. Vui lòng thử lại.' }])
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="chat-panel">
      <div className="panel-title">
        💬 Chat nội bộ — {user?.fullName}
        <button className="logout-btn" onClick={logout}>Đăng xuất</button>
      </div>
      <div className="chat-messages" ref={scrollRef}>
        {messages.length === 0 && (
          <div className="chat-empty">
            Trợ lý nội bộ MSB. Hãy nhập số CCCD, SĐT khách hàng, hoặc hỏi về sản phẩm.
          </div>
        )}
        {messages.map((msg, i) => (
          <div key={i} className={`message ${msg.role}`}>
            <div className="message-bubble">{msg.content}</div>
            {msg.lookupType && msg.lookupType !== 'LLM' && (
              <div className="lead-captured-badge">📋 {msg.lookupType}</div>
            )}
          </div>
        ))}
        {loading && (
          <div className="message assistant">
            <div className="message-bubble typing"><span></span><span></span><span></span></div>
          </div>
        )}
      </div>
      <div className="chat-input">
        <input
          type="text"
          value={input}
          onChange={e => setInput(e.target.value)}
          onKeyDown={e => e.key === 'Enter' && handleSend(input)}
          placeholder="Nhập số CCCD, SĐT, hoặc câu hỏi..."
          disabled={loading}
        />
        <button className="send-btn" onClick={() => handleSend(input)} disabled={loading || !input.trim()}>Gửi</button>
      </div>
    </div>
  )
}
