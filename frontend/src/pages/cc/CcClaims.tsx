import { useState, useEffect, useMemo } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../../contexts/AuthContext'
import { useToast } from '../../contexts/ToastContext'
import { getClaims, getClaimMessages, approveClaim, abortClaim, saleLookup } from '../../services/api'
import ChatWidget from '../../components/ChatWidget'
import ConfirmModal from '../../components/ConfirmModal'
import type { ClaimData } from '../../types'

const PAGE_SIZE = 10

export default function CcClaims() {
  const { user, logout } = useAuth()
  const { success: toastSuccess, error: toastError } = useToast()
  const navigate = useNavigate()
  const [claims, setClaims] = useState<ClaimData[]>([])
  const [selected, setSelected] = useState<ClaimData | null>(null)
  const [messages, setMessages] = useState<{ role: string; content: string; createdAt: string }[]>([])
  const [editedResponse, setEditedResponse] = useState('')
  const [filter, setFilter] = useState('')
  const [search, setSearch] = useState('')
  const [acting, setActing] = useState(false)
  const [showAbortModal, setShowAbortModal] = useState(false)
  const [page, setPage] = useState(0)
  const [customerEmail, setCustomerEmail] = useState('')
  const [customerInfo, setCustomerInfo] = useState<{ name: string; phone: string; email: string; idNumber: string } | null>(null)

  const fetchClaims = async () => {
    try { setClaims(await getClaims(filter || undefined)) } catch {}
  }
  useEffect(() => { fetchClaims() }, [filter])

  const sortedClaims = useMemo(() => {
    let filtered = [...claims]
    if (search.trim()) {
      const q = search.toLowerCase().trim()
      filtered = filtered.filter(c =>
        (c.customerName || '').toLowerCase().includes(q) ||
        (c.customerPhone || '').includes(q) ||
        (c.topic || '').toLowerCase().includes(q)
      )
    }
    return filtered.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
  }, [claims, search])

  const totalPages = Math.ceil(sortedClaims.length / PAGE_SIZE)
  const pageClaims = sortedClaims.slice(page * PAGE_SIZE, (page + 1) * PAGE_SIZE)

  const loadDetail = async (claim: ClaimData) => {
    setSelected(claim)
    setEditedResponse(claim.suggestedResponse)
    setCustomerEmail('')
    setCustomerInfo(null)
    try { setMessages(await getClaimMessages(claim.id)) } catch { setMessages([]) }
    if (claim.customerPhone) {
      try {
        const info = await saleLookup(claim.customerPhone) as Record<string, unknown>
        const cust = info.customer as Record<string, unknown> | undefined
        if (cust) {
          setCustomerInfo({
            name: cust.name as string || claim.customerName || '',
            phone: cust.phone as string || claim.customerPhone,
            email: cust.idNumber as string || '',
            idNumber: cust.idNumber as string || '',
          })
          setCustomerEmail(cust.email as string || '')
        }
      } catch {}
    }
  }

  const updateClaimStatus = (claimId: string, newStatus: string) => {
    setClaims(prev => prev.map(c => c.id === claimId ? { ...c, status: newStatus } : c))
    setSelected(prev => prev && prev.id === claimId ? { ...prev, status: newStatus } : prev)
  }

  const handleApprove = async () => {
    if (!selected || acting) return
    setActing(true)
    try {
      await approveClaim(selected.id, editedResponse, customerEmail || undefined)
      updateClaimStatus(selected.id, 'APPROVED')
      toastSuccess(customerEmail ? `Đã duyệt và gửi email đến ${customerEmail}` : 'Đã duyệt (email mock)')
    } catch {
      toastError('Không thể duyệt claim')
    } finally {
      setActing(false)
    }
  }

  const handleAbort = async () => {
    if (!selected) return
    setShowAbortModal(false)
    setActing(true)
    try {
      await abortClaim(selected.id)
      updateClaimStatus(selected.id, 'ABORTED')
      toastSuccess('Đã hủy claim')
    } catch {
      toastError('Không thể hủy claim')
    } finally {
      setActing(false)
    }
  }

  const isResolved = selected && (selected.status === 'APPROVED' || selected.status === 'ABORTED')
  const hasEmail = customerEmail && customerEmail.includes('@')

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
    <div className="app">
      <header className="header">
        <div className="header-left">
          <img src="/assets/logologinchatbot.png" alt="MSB" className="logo-image" />
          <div><h1>Contact Center — Claims</h1><p className="subtitle">{user?.fullName} ({user?.role})</p></div>
        </div>
        <div className="header-right">
          <button className="header-btn" onClick={() => navigate('/')}>← Trang chủ</button>
          <button className="header-btn" onClick={() => { logout(); navigate('/login') }}>Đăng xuất</button>
        </div>
      </header>
      <div style={{ display: 'flex', gap: '20px', padding: '20px', flex: 1 }}>
        <div style={{ flex: '0 0 42%' }}>
          <input
            type="text"
            placeholder="🔍 Tìm tên, SĐT, chủ đề..."
            value={search}
            onChange={e => { setSearch(e.target.value); setPage(0) }}
            style={{ width: '100%', padding: '8px 12px', borderRadius: 'var(--radius-sm)', border: '1px solid var(--console-border)', background: 'var(--console-bg)', color: 'var(--console-text)', fontSize: '13px', outline: 'none', marginBottom: '12px' }}
          />
          <div style={{ display: 'flex', gap: '4px', marginBottom: '12px', flexWrap: 'wrap' }}>
            {['', 'PENDING', 'APPROVED', 'ABORTED'].map(s => (
              <button key={s} onClick={() => { setFilter(s); setPage(0) }} style={{
                padding: '4px 10px', borderRadius: 'var(--radius-sm)', fontSize: '11px', cursor: 'pointer',
                border: '1px solid var(--console-border)',
                background: filter === s ? 'var(--console-accent)' : 'var(--console-bg)',
                color: filter === s ? 'white' : 'var(--console-text-muted)', fontWeight: 600,
              }}>{s || 'Tất cả'}</button>
            ))}
          </div>
          <div style={{ fontSize: '11px', color: 'var(--console-text-muted)', marginBottom: '8px' }}>
            {sortedClaims.length} claims {search || filter ? '(đã lọc)' : ''}
          </div>
          <div style={{ maxHeight: '500px', overflowY: 'auto' }}>
            {pageClaims.map(claim => (
              <div key={claim.id} onClick={() => loadDetail(claim)} style={{
                padding: '12px', borderRadius: 'var(--radius-sm)', cursor: 'pointer', marginBottom: '8px',
                background: selected?.id === claim.id ? 'var(--console-surface)' : 'var(--console-surface-alt)',
                border: selected?.id === claim.id ? '1px solid var(--console-accent)' : '1px solid var(--console-border)',
              }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <span style={{ fontWeight: 600, fontSize: '13px' }}>{claim.customerName || 'N/A'} | {claim.customerPhone || 'N/A'}</span>
                  <span className={`status ${claim.status.toLowerCase()}`} style={{ fontSize: '10px' }}>{claim.status}</span>
                </div>
                <div style={{ fontSize: '12px', color: 'var(--console-text-muted)', marginTop: '2px' }}>{claim.topic}: {(claim.claimContent || '').substring(0, 50)}...</div>
                <div style={{ fontSize: '10px', color: 'var(--console-text-muted)', marginTop: '2px', opacity: 0.7 }}>🕐 {fmtShort(claim.createdAt)}</div>
              </div>
            ))}
          </div>
          {totalPages > 1 && (
            <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', gap: '8px', marginTop: '12px' }}>
              <button onClick={() => setPage(p => Math.max(0, p - 1))} disabled={page === 0} style={{ padding: '4px 10px', borderRadius: 'var(--radius-sm)', fontSize: '12px', cursor: page === 0 ? 'not-allowed' : 'pointer', border: '1px solid var(--console-border)', background: 'var(--console-bg)', color: page === 0 ? 'var(--console-text-muted)' : 'var(--console-text)', opacity: page === 0 ? 0.5 : 1 }}>← Trước</button>
              <span style={{ fontSize: '12px', color: 'var(--console-text-muted)' }}>{page + 1}/{totalPages}</span>
              <button onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))} disabled={page >= totalPages - 1} style={{ padding: '4px 10px', borderRadius: 'var(--radius-sm)', fontSize: '12px',                 cursor: page >= totalPages - 1 ? 'not-allowed' : 'pointer', border: '1px solid var(--console-border)', background: 'var(--console-bg)', color: page >= totalPages - 1 ? 'var(--console-text-muted)' : 'var(--console-text)', opacity: page >= totalPages - 1 ? 0.5 : 1 }}>Sau →</button>
            </div>
          )}
        </div>
        <div style={{ flex: 1 }}>
          {selected ? (
            <>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px' }}>
                <h3 style={{ fontSize: '16px' }}>{selected.topic} — {selected.customerName}</h3>
                <span className={`status ${selected.status.toLowerCase()}`}>{selected.status}</span>
              </div>
              <div style={{ background: 'var(--console-surface-alt)', borderRadius: 'var(--radius-sm)', padding: '12px', marginBottom: '12px', border: '1px solid var(--console-border)' }}>
                <strong style={{ fontSize: '13px' }}>Nội dung claim:</strong> <span style={{ fontSize: '13px' }}>{selected.claimContent}</span>
              </div>
              <div style={{ fontSize: '11px', color: 'var(--console-text-muted)', marginBottom: '12px' }}>
                🕐 Tạo lúc: {fmtTime(selected.createdAt)}
              </div>

              {customerInfo && (
                <div style={{ background: 'var(--console-surface-alt)', borderRadius: 'var(--radius-sm)', padding: '12px', marginBottom: '12px', border: '1px solid var(--console-border)' }}>
                  <strong style={{ fontSize: '13px' }}>👤 Thông tin khách hàng:</strong>
                  <div style={{ fontSize: '13px', marginTop: '6px' }}>
                    <div>Họ tên: {customerInfo.name}</div>
                    <div>SĐT: {customerInfo.phone}</div>
                    {customerInfo.idNumber && <div>CCCD: {customerInfo.idNumber}</div>}
                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginTop: '4px' }}>
                      <span>Email:</span>
                      {hasEmail ? (
                        <span style={{ color: 'var(--status-success)', fontWeight: 600 }}>{customerEmail} ✓</span>
                      ) : (
                        <span style={{ color: 'var(--status-warning)' }}>chưa có — nhập bên dưới</span>
                      )}
                    </div>
                  </div>
                </div>
              )}

              {!isResolved && !hasEmail && (
                <div style={{ background: 'rgba(245,158,11,0.1)', borderRadius: 'var(--radius-sm)', padding: '12px', marginBottom: '12px', border: '1px solid rgba(245,158,11,0.3)' }}>
                  <label style={{ fontSize: '13px', fontWeight: 600, color: 'var(--status-warning)', display: 'block', marginBottom: '6px' }}>
                    ⚠️ Khách chưa có email — nhập email để gửi phản hồi:
                  </label>
                  <input
                    type="email"
                    placeholder="email@khachhang.com"
                    value={customerEmail}
                    onChange={e => setCustomerEmail(e.target.value)}
                    style={{ width: '100%', padding: '8px 12px', borderRadius: 'var(--radius-sm)', border: '1px solid var(--console-border)', background: 'var(--console-bg)', color: 'var(--console-text)', fontSize: '13px', outline: 'none' }}
                  />
                </div>
              )}

              <h4 style={{ fontSize: '13px', marginBottom: '8px' }}>Lịch sử chat gốc:</h4>
              <div style={{ background: 'var(--console-surface)', borderRadius: 'var(--radius-sm)', padding: '12px', maxHeight: '160px', overflowY: 'auto', marginBottom: '12px', border: '1px solid var(--console-border)' }}>
                {messages.map((msg, i) => (
                  <div key={i} className={`message ${msg.role}`} style={{ marginBottom: '6px' }}>
                    <div style={{ fontSize: '11px', color: 'var(--console-text-muted)' }}>{msg.role === 'user' ? '👤' : msg.role === 'system' ? '⚙️' : '🤖'}</div>
                    <div className="message-bubble" style={{ fontSize: '13px' }}>{msg.content}</div>
                  </div>
                ))}
                {messages.length === 0 && <div style={{ color: 'var(--console-text-muted)', fontSize: '13px' }}>Chưa có tin nhắn</div>}
              </div>
              <h4 style={{ fontSize: '13px', marginBottom: '8px' }}>Phản hồi đề xuất (có thể chỉnh sửa):</h4>
              <textarea value={editedResponse} onChange={e => setEditedResponse(e.target.value)} disabled={isResolved !== null && isResolved}
                style={{ width: '100%', minHeight: '80px', padding: '12px', borderRadius: 'var(--radius-sm)', border: '1px solid var(--console-border)', background: 'var(--console-surface)', color: 'var(--console-text)', fontSize: '13px', marginBottom: '12px' }} />
              <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
                {!isResolved && (
                  <>
                    <button className="send-btn" style={{ background: 'var(--status-success)' }} onClick={handleApprove} disabled={acting || (!hasEmail && !customerEmail)}>
                      {acting ? 'Đang xử lý...' : hasEmail ? '✓ Approve & Send Email' : customerEmail ? '✓ Approve & Send Email' : '✓ Approve (cần email)'}
                    </button>
                    <button className="send-btn" style={{ background: 'var(--status-error)' }} onClick={() => setShowAbortModal(true)} disabled={acting}>
                      ✗ Abort
                    </button>
                  </>
                )}
                {selected.status === 'APPROVED' && <span style={{ color: 'var(--status-success)', fontWeight: 600, fontSize: '14px' }}>✓ Đã duyệt</span>}
                {selected.status === 'ABORTED' && <span style={{ color: 'var(--status-error)', fontWeight: 600, fontSize: '14px' }}>✗ Đã hủy</span>}
              </div>
            </>
          ) : (
            <div style={{ color: 'var(--console-text-muted)', textAlign: 'center', padding: '40px' }}>Chọn 1 claim để xem chi tiết</div>
          )}
        </div>
      </div>
      <ConfirmModal
        open={showAbortModal}
        title="Xác nhận hủy claim"
        message={`Bạn có chắc muốn hủy claim của ${selected?.customerName}? Hành động này không thể hoàn tác.`}
        confirmLabel="Hủy claim"
        onConfirm={handleAbort}
        onCancel={() => setShowAbortModal(false)}
        danger
      />
      <ChatWidget sessionId={`cc-${user?.fullName?.replace(/\s/g,'')}-${Date.now()}`} mode="staff" />
    </div>
  )
}
