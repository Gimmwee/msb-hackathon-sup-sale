import { useState } from 'react'
import { saleLookup } from '../../services/api'

export default function SaleLookup() {
  const [query, setQuery] = useState('')
  const [result, setResult] = useState<Record<string, unknown> | null>(null)
  const [loading, setLoading] = useState(false)

  const handleSearch = async () => {
    if (!query.trim()) return
    setLoading(true)
    try { setResult(await saleLookup(query.trim())) } catch { setResult(null) }
    finally { setLoading(false) }
  }

  return (
    <div>
      <h2 style={{ marginBottom: '16px' }}>Tra cứu thông tin khách hàng</h2>
      <div style={{ display: 'flex', gap: '8px', marginBottom: '20px' }}>
        <input placeholder="Tên / SĐT / CCCD..." value={query} onChange={e => setQuery(e.target.value)}
          onKeyDown={e => e.key === 'Enter' && handleSearch()}
          style={{ flex: 1, padding: '10px 14px', border: '1px solid #ddd', borderRadius: '10px' }} />
        <button className="send-btn" onClick={handleSearch} disabled={loading}>{loading ? 'Đang tìm...' : 'Tra cứu'}</button>
      </div>
      {result && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
          {result.customer && (
            <div style={{ background: 'white', borderRadius: '12px', padding: '16px' }}>
              <h4>👤 Khách hàng</h4>
              <pre style={{ fontSize: '13px', margin: 0 }}>{JSON.stringify(result.customer, null, 2)}</pre>
            </div>
          )}
          {result.cic && (
            <div style={{ background: 'white', borderRadius: '12px', padding: '16px' }}>
              <h4>📊 CIC — Điểm: {(result.cic as { creditScore: number }).creditScore} | Tier: {(result.cic as { tier: string }).tier}</h4>
              <pre style={{ fontSize: '13px', margin: 0 }}>{JSON.stringify(result.cic, null, 2)}</pre>
            </div>
          )}
          {result.dominantCategory && (
            <div style={{ background: 'white', borderRadius: '12px', padding: '16px' }}>
              <h4>💳 Danh mục chi tiêu chủ đạo: {result.dominantCategory as string}</h4>
            </div>
          )}
          {result.recommendedProduct && (
            <div style={{ background: '#fff3e0', borderRadius: '12px', padding: '16px', border: '1px solid #ffe0b2' }}>
              <h4>⭐ Sản phẩm gợi ý: {(result.recommendedProduct as { name: string }).name}</h4>
              <p style={{ fontSize: '13px', margin: 0 }}>{(result.recommendedProduct as { description: string }).description}</p>
            </div>
          )}
          {!result.customer && !result.cic && <div style={{ color: '#999' }}>Không tìm thấy thông tin</div>}
        </div>
      )}
    </div>
  )
}
