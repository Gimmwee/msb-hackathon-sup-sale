import type { Lead } from '../types'

interface Props {
  leads: Lead[]
}

export default function LeadTable({ leads }: Props) {
  return (
    <div className="lead-table-container">
      <h3>Danh sách Leads</h3>
      {leads.length === 0 ? (
        <div className="no-leads">Chưa có lead nào.</div>
      ) : (
        <table className="lead-table">
          <thead>
            <tr>
              <th>Tên</th>
              <th>SĐT</th>
              <th>Nhu cầu</th>
              <th>Số CCCD</th>
              <th>Địa chỉ</th>
              <th>Trạng thái</th>
              <th>Thời gian</th>
            </tr>
          </thead>
          <tbody>
            {leads.map(lead => (
              <tr key={lead.id}>
                <td>{lead.customerName}</td>
                <td>{lead.phone}</td>
                <td>{lead.productInterest}</td>
                <td>{lead.idNumber || <span className="null-cell">—</span>}</td>
                <td>{lead.address || <span className="null-cell">—</span>}</td>
                <td><span className={`status ${lead.status.toLowerCase()}`}>{lead.status}</span></td>
                <td>{new Date(lead.createdAt).toLocaleString('vi-VN')}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  )
}
