import type { ChatRequest, ChatResponse, Lead, Metrics, FeedbackRequest, CccdInfo, LoginResponse, UserDto } from '../types'

const API_BASE = import.meta.env.VITE_API_BASE_URL || ''
const API_KEY = import.meta.env.VITE_DASHBOARD_API_KEY || 'demo-key'

function authHeaders(): Record<string, string> {
  const token = localStorage.getItem('token')
  return token ? { Authorization: `Bearer ${token}` } : {}
}

export async function sendChat(request: ChatRequest): Promise<ChatResponse> {
  const res = await fetch(`${API_BASE}/api/v1/chat`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
  })
  if (!res.ok) throw new Error(`Chat failed: ${res.status}`)
  return res.json()
}

export async function uploadCccd(file: File, sessionId: string, phone: string): Promise<CccdInfo> {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('sessionId', sessionId)
  formData.append('phone', phone)
  const res = await fetch(`${API_BASE}/api/v1/ocr/cccd`, {
    method: 'POST',
    body: formData,
  })
  if (!res.ok) throw new Error(`OCR failed: ${res.status}`)
  return res.json()
}

export async function getLeads(): Promise<Lead[]> {
  const res = await fetch(`${API_BASE}/api/v1/leads`, {
    headers: { 'X-API-Key': API_KEY, ...authHeaders() },
  })
  if (!res.ok) throw new Error(`Get leads failed: ${res.status}`)
  return res.json()
}

export async function getMetrics(): Promise<Metrics> {
  const res = await fetch(`${API_BASE}/api/v1/leads/metrics`, {
    headers: { 'X-API-Key': API_KEY, ...authHeaders() },
  })
  if (!res.ok) throw new Error(`Get metrics failed: ${res.status}`)
  return res.json()
}

export async function sendFeedback(request: FeedbackRequest): Promise<void> {
  const res = await fetch(`${API_BASE}/api/v1/feedback`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
  })
  if (!res.ok) throw new Error(`Feedback failed: ${res.status}`)
}

export async function login(username: string, password: string): Promise<LoginResponse> {
  const res = await fetch(`${API_BASE}/api/v1/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password }),
  })
  if (!res.ok) throw new Error('Login failed')
  return res.json()
}

export async function staffChat(sessionId: string, message: string): Promise<{ message: string; lookupType: string }> {
  const res = await fetch(`${API_BASE}/api/v1/staff/chat`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...authHeaders() },
    body: JSON.stringify({ sessionId, message }),
  })
  if (!res.ok) throw new Error(`Staff chat failed: ${res.status}`)
  return res.json()
}

export async function getUsers(): Promise<UserDto[]> {
  const res = await fetch(`${API_BASE}/api/v1/admin/users`, {
    headers: authHeaders(),
  })
  if (!res.ok) throw new Error('Get users failed')
  return res.json()
}

export async function createUser(data: { username: string; password: string; fullName: string; role: string }): Promise<void> {
  const res = await fetch(`${API_BASE}/api/v1/admin/users`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...authHeaders() },
    body: JSON.stringify(data),
  })
  if (!res.ok) throw new Error('Create user failed')
}

export async function updateUser(id: string, updates: Record<string, unknown>): Promise<void> {
  const res = await fetch(`${API_BASE}/api/v1/admin/users/${id}`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json', ...authHeaders() },
    body: JSON.stringify(updates),
  })
  if (!res.ok) throw new Error('Update user failed')
}

export async function getConversations(): Promise<unknown[]> {
  const res = await fetch(`${API_BASE}/api/v1/admin/conversations`, {
    headers: authHeaders(),
  })
  if (!res.ok) throw new Error('Get conversations failed')
  return res.json()
}
