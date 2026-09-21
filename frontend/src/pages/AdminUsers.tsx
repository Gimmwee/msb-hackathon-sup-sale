import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import { useToast } from '../contexts/ToastContext'
import { getUsers, createUser, updateUser } from '../services/api'
import ConfirmModal from '../components/ConfirmModal'
import type { UserDto } from '../types'

export default function AdminUsers() {
  const { user, logout } = useAuth()
  const { success: toastSuccess, error: toastError } = useToast()
  const navigate = useNavigate()
  const [users, setUsers] = useState<UserDto[]>([])
  const [showForm, setShowForm] = useState(false)
  const [form, setForm] = useState({ username: '', password: '', fullName: '', role: 'SALE' })
  const [error, setError] = useState('')
  const [roleFilter, setRoleFilter] = useState('')
  const [creating, setCreating] = useState(false)
  const [toggling, setToggling] = useState('')
  const [lockTarget, setLockTarget] = useState<UserDto | null>(null)

  const fetchUsers = async () => {
    try { setUsers(await getUsers()) } catch { setError('Không tải được danh sách users') }
  }
  useEffect(() => { fetchUsers() }, [])

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault()
    if (creating) return
    setCreating(true); setError('')
    try {
      await createUser(form)
      setForm({ username: '', password: '', fullName: '', role: 'SALE' })
      setShowForm(false)
      fetchUsers()
      toastSuccess('Đã tạo tài khoản thành công')
    } catch { setError('Tạo user thất bại') } finally { setCreating(false) }
  }

  const handleToggle = async () => {
    if (!lockTarget) return
    setLockTarget(null)
    setToggling(lockTarget.id)
    try {
      await updateUser(lockTarget.id, { active: !lockTarget.active })
      fetchUsers()
      toastSuccess(lockTarget.active ? 'Đã khoá tài khoản' : 'Đã mở khoá tài khoản')
    } catch { toastError('Cập nhật thất bại') } finally { setToggling('') }
  }

  return (
    <div className="app">
      <header className="header">
        <div className="header-left">
          <div className="logo">M</div>
          <div><h1>Quản lý tài khoản</h1><p className="subtitle">{user?.fullName} (ADMIN)</p></div>
        </div>
        <div className="header-right">
          <button className="header-btn" onClick={() => navigate('/sale/upsale')}>← Sale Dashboard</button>
          <button className="header-btn" onClick={() => { logout(); navigate('/') }}>Đăng xuất</button>
        </div>
      </header>
      <div style={{ padding: '20px', maxWidth: '1000px', margin: '0 auto' }}>
        {error && <div className="error-msg">{error}</div>}
        <button className="send-btn" onClick={() => setShowForm(!showForm)}>
          {showForm ? 'Đóng form' : '+ Tạo tài khoản mới'}
        </button>
        <select value={roleFilter} onChange={e => setRoleFilter(e.target.value)} style={{ marginLeft: '12px', padding: '8px', borderRadius: '8px', border: '1px solid var(--console-border)', background: 'var(--console-surface)', color: 'var(--console-text)' }}>
          <option value="">Tất cả role</option>
          <option value="ADMIN">ADMIN</option>
          <option value="SALE">SALE</option>
          <option value="CONTACT_CENTER">CONTACT_CENTER</option>
        </select>
        {showForm && (
          <form onSubmit={handleCreate} className="admin-form">
            <input placeholder="Username" value={form.username} onChange={e => setForm({...form, username: e.target.value})} required />
            <input type="password" placeholder="Password (min 6)" value={form.password} onChange={e => setForm({...form, password: e.target.value})} required />
            <input placeholder="Họ tên" value={form.fullName} onChange={e => setForm({...form, fullName: e.target.value})} required />
            <select value={form.role} onChange={e => setForm({...form, role: e.target.value})} style={{ background: 'var(--console-surface)', color: 'var(--console-text)', border: '1px solid var(--console-border)' }}>
              <option value="SALE">SALE</option>
              <option value="CONTACT_CENTER">CONTACT_CENTER</option>
              <option value="ADMIN">ADMIN</option>
            </select>
            <button type="submit" className="send-btn" disabled={creating}>{creating ? 'Đang tạo...' : 'Tạo'}</button>
          </form>
        )}
        <table className="lead-table" style={{ marginTop: '20px' }}>
          <thead>
            <tr><th>Username</th><th>Họ tên</th><th>Role</th><th>Trạng thái</th><th>Hành động</th></tr>
          </thead>
          <tbody>
            {users.filter(u => !roleFilter || u.role === roleFilter).map(u => (
              <tr key={u.id}>
                <td>{u.username}</td>
                <td>{u.fullName}</td>
                <td>{u.role}</td>
                <td>{u.active ? '✅ Active' : '🔒 Locked'}</td>
                <td>
                  <button className="quick-btn" onClick={() => setLockTarget(u)} disabled={u.username === user?.username || toggling === u.id}>
                    {toggling === u.id ? 'Đang xử lý...' : u.active ? 'Khoá' : 'Mở khoá'}
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <ConfirmModal
        open={!!lockTarget}
        title={lockTarget?.active ? 'Xác nhận khoá tài khoản' : 'Xác nhận mở khoá'}
        message={lockTarget?.active ? `Bạn có chắc muốn khoá tài khoản "${lockTarget.username}"? Người dùng sẽ không thể đăng nhập.` : `Mở khoá tài khoản "${lockTarget?.username}"?`}
        confirmLabel={lockTarget?.active ? 'Khoá tài khoản' : 'Mở khoá'}
        onConfirm={handleToggle}
        onCancel={() => setLockTarget(null)}
        danger={lockTarget?.active}
      />
    </div>
  )
}
