import { useState, useRef, useEffect } from 'react'
import { sendChat, uploadCccd, staffChat } from '../services/api'
import type { ChatMessage } from '../types'

interface Props {
  sessionId: string
  onLeadCaptured?: () => void
  mode?: 'customer' | 'staff'
}

const CUSTOMER_QUICK = [
  'Tôi muốn vay mua ô tô',
  'Tôi muốn vay tín chấp',
  'Tôi muốn mở thẻ tín dụng',
]

const STAFF_QUICK: string[] = []

export default function ChatWidget({ sessionId, onLeadCaptured, mode = 'customer' }: Props) {
  const [open, setOpen] = useState(false)
  const [messages, setMessages] = useState<ChatMessage[]>([])
  const [input, setInput] = useState('')
  const [loading, setLoading] = useState(false)
  const [customerPhone, setCustomerPhone] = useState('')
  const scrollRef = useRef<HTMLDivElement>(null)
  const fileInputRef = useRef<HTMLInputElement>(null)
  const isStaff = mode === 'staff'

  useEffect(() => {
    scrollRef.current?.scrollTo({ top: scrollRef.current.scrollHeight, behavior: 'smooth' })
  }, [messages, loading])

  const handleSend = async (text: string) => {
    if (!text.trim() || loading) return
    setInput('')
    setMessages(prev => [...prev, { role: 'user', content: text }])
    setLoading(true)
    try {
      if (isStaff) {
        const res = await staffChat(sessionId, text)
        setMessages(prev => [...prev, { role: 'assistant', content: res.message }])
      } else {
        const res = await sendChat({ sessionId, platform: 'WEB', message: text })
        setMessages(prev => [...prev, {
          role: 'assistant', content: res.message,
          leadCaptured: res.leadCaptured, intent: res.intent,
        }])
        if (res.leadCaptured) onLeadCaptured?.()
        const phoneMatch = text.match(/(0|\+84)[3-9][0-9]{8}/)
        if (phoneMatch) setCustomerPhone(phoneMatch[0])
      }
    } catch {
      setMessages(prev => [...prev, { role: 'assistant', content: 'Xin lỗi, đã có lỗi xảy ra.' }])
    } finally {
      setLoading(false)
    }
  }

  const handleImageUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    if (isStaff) return
    const file = e.target.files?.[0]
    if (!file || loading) return
    e.target.value = ''
    if (file.size > 5 * 1024 * 1024) {
      setMessages(prev => [...prev, { role: 'assistant', content: `Ảnh quá lớn (${(file.size / 1024 / 1024).toFixed(1)}MB). Vui lòng gửi ảnh nhỏ hơn 5MB.` }])
      return
    }
    setMessages(prev => [...prev, { role: 'user', content: `📎 Đã tải lên ảnh CCCD (${(file.size / 1024).toFixed(0)}KB)` }])
    setLoading(true)
    try {
      const cccd = await uploadCccd(file, sessionId, customerPhone)
      const infoLines = [
        cccd.fullName && `Họ tên: ${cccd.fullName}`,
        cccd.idNumber && `Số CCCD: ${cccd.idNumber}`,
        cccd.dob && `Ngày sinh: ${cccd.dob}`,
        cccd.gender && `Giới tính: ${cccd.gender}`,
        cccd.address && `Địa chỉ: ${cccd.address}`,
      ].filter(Boolean)
      setMessages(prev => [...prev, {
        role: 'assistant',
        content: cccd.message + (infoLines.length > 0 ? '\n\n' + infoLines.join('\n') : ''),
        cccdInfo: cccd,
      }])
      if (cccd.fullName && customerPhone) onLeadCaptured?.()
    } catch (err) {
      const errMsg = err instanceof Error ? err.message : String(err)
      setMessages(prev => [...prev, { role: 'assistant', content: `Lỗi OCR: ${errMsg}` }])
    } finally {
      setLoading(false)
    }
  }

  const quickButtons = isStaff ? STAFF_QUICK : CUSTOMER_QUICK
  const headerLabel = isStaff ? '🛠️ Trợ lý nội bộ' : '💬 sup-sale — Tư vấn MSB'
  const emptyMsg = isStaff
    ? 'Trợ lý nội bộ MSB. Nhập số CCCD, SĐT khách hàng, hoặc hỏi về sản phẩm.'
    : 'Chào bạn! Em là tư vấn viên MSB. Em có thể giúp gì cho bạn?'
  const placeholder = isStaff ? 'Nhập số CCCD, SĐT, hoặc câu hỏi...' : 'Nhập tin nhắn...'

  if (!open) {
    return (
      <button className="chat-fab" onClick={() => setOpen(true)} title={isStaff ? 'Trợ lý nội bộ' : 'Chat với sup-sale'}>
        {isStaff ? '🛠️' : '💬'}
      </button>
    )
  }

  return (
    <div className="chat-widget">
      <div className="chat-widget-header">
        <span>{headerLabel}</span>
        <button className="chat-widget-close" onClick={() => setOpen(false)}>✕</button>
      </div>
      <div className="chat-messages" ref={scrollRef}>
        {messages.length === 0 && <div className="chat-empty">{emptyMsg}</div>}
        {messages.map((msg, i) => (
          <div key={i} className={`message ${msg.role}`}>
            <div className="message-bubble">{msg.content}</div>
            {msg.leadCaptured && <div className="lead-captured-badge">✓ Lead captured</div>}
            {msg.cccdInfo?.saved && <div className="lead-captured-badge">✓ CCCD saved</div>}
          </div>
        ))}
        {loading && (
          <div className="message assistant">
            <div className="message-bubble typing"><span></span><span></span><span></span></div>
          </div>
        )}
      </div>
      {quickButtons.length > 0 && (
        <div className="quick-buttons">
          {quickButtons.map(btn => (
            <button key={btn} className="quick-btn" onClick={() => handleSend(btn)} disabled={loading}>{btn}</button>
          ))}
        </div>
      )}
      <div className="chat-input">
        {!isStaff && <input type="file" ref={fileInputRef} onChange={handleImageUpload} accept="image/*" style={{ display: 'none' }} />}
        {!isStaff && <button className="upload-btn" onClick={() => fileInputRef.current?.click()} disabled={loading} title="CCCD">📎</button>}
        <input type="text" value={input} onChange={e => setInput(e.target.value)}
          onKeyDown={e => e.key === 'Enter' && handleSend(input)} placeholder={placeholder} disabled={loading} />
        <button className="send-btn" onClick={() => handleSend(input)} disabled={loading || !input.trim()}>Gửi</button>
      </div>
    </div>
  )
}
