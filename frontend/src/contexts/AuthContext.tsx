import { createContext, useContext, useState, type ReactNode } from 'react'

interface AuthUser {
  token: string
  role: string
  fullName: string
}

interface AuthContextType {
  user: AuthUser | null
  login: (token: string, role: string, fullName: string) => void
  logout: () => void
}

const AuthContext = createContext<AuthContextType | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(() => {
    const token = localStorage.getItem('token')
    const role = localStorage.getItem('role')
    const fullName = localStorage.getItem('fullName')
    if (token && role && fullName) {
      return { token, role, fullName }
    }
    return null
  })

  const login = (token: string, role: string, fullName: string) => {
    localStorage.setItem('token', token)
    localStorage.setItem('role', role)
    localStorage.setItem('fullName', fullName)
    setUser({ token, role, fullName })
  }

  const logout = () => {
    localStorage.removeItem('token')
    localStorage.removeItem('role')
    localStorage.removeItem('fullName')
    setUser(null)
  }

  return <AuthContext.Provider value={{ user, login, logout }}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
