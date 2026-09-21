import { useState, useEffect } from 'react'
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, PieChart, Pie, Cell, LineChart, Line } from 'recharts'
import { getSaleKpi } from '../../services/api'

export default function SaleKpi() {
  const [kpi, setKpi] = useState<{ totalActivities: number; contacted: number; converted: number; conversionRate: number; dailyActivity?: { date: string; count: number }[] } | null>(null)

  useEffect(() => { getSaleKpi().then(setKpi).catch(() => {}) }, [])

  const pieData = kpi ? [
    { name: 'Đã liên hệ', value: kpi.contacted, color: '#2196f3' },
    { name: 'Đã chuyển đổi', value: kpi.converted, color: '#4caf50' },
  ] : []
  const dailyData = (kpi?.dailyActivity || []).map(d => ({ date: d.date.substring(5), count: d.count }))

  return (
    <div>
      <h2 style={{ marginBottom: '16px' }}>Dashboard KPI cá nhân</h2>
      {kpi && (
        <>
          <div className="metrics">
            <div className="metric-card"><div className="metric-value">{kpi.totalActivities}</div><div className="metric-label">Tổng hoạt động</div></div>
            <div className="metric-card"><div className="metric-value">{kpi.contacted}</div><div className="metric-label">Đã liên hệ</div></div>
            <div className="metric-card"><div className="metric-value">{kpi.converted}</div><div className="metric-label">Đã chuyển đổi</div></div>
            <div className="metric-card"><div className="metric-value">{kpi.conversionRate}%</div><div className="metric-label">Tỉ lệ chuyển đổi</div></div>
          </div>
          <div style={{ display: 'flex', gap: '20px', marginTop: '24px' }}>
            <div style={{ flex: 1, background: 'white', borderRadius: '12px', padding: '20px' }}>
              <h4>Phân bố hoạt động</h4>
              <ResponsiveContainer width="100%" height={250}>
                <BarChart data={pieData}>
                  <XAxis dataKey="name" /><YAxis /><Tooltip />
                  <Bar dataKey="value" fill="#E31837" radius={[8,8,0,0]} />
                </BarChart>
              </ResponsiveContainer>
            </div>
            <div style={{ flex: 1, background: 'white', borderRadius: '12px', padding: '20px' }}>
              <h4>Tỉ lệ chuyển đổi</h4>
              <ResponsiveContainer width="100%" height={250}>
                <PieChart>
                  <Pie data={pieData} dataKey="value" nameKey="name" cx="50%" cy="50%" outerRadius={80} label>
                    {pieData.map((d, i) => <Cell key={i} fill={d.color} />)}
                  </Pie>
                  <Tooltip />
                </PieChart>
              </ResponsiveContainer>
            </div>
          </div>
          {dailyData.length > 0 && (
            <div style={{ marginTop: '20px', background: 'white', borderRadius: '12px', padding: '20px' }}>
              <h4>Hoạt động 14 ngày gần nhất</h4>
              <ResponsiveContainer width="100%" height={250}>
                <LineChart data={dailyData}>
                  <XAxis dataKey="date" /><YAxis /><Tooltip />
                  <Line type="monotone" dataKey="count" stroke="#E31837" strokeWidth={2} dot={{ r: 3 }} />
                </LineChart>
              </ResponsiveContainer>
            </div>
          )}
        </>
      )}
    </div>
  )
}
