import { useState } from 'react'
import { getSaleTransactions } from '../../services/api'

export default function SaleTransactions() {
  const [query, setQuery] = useState('')
  const [txns, setTxns] = useState<{ category: string; amount: number; description: string; transactionDate: string }[]>([])
  const [searched, setSearched] = useState(false)

  const handleSearch = async () => {
    if (!query.trim()) return
    try { setTxns(await getSaleTransactions(query.trim())); setSearched(true) } catch { setTxns([]); setSearched(true) }
  }

  return (
    <div>
      <h2 style={{ marginBottom: '16px' }}>Lịch sử giao dịch</h2>
      <div style={{ display: 'flex', gap: '8px', marginBottom: '20px' }}>
        <input placeholder="Tìm theo tên, SĐT, hoặc CCCD..." value={query} onChange={e => setQuery(e.target.value)}
          onKeyDown={e => e.key === 'Enter' && handleSearch()}
          style={{ flex: 1, padding: '10px 14px', border: '1px solid #ddd', borderRadius: '10px' }} />
        <button className="send-btn" onClick={handleSearch}>Tìm</button>
      </div>
      {searched && txns.length === 0 && <div style={{ color: '#999', textAlign: 'center' }}>Không tìm thấy giao dịch</div>}
      {txns.length > 0 && (
        <table className="lead-table">
          <thead><tr><th>Ngày</th><th>Số tiền</th><th>Danh mục</th><th>Mô tả</th></tr></thead>
          <tbody>
            {txns.map((t, i) => (
              <tr key={i}>
                <td>{new Date(t.transactionDate).toLocaleDateString('vi-VN')}</td>
                <td>{t.amount.toLocaleString('vi-VN')} đ</td>
                <td><span className="status new">{t.category}</span></td>
                <td>{t.description}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  )
}
