import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../../contexts/AuthContext'
import { useToast } from '../../contexts/ToastContext'
import { getClaims, getClaimMessages, approveClaim, abortClaim } from '../../services/api'
import ChatWidget from '../../components/ChatWidget'
import ConfirmModal from '../../components/ConfirmModal'

interface ClaimData {
  id: string; sessionId: string; customerName: string; customerPhone: string;
  topic: string; claimContent: string; suggestedResponse: string; status: string; createdAt: string;
}

export default function CcClaims() {
  const { user, logout } = useAuth()
  const { success: toastSuccess, error: toastError } = useToast()
  const navigate = useNavigate()
  const [claims, setClaims] = useState<ClaimData[]>([])
  const [selected, setSelected] = useState<ClaimData | null>(null)
  const [messages, setMessages] = useState<{ role: string; content: string; createdAt: string }[]>([])
  const [editedResponse, setEditedResponse] = useState('')
  const [filter, setFilter] = useState('')
  const [acting, setActing] = useState(false)
  const [showAbortModal, setShowAbortModal] = useState(false)

  const fetchClaims = async () => {
    try { setClaims(await getClaims(filter || undefined) as ClaimData[]) } catch {}
  }
  useEffect(() => { fetchClaims() }, [filter])

  const loadDetail = async (claim: ClaimData) => {
    setSelected(claim); setEditedResponse(claim.suggestedResponse)
    try { setMessages(await getClaimMessages(claim.id)) } catch { setMessages([]) }
  }

  const updateClaimStatus = (claimId: string, newStatus: string) => {
    setClaims(prev => prev.map(c => c.id === claimId ? { ...c, status: newStatus } : c))
    setSelected(prev => prev && prev.id === claimId ? { ...prev, status: newStatus } : prev)
  }

  const handleApprove = async () => {
    if (!selected || acting) return
    setActing(true)
    try {
      await approveClaim(selected.id, editedResponse)
      updateClaimStatus(selected.id, 'APPROVED')
      toastSuccess('Đã duyệt và gửi email phản hồi')
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

  return (
    <div className="app">
      <header className="header">
        <div className="header-left">
          <div className="logo">M</div>
          <div><h1>Contact Center — Claims</h1><p className="subtitle">{user?.fullName} ({user?.role})</p></div>
        </div>
        <div className="header-right">
          <button className="header-btn" onClick={() => navigate('/')}>← Trang chủ</button>
          <button className="header-btn" onClick={() => { logout(); navigate('/login') }}>Đăng xuất</button>
        </div>
      </header>
      <div style={{ display: 'flex', gap: '20px', padding: '20px', flex: 1 }}>
        <div style={{ flex: '0 0 45%' }}>
          <div style={{ display: 'flex', gap: '8px', marginBottom: '12px' }}>
            <select value={filter} onChange={e => setFilter(e.target.value)} style={{ padding: '8px', borderRadius: '8px', border: '1px solid var(--console-border)', background: 'var(--console-surface)', color: 'var(--console-text)' }}>
              <option value="">Tất cả</option>
              <option value="PENDING">PENDING</option>
              <option value="APPROVED">APPROVED</option>
              <option value="ABORTED">ABORTED</option>
            </select>
          </div>
          {claims.map(claim => (
            <div key={claim.id} onClick={() => loadDetail(claim)} style={{
              padding: '12px', borderRadius: 'var(--radius-sm)', cursor: 'pointer', marginBottom: '8px',
              background: selected?.id === claim.id ? 'var(--console-surface)' : 'var(--console-surface-alt)',
              border: selected?.id === claim.id ? '1px solid var(--console-accent)' : '1px solid var(--console-border)',
            }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <span style={{ fontWeight: 600, fontSize: '13px' }}>{claim.customerName} | {claim.customerPhone}</span>
                <span className={`status ${claim.status.toLowerCase()}`} style={{ fontSize: '10px' }}>{claim.status}</span>
              </div>
              <div style={{ fontSize: '12px', color: 'var(--console-text-muted)', marginTop: '2px' }}>{claim.topic}: {claim.claimContent.substring(0, 60)}...</div>
            </div>
          ))}
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
              <h4 style={{ fontSize: '13px', marginBottom: '8px' }}>Lịch sử chat gốc:</h4>
              <div style={{ background: 'var(--console-surface)', borderRadius: 'var(--radius-sm)', padding: '12px', maxHeight: '160px', overflowY: 'auto', marginBottom: '12px', border: '1px solid var(--console-border)' }}>
                {messages.map((msg, i) => (
                  <div key={i} className={`message ${msg.role}`} style={{ marginBottom: '6px' }}>
                    <div style={{ fontSize: '11px', color: 'var(--console-text-muted)' }}>{msg.role === 'user' ? '👤' : '🤖'}</div>
                    <div className="message-bubble" style={{ fontSize: '13px' }}>{msg.content}</div>
                  </div>
                ))}
                {messages.length === 0 && <div style={{ color: 'var(--console-text-muted)', fontSize: '13px' }}>Chưa có tin nhắn</div>}
              </div>
              <h4 style={{ fontSize: '13px', marginBottom: '8px' }}>Phản hồi đề xuất (có thể chỉnh sửa):</h4>
              <textarea value={editedResponse} onChange={e => setEditedResponse(e.target.value)} disabled={isResolved !== null && isResolved}
                style={{ width: '100%', minHeight: '80px', padding: '12px', borderRadius: 'var(--radius-sm)', border: '1px solid var(--console-border)', background: 'var(--console-surface)', color: 'var(--console-text)', fontSize: '13px', marginBottom: '12px' }} />
              <div style={{ display: 'flex', gap: '8px' }}>
                {!isResolved && (
                  <>
                    <button className="send-btn" style={{ background: 'var(--status-success)' }} onClick={handleApprove} disabled={acting}>
                      {acting ? 'Đang xử lý...' : '✓ Approve & Send'}
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
