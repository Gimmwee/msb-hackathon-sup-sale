import { useState } from 'react'
import { useNavigate, useLocation } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import { login as apiLogin } from '../services/api'

export default function Login() {
  const navigate = useNavigate()
  const location = useLocation()
  const { login } = useAuth()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const errorMsg = (location.state as { error?: string })?.error

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!username || !password) return
    setError('')
    setLoading(true)
    try {
      const res = await apiLogin(username, password)
      login(res.accessToken, res.role, res.fullName)
      const dest = res.role === 'ADMIN' ? '/admin/users' : res.role === 'SALE' ? '/sale/upsale' : '/cc'
      navigate(dest)
    } catch {
      setError('Tên đăng nhập hoặc mật khẩu không đúng')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="login-page">
      <div className="login-card">
        <div className="login-logo">M</div>
        <h1>Đăng nhập nhân viên</h1>
        {errorMsg && <div className="login-error">{errorMsg}</div>}
        {error && <div className="login-error">{error}</div>}
        <form onSubmit={handleLogin}>
          <input
            type="text"
            placeholder="Tên đăng nhập"
            value={username}
            onChange={e => setUsername(e.target.value)}
            disabled={loading}
          />
          <input
            type="password"
            placeholder="Mật khẩu"
            value={password}
            onChange={e => setPassword(e.target.value)}
            disabled={loading}
          />
          <button type="submit" disabled={loading}>
            {loading ? 'Đang đăng nhập...' : 'Đăng nhập'}
          </button>
        </form>
        <button className="back-btn" onClick={() => navigate('/')}>← Quay lại</button>
      </div>
    </div>
  )
}
