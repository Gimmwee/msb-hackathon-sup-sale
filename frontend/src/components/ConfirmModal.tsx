interface Props {
  open: boolean
  title: string
  message: string
  confirmLabel: string
  onConfirm: () => void
  onCancel: () => void
  danger?: boolean
}

export default function ConfirmModal({ open, title, message, confirmLabel, onConfirm, onCancel, danger }: Props) {
  if (!open) return null

  return (
    <div onClick={onCancel} style={{
      position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.4)', zIndex: 10001,
      display: 'flex', alignItems: 'center', justifyContent: 'center',
    }}>
      <div onClick={e => e.stopPropagation()} style={{
        background: 'var(--console-surface)', borderRadius: 'var(--radius-lg)',
        padding: '24px', width: '380px', boxShadow: '0 12px 40px rgba(0,0,0,0.2)',
        animation: 'modalIn 200ms ease',
      }}>
        <h3 style={{ fontSize: '16px', marginBottom: '8px', color: 'var(--console-text)' }}>{title}</h3>
        <p style={{ fontSize: '14px', color: 'var(--console-text-muted)', marginBottom: '20px' }}>{message}</p>
        <div style={{ display: 'flex', gap: '8px', justifyContent: 'flex-end' }}>
          <button onClick={onCancel} style={{
            background: 'transparent', color: 'var(--console-text-muted)',
            border: '1px solid var(--console-border)', borderRadius: 'var(--radius-sm)',
            padding: '8px 16px', fontSize: '14px', cursor: 'pointer',
          }}>Huỷ</button>
          <button onClick={onConfirm} style={{
            background: danger ? 'var(--status-error)' : 'var(--console-accent)',
            color: 'white', border: 'none', borderRadius: 'var(--radius-sm)',
            padding: '8px 16px', fontSize: '14px', fontWeight: 600, cursor: 'pointer',
          }}>{confirmLabel}</button>
        </div>
      </div>
    </div>
  )
}
