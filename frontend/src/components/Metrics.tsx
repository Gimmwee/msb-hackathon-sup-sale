import type { Metrics as MetricsType } from '../types'

interface Props {
  metrics: MetricsType | null
  leadCount: number
}

export default function Metrics({ metrics, leadCount }: Props) {
  const total = metrics?.totalLeads ?? leadCount
  const newLeads = metrics?.newLeads ?? 0
  const contacted = metrics?.contactedLeads ?? 0
  const conversionRate = total > 0 ? ((contacted / total) * 100).toFixed(1) : '0.0'

  return (
    <div className="metrics">
      <div className="metric-card">
        <div className="metric-value">{total}</div>
        <div className="metric-label">Total Leads</div>
      </div>
      <div className="metric-card">
        <div className="metric-value">{newLeads}</div>
        <div className="metric-label">Leads mới</div>
      </div>
      <div className="metric-card">
        <div className="metric-value">{contacted}</div>
        <div className="metric-label">Đã liên hệ</div>
      </div>
      <div className="metric-card">
        <div className="metric-value">{conversionRate}%</div>
        <div className="metric-label">Conversion</div>
      </div>
    </div>
  )
}
