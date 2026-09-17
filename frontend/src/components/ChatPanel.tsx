import { useState, useRef, useEffect } from 'react'
import { sendChat, uploadCccd } from '../services/api'
import type { ChatMessage } from '../types'

interface Props {
  sessionId: string
  onLeadCaptured: () => void
}

const QUICK_BUTTONS = [
  'Chào shop, mình muốn vay mua ô tô',
  'Mình muốn vay tín chấp',
  'Mình muốn mở thẻ tín dụng',
]

export default function ChatPanel({ sessionId, onLeadCaptured }: Props) {
  const [messages, setMessages] = useState<ChatMessage[]>([])
  const [input, setInput] = useState('')
  const [loading, setLoading] = useState(false)
  const [customerPhone, setCustomerPhone] = useState('')
  const scrollRef = useRef<HTMLDivElement>(null)
  const fileInputRef = useRef<HTMLInputElement>(null)

  useEffect(() => {
    scrollRef.current?.scrollTo({ top: scrollRef.current.scrollHeight, behavior: 'smooth' })
  }, [messages, loading])

  const handleSend = async (text: string) => {
    if (!text.trim() || loading) return
    setInput('')
    setMessages(prev => [...prev, { role: 'user', content: text }])
    setLoading(true)
    try {
      const res = await sendChat({ sessionId, platform: 'WEB', message: text })
      setMessages(prev => [...prev, {
        role: 'assistant',
        content: res.message,
        leadCaptured: res.leadCaptured,
        intent: res.intent,
      }])
      if (res.leadCaptured) onLeadCaptured()
      const phoneMatch = text.match(/(0|\+84)[3-9][0-9]{8}/)
      if (phoneMatch) setCustomerPhone(phoneMatch[0])
    } catch {
      setMessages(prev => [...prev, { role: 'assistant', content: 'Xin lỗi, đã có lỗi xảy ra. Vui lòng thử lại.' }])
    } finally {
      setLoading(false)
    }
  }

  const handleImageUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (!file || loading) return
    e.target.value = ''

    if (file.size > 5 * 1024 * 1024) {
      setMessages(prev => [...prev, {
        role: 'assistant',
        content: `Ảnh quá lớn (${(file.size / 1024 / 1024).toFixed(1)}MB). Vui lòng gửi ảnh nhỏ hơn 5MB ạ.`,
      }])
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
      if (cccd.fullName && customerPhone) onLeadCaptured()
    } catch (err) {
      const errMsg = err instanceof Error ? err.message : String(err)
      setMessages(prev => [...prev, { role: 'assistant', content: `Lỗi OCR: ${errMsg}` }])
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="chat-panel">
      <div className="panel-title">💬 Chat với sup-sale</div>
      <div className="chat-messages" ref={scrollRef}>
        {messages.length === 0 && (
          <div className="chat-empty">Chào bạn! Em là tư vấn viên MSB. Em có thể giúp gì cho bạn hôm nay?</div>
        )}
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
      <div className="quick-buttons">
        {QUICK_BUTTONS.map(btn => (
          <button key={btn} className="quick-btn" onClick={() => handleSend(btn)} disabled={loading}>{btn}</button>
        ))}
      </div>
      <div className="chat-input">
        <input type="file" ref={fileInputRef} onChange={handleImageUpload} accept="image/*" style={{ display: 'none' }} />
        <button className="upload-btn" onClick={() => fileInputRef.current?.click()} disabled={loading} title="Tải lên CCCD">📎</button>
        <input
          type="text"
          value={input}
          onChange={e => setInput(e.target.value)}
          onKeyDown={e => e.key === 'Enter' && handleSend(input)}
          placeholder="Nhập tin nhắn..."
          disabled={loading}
        />
        <button className="send-btn" onClick={() => handleSend(input)} disabled={loading || !input.trim()}>Gửi</button>
      </div>
    </div>
  )
}

async function compressImage(file: File, maxWidth = 1024, quality = 0.7): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => {
      const img = new Image()
      img.onload = () => {
        const scale = Math.min(1, maxWidth / img.width)
        const canvas = document.createElement('canvas')
        canvas.width = img.width * scale
        canvas.height = img.height * scale
        const ctx = canvas.getContext('2d')!
        ctx.drawImage(img, 0, 0, canvas.width, canvas.height)
        resolve(canvas.toDataURL('image/jpeg', quality))
      }
      img.onerror = reject
      img.src = reader.result as string
    }
    reader.onerror = reject
    reader.readAsDataURL(file)
  })
}
