import { createContext, useContext, useState, useCallback, type ReactNode } from 'react'

type ToastType = 'success' | 'error' | 'info'

interface ToastItem {
  id: number
  type: ToastType
  message: string
}

interface ToastContextType {
  success: (msg: string) => void
  error: (msg: string) => void
  info: (msg: string) => void
}

const ToastContext = createContext<ToastContextType | null>(null)

let toastId = 0

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<ToastItem[]>([])

  const remove = useCallback((id: number) => {
    setToasts(prev => prev.filter(t => t.id !== id))
  }, [])

  const add = useCallback((type: ToastType, message: string) => {
    const id = ++toastId
    setToasts(prev => [...prev, { id, type, message }])
    setTimeout(() => remove(id), 3000)
  }, [remove])

  const success = useCallback((msg: string) => add('success', msg), [add])
  const error = useCallback((msg: string) => add('error', msg), [add])
  const info = useCallback((msg: string) => add('info', msg), [add])

  const colors: Record<ToastType, string> = {
    success: 'var(--status-success)',
    error: 'var(--status-error)',
    info: 'var(--status-info)',
  }

  return (
    <ToastContext.Provider value={{ success, error, info }}>
      {children}
      <div style={{
        position: 'fixed', top: '16px', right: '16px', zIndex: 10000,
        display: 'flex', flexDirection: 'column', gap: '8px', pointerEvents: 'none',
      }}>
        {toasts.map(t => (
          <div key={t.id} style={{
            background: 'var(--console-surface)', color: 'var(--console-text)',
            border: `1px solid ${colors[t.type]}`, borderLeft: `4px solid ${colors[t.type]}`,
            borderRadius: 'var(--radius-md)', padding: '12px 16px', fontSize: '14px',
            boxShadow: '0 4px 16px rgba(0,0,0,0.12)', maxWidth: '360px',
            animation: 'toastIn 200ms ease', pointerEvents: 'auto',
          }}>
            <span style={{ color: colors[t.type], fontWeight: 600, marginRight: '6px' }}>
              {t.type === 'success' ? '✓' : t.type === 'error' ? '✗' : 'ℹ'}
            </span>
            {t.message}
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  )
}

export function useToast() {
  const ctx = useContext(ToastContext)
  if (!ctx) throw new Error('useToast must be used within ToastProvider')
  return ctx
}
