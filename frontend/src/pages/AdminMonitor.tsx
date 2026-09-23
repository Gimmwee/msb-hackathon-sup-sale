import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import { getMonitorStatus } from '../services/api'

interface MonitorData {
  timestamp: string
  backend: { status: string; uptime: string; heapUsed: number; heapMax: number }
  database: { status: string; type?: string; error?: string; tables?: Record<string, number> }
  agent: { status: string; model: string; mockMode: boolean; apiConfigured: boolean }
  frontend: { status: string; servedFrom: string }
}

export default function AdminMonitor() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const [data, setData] = useState<MonitorData | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const fetchMonitor = async () => {
    setLoading(true)
    try {
      setData(await getMonitorStatus() as unknown as MonitorData)
      setError('')
    } catch {
      setError('Không tải được trạng thái hệ thống')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchMonitor()
    const interval = setInterval(fetchMonitor, 10000)
    return () => clearInterval(interval)
  }, [])

  const StatusBadge = ({ status }: { status: string }) => {
    const isUp = status === 'UP' || status === 'ACTIVE'
    const isMock = status === 'MOCK'
    const color = isUp ? '#22c55e' : isMock ? '#f59e0b' : '#ef4444'
    const bg = isUp ? 'rgba(34,197,94,0.15)' : isMock ? 'rgba(245,158,11,0.15)' : 'rgba(239,68,68,0.15)'
    return (
      <span style={{
        display: 'inline-flex', alignItems: 'center', gap: '6px',
        background: bg, color, padding: '4px 12px', borderRadius: '20px',
        fontSize: '12px', fontWeight: 600,
      }}>
        <span style={{ width: '8px', height: '8px', borderRadius: '50%', background: color, animation: 'pulse 2s infinite' }} />
        {status}
      </span>
    )
  }

  const ServiceCard = ({ title, icon, status, children }: { title: string; icon: string; status: string; children?: React.ReactNode }) => (
    <div style={{
      background: 'var(--console-surface)', border: '1px solid var(--console-border)',
      borderRadius: 'var(--radius-lg)', padding: '20px', flex: 1, minWidth: '280px',
    }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
          <span style={{ fontSize: '24px' }}>{icon}</span>
          <h3 style={{ fontSize: '16px', fontWeight: 600 }}>{title}</h3>
        </div>
        <StatusBadge status={status} />
      </div>
      {children}
    </div>
  )

  const InfoRow = ({ label, value }: { label: string; value: string | number }) => (
    <div style={{ display: 'flex', justifyContent: 'space-between', padding: '6px 0', borderBottom: '1px solid var(--console-border)', fontSize: '13px' }}>
      <span style={{ color: 'var(--console-text-muted)' }}>{label}</span>
      <span style={{ fontWeight: 500 }}>{value}</span>
    </div>
  )

  const formatBytes = (bytes: number) => {
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(0)} KB`
    if (bytes < 1024 * 1024 * 1024) return `${(bytes / 1024 / 1024).toFixed(1)} MB`
    return `${(bytes / 1024 / 1024 / 1024).toFixed(2)} GB`
  }

  return (
    <div className="app">
      <header className="header">
        <div className="header-left">
          <img src="/assets/logologinchatbot.png" alt="MSB" className="logo-image" />
          <div><h1>System Monitor</h1><p className="subtitle">{user?.fullName} (ADMIN)</p></div>
        </div>
        <div className="header-right">
          <button className="header-btn" onClick={() => navigate('/admin/users')}>Users</button>
          <button className="header-btn" onClick={() => navigate('/sale/upsale')}>Sale</button>
          <button className="header-btn" onClick={() => navigate('/')}>Home</button>
          <button className="header-btn" onClick={() => { logout(); navigate('/login') }}>Logout</button>
        </div>
      </header>

      <div style={{ padding: '24px', maxWidth: '1200px', margin: '0 auto' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
          <h2 style={{ fontSize: '20px', fontWeight: 700 }}>System Health Dashboard</h2>
          <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
            <span style={{ fontSize: '12px', color: 'var(--console-text-muted)' }}>
              {data ? `Last updated: ${new Date(data.timestamp).toLocaleTimeString('vi-VN')}` : ''}
            </span>
            <button className="header-btn" onClick={fetchMonitor} disabled={loading}>
              {loading ? 'Loading...' : 'Refresh'}
            </button>
          </div>
        </div>

        {error && <div className="error-msg">{error}</div>}

        {data && (
          <>
            <div style={{ display: 'flex', gap: '20px', flexWrap: 'wrap', marginBottom: '20px' }}>
              <ServiceCard title="Backend (API)" icon="⚙️" status={data.backend.status}>
                <InfoRow label="Uptime" value={data.backend.uptime} />
                <InfoRow label="Heap Used" value={formatBytes(data.backend.heapUsed)} />
                <InfoRow label="Heap Max" value={formatBytes(data.backend.heapMax)} />
                <InfoRow label="Heap Usage" value={`${((data.backend.heapUsed / data.backend.heapMax) * 100).toFixed(1)}%`} />
              </ServiceCard>

              <ServiceCard title="Database" icon="🗄️" status={data.database.status}>
                {data.database.error ? (
                  <div style={{ color: 'var(--status-error)', fontSize: '13px' }}>{data.database.error}</div>
                ) : (
                  <>
                    <InfoRow label="Type" value={data.database.type || 'Unknown'} />
                    {data.database.tables && Object.entries(data.database.tables).map(([table, count]) => (
                      <InfoRow key={table} label={table} value={count} />
                    ))}
                  </>
                )}
              </ServiceCard>

              <ServiceCard title="AI Agent" icon="🤖" status={data.agent.status}>
                <InfoRow label="Model" value={data.agent.model} />
                <InfoRow label="Mock Mode" value={data.agent.mockMode ? 'Yes' : 'No'} />
                <InfoRow label="API Key" value={data.agent.apiConfigured ? 'Configured' : 'Missing'} />
              </ServiceCard>

              <ServiceCard title="Frontend" icon="🖥️" status={data.frontend.status}>
                <InfoRow label="Served From" value={data.frontend.servedFrom} />
                <InfoRow label="Build" value="Embedded in JAR" />
              </ServiceCard>
            </div>

            <div style={{
              background: 'var(--console-surface)', border: '1px solid var(--console-border)',
              borderRadius: 'var(--radius-lg)', padding: '20px',
            }}>
              <h3 style={{ fontSize: '15px', fontWeight: 600, marginBottom: '12px' }}>Database Tables Overview</h3>
              {data.database.tables && (
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(200px, 1fr))', gap: '12px' }}>
                  {Object.entries(data.database.tables).map(([table, count]) => (
                    <div key={table} style={{
                      background: 'var(--console-bg)', borderRadius: 'var(--radius-md)',
                      padding: '12px 16px', border: '1px solid var(--console-border)',
                    }}>
                      <div style={{ fontSize: '11px', color: 'var(--console-text-muted)', marginBottom: '4px' }}>{table}</div>
                      <div style={{ fontSize: '22px', fontWeight: 700, color: 'var(--console-accent)' }}>{count}</div>
                      <div style={{
                        height: '4px', background: 'var(--console-border)', borderRadius: '2px',
                        marginTop: '8px', overflow: 'hidden',
                      }}>
                        <div style={{
                          height: '100%', background: 'var(--console-accent)',
                          width: `${Math.min((count / Math.max(...Object.values(data.database.tables!))) * 100, 100)}%`,
                          borderRadius: '2px',
                        }} />
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </>
        )}
      </div>
      <style>{`@keyframes pulse { 0%,100% { opacity: 1; } 50% { opacity: 0.5; } }`}</style>
    </div>
  )
}
