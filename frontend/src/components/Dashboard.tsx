import { useState, useEffect } from 'react'
import { getLeads, getMetrics } from '../services/api'
import type { Lead, Metrics as MetricsType } from '../types'
import Metrics from './Metrics'
import LeadTable from './LeadTable'

interface Props {
  refreshKey: number
}

export default function Dashboard({ refreshKey }: Props) {
  const [leads, setLeads] = useState<Lead[]>([])
  const [metrics, setMetrics] = useState<MetricsType | null>(null)
  const [error, setError] = useState('')

  useEffect(() => {
    const fetchData = async () => {
      try {
        const [leadsData, metricsData] = await Promise.all([getLeads(), getMetrics()])
        setLeads(leadsData)
        setMetrics(metricsData)
        setError('')
      } catch (e) {
        setError('Không kết nối được backend')
      }
    }
    fetchData()
    const interval = setInterval(fetchData, 5000)
    return () => clearInterval(interval)
  }, [refreshKey])

  return (
    <div className="dashboard">
      <div className="panel-title">📊 Dashboard</div>
      {error && <div className="error-msg">{error}</div>}
      <Metrics metrics={metrics} leadCount={leads.length} />
      <LeadTable leads={leads} />
    </div>
  )
}
