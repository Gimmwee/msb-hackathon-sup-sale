import { useState } from 'react'
import { saleLookup } from '../../services/api'

interface LookupResult {
  customer?: { name: string; phone: string; idNumber: string; address: string }
  cic?: { creditScore: number; debtGroup: string; outstandingLoans: number; tier: string }
  dominantCategory?: string
  transactions?: { category: string; amount: number; description: string; transactionDate: string }[]
  recommendedProduct?: { name: string; description: string }
}

const CATEGORY_ICONS: Record<string, string> = {
  'Du lịch': '✈️', 'Mua sắm': '🛍️', 'Ăn uống': '🍽️',
  'Chuyển khoản': '💸', 'Tiện ích': '⚡',
}

export default function SaleLookup() {
  const [query, setQuery] = useState('')
  const [result, setResult] = useState<LookupResult | null>(null)
  const [loading, setLoading] = useState(false)
  const [searched, setSearched] = useState(false)

  const handleSearch = async () => {
    if (!query.trim()) return
    setLoading(true); setSearched(true)
    try { setResult(await saleLookup(query.trim()) as unknown as LookupResult) }
    catch { setResult(null) }
    finally { setLoading(false) }
  }

  const tierColor = (tier: string) => {
    if (tier === 'Tốt') return 'var(--status-success)'
    if (tier === 'Trung bình') return 'var(--status-warning)'
    return 'var(--status-error)'
  }

  const fieldValue = (val: string | null | undefined) =>
    (val && val.trim()) ? val : 'Chưa cập nhật'

  const cardStyle: React.CSSProperties = {
    background: 'var(--console-surface)', border: '1px solid var(--console-border)',
    borderRadius: 'var(--radius-md)', padding: '16px 20px',
  }
  const labelStyle: React.CSSProperties = {
    fontSize: '12px', color: 'var(--console-text-muted)', fontWeight: 600,
    textTransform: 'uppercase', letterSpacing: '0.5px', marginBottom: '4px',
  }

  return (
    <div>
      <h2 style={{ fontSize: '18px', marginBottom: '16px' }}>Tra cứu thông tin khách hàng</h2>
      <div style={{ display: 'flex', gap: '8px', marginBottom: '20px' }}>
        <input placeholder="Nhập tên, SĐT, hoặc CCCD..." value={query}
          onChange={e => setQuery(e.target.value)}
          onKeyDown={e => e.key === 'Enter' && handleSearch()}
          style={{ flex: 1, padding: '10px 14px', border: '1px solid var(--console-border)',
            borderRadius: 'var(--radius-md)', background: 'var(--console-surface)',
            color: 'var(--console-text)', fontSize: '14px', outline: 'none' }} />
        <button className="send-btn" onClick={handleSearch} disabled={loading}>
          {loading ? 'Đang tìm...' : 'Tra cứu'}
        </button>
      </div>

      {searched && !result && !loading && (
        <div style={{ color: 'var(--console-text-muted)', textAlign: 'center', padding: '48px', fontSize: '14px' }}>
          Không tìm thấy khách hàng phù hợp
        </div>
      )}

      {result && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>

          {/* Customer Info */}
          {result.customer && (
            <div style={cardStyle}>
              <div style={{ fontSize: '14px', fontWeight: 600, marginBottom: '12px', color: 'var(--console-text)' }}>
                👤 Thông tin khách hàng
              </div>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px 24px' }}>
                <div><div style={labelStyle}>Họ và tên</div><div style={{ fontSize: '14px', color: 'var(--console-text)' }}>{fieldValue(result.customer.name)}</div></div>
                <div><div style={labelStyle}>Số điện thoại</div><div style={{ fontSize: '14px', color: 'var(--console-text)' }}>{fieldValue(result.customer.phone)}</div></div>
                <div><div style={labelStyle}>Số CCCD</div><div style={{ fontSize: '14px', color: 'var(--console-text)' }}>{fieldValue(result.customer.idNumber)}</div></div>
                <div><div style={labelStyle}>Địa chỉ</div><div style={{ fontSize: '14px', color: 'var(--console-text)' }}>{fieldValue(result.customer.address)}</div></div>
              </div>
            </div>
          )}

          {/* CIC */}
          {result.cic && (
            <div style={cardStyle}>
              <div style={{ fontSize: '14px', fontWeight: 600, marginBottom: '12px', color: 'var(--console-text)' }}>
                📊 Đánh giá tín dụng (CIC)
              </div>
              <div style={{ display: 'flex', gap: '24px', alignItems: 'center' }}>
                <div style={{ textAlign: 'center' }}>
                  <div style={{ fontSize: '32px', fontWeight: 700, color: tierColor(result.cic.tier) }}>
                    {result.cic.creditScore}
                  </div>
                  <div style={{ fontSize: '11px', color: 'var(--console-text-muted)' }}>Điểm tín dụng</div>
                </div>
                <div style={{ flex: 1, display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '8px 16px' }}>
                  <div><div style={labelStyle}>Xếp hạng</div>
                    <span style={{ background: tierColor(result.cic.tier), color: 'white', padding: '2px 12px', borderRadius: '12px', fontSize: '12px', fontWeight: 600 }}>
                      {result.cic.tier}
                    </span>
                  </div>
                  <div><div style={labelStyle}>Nhóm nợ</div><div style={{ fontSize: '14px' }}>{fieldValue(result.cic.debtGroup)}</div></div>
                  <div><div style={labelStyle}>Dư nợ hiện tại</div><div style={{ fontSize: '14px' }}>{result.cic.outstandingLoans > 0 ? `${result.cic.outstandingLoans.toLocaleString('vi-VN')} đ` : 'Không có dư nợ'}</div></div>
                </div>
              </div>
            </div>
          )}

          {/* Dominant Category */}
          {result.dominantCategory && (
            <div style={cardStyle}>
              <div style={{ fontSize: '14px', fontWeight: 600, marginBottom: '12px', color: 'var(--console-text)' }}>
                💳 Hành vi giao dịch nổi bật
              </div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                <span style={{ fontSize: '32px' }}>{CATEGORY_ICONS[result.dominantCategory] || '📊'}</span>
                <div>
                  <div style={{ fontSize: '16px', fontWeight: 600, color: 'var(--console-text)' }}>
                    {result.dominantCategory}
                  </div>
                  <div style={{ fontSize: '12px', color: 'var(--console-text-muted)' }}>
                    Danh mục chi tiêu chiếm tỉ trọng cao nhất
                  </div>
                </div>
              </div>
              {result.transactions && result.transactions.length > 0 && (
                <div style={{ marginTop: '12px', fontSize: '12px', color: 'var(--console-text-muted)' }}>
                  {result.transactions.filter(t => t.category === result.dominantCategory).length}/{result.transactions.length} giao dịch gần đây thuộc danh mục này
                </div>
              )}
            </div>
          )}

          {/* Product Recommendation */}
          {result.recommendedProduct && (
            <div style={{ ...cardStyle, background: '#FFF8E1', border: '1px solid #FFE082' }}>
              <div style={{ fontSize: '14px', fontWeight: 600, marginBottom: '8px' }}>
                ⭐ Sản phẩm gợi ý
              </div>
              <div style={{ fontSize: '16px', fontWeight: 600, marginBottom: '4px' }}>
                {result.recommendedProduct.name}
              </div>
              <p style={{ fontSize: '13px', color: '#6B5E3A', margin: 0 }}>
                {result.recommendedProduct.description}
              </p>
            </div>
          )}

          {/* No customer found but has other data */}
          {!result.customer && !result.cic && !result.recommendedProduct && (
            <div style={{ color: 'var(--console-text-muted)', textAlign: 'center', padding: '48px', fontSize: '14px' }}>
              Không tìm thấy thông tin khách hàng
            </div>
          )}
        </div>
      )}
    </div>
  )
}
