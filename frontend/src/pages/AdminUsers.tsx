import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import { getUsers, createUser, updateUser } from '../services/api'
import type { UserDto } from '../types'

export default function AdminUsers() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const [users, setUsers] = useState<UserDto[]>([])
  const [showForm, setShowForm] = useState(false)
  const [form, setForm] = useState({ username: '', password: '', fullName: '', role: 'STAFF' })
  const [error, setError] = useState('')

  const fetchUsers = async () => {
    try {
      const data = await getUsers()
      setUsers(data)
    } catch { setError('Không tải được danh sách users') }
  }

  useEffect(() => { fetchUsers() }, [])

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    try {
      await createUser(form)
      setForm({ username: '', password: '', fullName: '', role: 'STAFF' })
      setShowForm(false)
      fetchUsers()
    } catch { setError('Tạo user thất bại') }
  }

  const handleToggle = async (id: string, active: boolean) => {
    try {
      await updateUser(id, { active: !active })
      fetchUsers()
    } catch { setError('Cập nhật thất bại') }
  }

  return (
    <div className="app">
      <header className="header">
        <div className="header-left">
          <div className="logo">M</div>
          <div>
            <h1>Quản lý tài khoản</h1>
            <p className="subtitle">{user?.fullName} (ADMIN)</p>
          </div>
        </div>
        <div className="header-right">
          <button className="header-btn" onClick={() => navigate('/staff/dashboard')}>← Dashboard</button>
          <button className="header-btn" onClick={() => { logout(); navigate('/') }}>Đăng xuất</button>
        </div>
      </header>
      <div style={{ padding: '20px', maxWidth: '1000px', margin: '0 auto' }}>
        {error && <div className="error-msg">{error}</div>}
        <button className="send-btn" onClick={() => setShowForm(!showForm)}>
          {showForm ? 'Đóng form' : '+ Tạo tài khoản mới'}
        </button>
        {showForm && (
          <form onSubmit={handleCreate} className="admin-form">
            <input placeholder="Username" value={form.username} onChange={e => setForm({...form, username: e.target.value})} required />
            <input type="password" placeholder="Password (min 6)" value={form.password} onChange={e => setForm({...form, password: e.target.value})} required />
            <input placeholder="Họ tên" value={form.fullName} onChange={e => setForm({...form, fullName: e.target.value})} required />
            <select value={form.role} onChange={e => setForm({...form, role: e.target.value})}>
              <option value="STAFF">STAFF</option>
              <option value="ADMIN">ADMIN</option>
            </select>
            <button type="submit" className="send-btn">Tạo</button>
          </form>
        )}
        <table className="lead-table" style={{ marginTop: '20px' }}>
          <thead>
            <tr>
              <th>Username</th>
              <th>Họ tên</th>
              <th>Role</th>
              <th>Trạng thái</th>
              <th>Hành động</th>
            </tr>
          </thead>
          <tbody>
            {users.map(u => (
              <tr key={u.id}>
                <td>{u.username}</td>
                <td>{u.fullName}</td>
                <td>{u.role}</td>
                <td>{u.active ? '✅ Active' : '🔒 Locked'}</td>
                <td>
                  <button className="quick-btn" onClick={() => handleToggle(u.id, u.active)} disabled={u.username === user?.username}>
                    {u.active ? 'Khoá' : 'Mở khoá'}
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}
