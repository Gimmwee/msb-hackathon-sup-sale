import { useState, useEffect } from 'react'
import { getSaleLeads, getLeadMessages, logSaleActivity } from '../../services/api'
import { useToast } from '../../contexts/ToastContext'
import type { Lead } from '../../types'

export default function SaleUpSale() {
  const { success: toastSuccess, error: toastError } = useToast()
  const [leads, setLeads] = useState<Lead[]>([])
  const [selected, setSelected] = useState<Lead | null>(null)
  const [messages, setMessages] = useState<{ role: string; content: string; createdAt: string }[]>([])
  const [fadeKey, setFadeKey] = useState(0)
  const [acting, setActing] = useState(false)

  useEffect(() => { getSaleLeads().then(setLeads).catch(() => {}) }, [])

  const loadMessages = async (lead: Lead) => {
    setSelected(lead)
    setFadeKey(k => k + 1)
    try { setMessages(await getLeadMessages(lead.sessionId)) } catch { setMessages([]) }
  }

  const updateLeadStatus = (leadId: string, newStatus: string) => {
    setLeads(prev => prev.map(l => l.id === leadId ? { ...l, status: newStatus } : l))
    setSelected(prev => prev && prev.id === leadId ? { ...prev, status: newStatus } : prev)
  }

  const handleAction = async (action: 'CONTACTED' | 'CONVERTED') => {
    if (!selected || acting) return
    setActing(true)
    try {
      await logSaleActivity(selected.id, action, '')
      const newStatus = action === 'CONTACTED' ? 'CONTACTED' : 'CONVERTED'
      updateLeadStatus(selected.id, newStatus)
      toastSuccess(`Đã ghi: ${action === 'CONTACTED' ? 'Đã liên hệ' : 'Đã chuyển đổi'}`)
    } catch {
      toastError('Không thể ghi nhận hoạt động')
    } finally {
      setActing(false)
    }
  }

  const statusBadge = (status: string) => (
    <span className={`status ${status.toLowerCase()}`} style={{ fontSize: '11px' }}>{status}</span>
  )

  return (
    <div>
      <h2 style={{ fontSize: '18px', marginBottom: '16px' }}>Data UpSale</h2>
      <div style={{ display: 'flex', gap: '20px' }}>
        <div style={{ flex: 1 }}>
          {selected ? (
            <div key={fadeKey} className="crossfade">
              <div style={{
                background: 'var(--console-bg)', border: '1px solid var(--console-border)',
                borderRadius: 'var(--radius-md)', padding: '14px 16px', marginBottom: '12px',
              }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <div>
                    <span style={{ fontSize: '16px', fontWeight: 600 }}>{selected.customerName}</span>
                    <span style={{ marginLeft: '12px', fontSize: '13px', color: 'var(--console-text-muted)' }}>{selected.phone}</span>
                  </div>
                  {statusBadge(selected.status)}
                </div>
                <div style={{ marginTop: '6px', fontSize: '13px', color: 'var(--console-text-muted)' }}>
                  Nhu cầu: {selected.productInterest}
                </div>
              </div>

              <div style={{
                background: 'var(--console-surface)', border: '1px solid var(--console-border)',
                borderRadius: 'var(--radius-md)', padding: '16px', maxHeight: '380px', overflowY: 'auto',
              }}>
                {messages.length > 0 ? (
                  messages.map((msg, i) => (
                    <div key={i} className={`message ${msg.role}`} style={{ marginBottom: '8px' }}>
                      <div style={{ fontSize: '11px', color: 'var(--console-text-muted)', marginBottom: '2px' }}>
                        {msg.role === 'user' ? '👤 Khách hàng' : '🤖 AI'} • {new Date(msg.createdAt).toLocaleTimeString('vi-VN')}
                      </div>
                      <div className="message-bubble" style={{ fontSize: '13px' }}>{msg.content}</div>
                    </div>
                  ))
                ) : (
                  <div style={{ color: 'var(--console-text-muted)', textAlign: 'center', padding: '32px', fontSize: '14px' }}>
                    Khách hàng này chưa tương tác với chatbot
                  </div>
                )}
              </div>

              <div style={{ marginTop: '12px', display: 'flex', gap: '8px', alignItems: 'center' }}>
                {selected.status === 'NEW' && (
                  <>
                    <button className="send-btn" onClick={() => handleAction('CONTACTED')} disabled={acting}>
                      {acting ? 'Đang lưu...' : 'Đã liên hệ'}
                    </button>
                    <button className="send-btn" style={{ background: 'var(--status-success)' }} onClick={() => handleAction('CONVERTED')} disabled={acting}>
                      {acting ? 'Đang lưu...' : 'Đã chuyển đổi'}
                    </button>
                  </>
                )}
                {selected.status === 'CONTACTED' && (
                  <>
                    <span style={{ fontSize: '13px', color: 'var(--status-success)', fontWeight: 600 }}>✓ Đã liên hệ</span>
                    <button className="send-btn" style={{ background: 'var(--status-success)' }} onClick={() => handleAction('CONVERTED')} disabled={acting}>
                      {acting ? 'Đang lưu...' : 'Đã chuyển đổi'}
                    </button>
                  </>
                )}
                {selected.status === 'CONVERTED' && (
                  <span style={{ fontSize: '13px', color: 'var(--status-success)', fontWeight: 600 }}>✓ Đã chuyển đổi</span>
                )}
              </div>
            </div>
          ) : (
            <div style={{ color: 'var(--console-text-muted)', textAlign: 'center', padding: '48px', fontSize: '14px' }}>
              Chọn 1 khách hàng để xem chi tiết
            </div>
          )}
        </div>

        <div style={{ flex: '0 0 260px' }}>
          <h4 style={{ fontSize: '13px', marginBottom: '10px', color: 'var(--console-text-muted)' }}>Khách hàng gần đây</h4>
          {leads.map(lead => (
            <div key={lead.id} onClick={() => loadMessages(lead)} style={{
              padding: '10px 12px', borderRadius: 'var(--radius-sm)', cursor: 'pointer', marginBottom: '4px',
              background: selected?.id === lead.id ? 'var(--console-surface)' : 'transparent',
              borderLeft: selected?.id === lead.id ? '3px solid var(--console-accent)' : '3px solid transparent',
              transition: 'all var(--transition-fast)',
            }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <span style={{ fontWeight: 600, fontSize: '13px' }}>{lead.customerName}</span>
                {statusBadge(lead.status)}
              </div>
              <div style={{ fontSize: '11px', color: 'var(--console-text-muted)' }}>{lead.phone} | {lead.productInterest}</div>
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}
