import { useState, useEffect, useMemo } from 'react'
import { getSaleLeads, getLeadMessages, logSaleActivity, deleteLead } from '../../services/api'
import { useToast } from '../../contexts/ToastContext'
import ConfirmModal from '../../components/ConfirmModal'
import type { Lead } from '../../types'

const PAGE_SIZE = 10

export default function SaleUpSale() {
  const { success: toastSuccess, error: toastError } = useToast()
  const [leads, setLeads] = useState<Lead[]>([])
  const [selected, setSelected] = useState<Lead | null>(null)
  const [messages, setMessages] = useState<{ role: string; content: string; createdAt: string }[]>([])
  const [fadeKey, setFadeKey] = useState(0)
  const [acting, setActing] = useState(false)
  const [showDeleteModal, setShowDeleteModal] = useState(false)
  const [page, setPage] = useState(0)
  const [statusFilter, setStatusFilter] = useState('')
  const [search, setSearch] = useState('')

  useEffect(() => { getSaleLeads().then(setLeads).catch(() => {}) }, [])

  const sortedLeads = useMemo(() => {
    let filtered = [...leads]
    if (statusFilter) filtered = filtered.filter(l => l.status === statusFilter)
    if (search.trim()) {
      const q = search.toLowerCase().trim()
      filtered = filtered.filter(l =>
        l.customerName.toLowerCase().includes(q) ||
        l.phone.includes(q) ||
        l.productInterest.toLowerCase().includes(q)
      )
    }
    return filtered.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
  }, [leads, statusFilter, search])

  const totalPages = Math.ceil(sortedLeads.length / PAGE_SIZE)
  const pageLeads = sortedLeads.slice(page * PAGE_SIZE, (page + 1) * PAGE_SIZE)

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

  const handleDelete = async () => {
    if (!selected) return
    setShowDeleteModal(false)
    setActing(true)
    try {
      await deleteLead(selected.id)
      setLeads(prev => prev.filter(l => l.id !== selected.id))
      setSelected(null)
      toastSuccess('Đã xóa lead')
    } catch {
      toastError('Không thể xóa lead')
    } finally {
      setActing(false)
    }
  }

  const statusBadge = (status: string) => (
    <span className={`status ${status.toLowerCase()}`} style={{ fontSize: '11px' }}>{status}</span>
  )

  const fmtTime = (iso: string) => {
    const d = new Date(iso)
    return d.toLocaleDateString('vi-VN') + ' ' + d.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
  }

  const fmtShort = (iso: string) => {
    const d = new Date(iso)
    const now = new Date()
    const diffH = Math.floor((now.getTime() - d.getTime()) / 3600000)
    if (diffH < 1) return `${Math.floor((now.getTime() - d.getTime()) / 60000)} phút trước`
    if (diffH < 24) return `${diffH}h trước`
    return d.toLocaleDateString('vi-VN') + ' ' + d.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })
  }

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
                <div style={{ marginTop: '4px', fontSize: '11px', color: 'var(--console-text-muted)' }}>
                  🕐 Ghi nhận: {fmtTime(selected.createdAt)}
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
                        {msg.role === 'user' ? '👤 Khách hàng' : msg.role === 'system' ? '⚙️ System' : '🤖 AI'} • {new Date(msg.createdAt).toLocaleTimeString('vi-VN')}
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
                <button className="send-btn" style={{ background: 'var(--console-surface)', color: 'var(--status-error)', border: '1px solid var(--status-error)', marginLeft: 'auto' }} onClick={() => setShowDeleteModal(true)} disabled={acting}>
                  🗑 Xóa
                </button>
              </div>
            </div>
          ) : (
            <div style={{ color: 'var(--console-text-muted)', textAlign: 'center', padding: '48px', fontSize: '14px' }}>
              Chọn 1 khách hàng để xem chi tiết
            </div>
          )}
        </div>

        <div style={{ flex: '0 0 280px' }}>
          <div style={{ marginBottom: '12px' }}>
            <input
              type="text"
              placeholder="🔍 Tìm tên, SĐT, sản phẩm..."
              value={search}
              onChange={e => { setSearch(e.target.value); setPage(0) }}
              style={{
                width: '100%', padding: '8px 12px', borderRadius: 'var(--radius-sm)',
                border: '1px solid var(--console-border)', background: 'var(--console-bg)',
                color: 'var(--console-text)', fontSize: '13px', outline: 'none',
              }}
            />
          </div>

          <div style={{ display: 'flex', gap: '4px', marginBottom: '12px', flexWrap: 'wrap' }}>
            {['', 'NEW', 'CONTACTED', 'CONVERTED'].map(s => (
              <button
                key={s}
                onClick={() => { setStatusFilter(s); setPage(0) }}
                style={{
                  padding: '4px 10px', borderRadius: 'var(--radius-sm)', fontSize: '11px',
                  cursor: 'pointer', border: '1px solid var(--console-border)',
                  background: statusFilter === s ? 'var(--console-accent)' : 'var(--console-bg)',
                  color: statusFilter === s ? 'white' : 'var(--console-text-muted)',
                  fontWeight: 600, transition: 'all var(--transition-fast)',
                }}
              >
                {s || 'Tất cả'}
              </button>
            ))}
          </div>

          <div style={{ fontSize: '11px', color: 'var(--console-text-muted)', marginBottom: '8px' }}>
            {sortedLeads.length} khách hàng {search || statusFilter ? '(đã lọc)' : ''}
          </div>

          <div style={{ maxHeight: '480px', overflowY: 'auto' }}>
            {pageLeads.map(lead => (
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
                <div style={{ fontSize: '11px', color: 'var(--console-text-muted)' }}>
                  {lead.phone} | {lead.productInterest}
                </div>
                <div style={{ fontSize: '10px', color: 'var(--console-text-muted)', marginTop: '2px', opacity: 0.7 }}>
                  🕐 {fmtShort(lead.createdAt)}
                </div>
              </div>
            ))}
          </div>

          {totalPages > 1 && (
            <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', gap: '8px', marginTop: '12px' }}>
              <button
                onClick={() => setPage(p => Math.max(0, p - 1))}
                disabled={page === 0}
                style={{
                  padding: '4px 10px', borderRadius: 'var(--radius-sm)', fontSize: '12px',
                  cursor: page === 0 ? 'not-allowed' : 'pointer',
                  border: '1px solid var(--console-border)', background: 'var(--console-bg)',
                  color: page === 0 ? 'var(--console-text-muted)' : 'var(--console-text)',
                  opacity: page === 0 ? 0.5 : 1,
                }}
              >
                ← Trước
              </button>
              <span style={{ fontSize: '12px', color: 'var(--console-text-muted)' }}>
                {page + 1}/{totalPages}
              </span>
              <button
                onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))}
                disabled={page >= totalPages - 1}
                style={{
                  padding: '4px 10px', borderRadius: 'var(--radius-sm)', fontSize: '12px',
                  cursor: page >= totalPages - 1 ? 'not-allowed' : 'pointer',
                  border: '1px solid var(--console-border)', background: 'var(--console-bg)',
                  color: page >= totalPages - 1 ? 'var(--console-text-muted)' : 'var(--console-text)',
                  opacity: page >= totalPages - 1 ? 0.5 : 1,
                }}
              >
                Sau →
              </button>
            </div>
          )}
        </div>
      </div>
      <ConfirmModal
        open={showDeleteModal}
        title="Xác nhận xóa lead"
        message={`Bạn có chắc muốn XÓA lead của ${selected?.customerName}? Dữ liệu sẽ bị xóa vĩnh viễn.`}
        confirmLabel="Xóa vĩnh viễn"
        onConfirm={handleDelete}
        onCancel={() => setShowDeleteModal(false)}
        danger
      />
    </div>
  )
}
