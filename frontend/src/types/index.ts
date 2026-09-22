export interface ChatRequest {
  sessionId: string
  platform: string
  message: string
}

export interface ChatResponse {
  sessionId: string
  message: string
  intent: string
  leadCaptured: boolean
}

export interface Lead {
  id: string
  sessionId: string
  customerName: string
  phone: string
  productInterest: string
  status: string
  createdAt: string
  idNumber: string | null
  dob: string | null
  gender: string | null
  address: string | null
}

export interface Metrics {
  totalLeads: number
  newLeads: number
  contactedLeads: number
}

export interface FeedbackRequest {
  sessionId: string
  rating: number
  comment: string
}

export interface ChatMessage {
  role: 'user' | 'assistant'
  content: string
  leadCaptured?: boolean
  intent?: string
  cccdInfo?: CccdInfo
}

export interface CccdInfo {
  fullName: string | null
  idNumber: string | null
  dob: string | null
  gender: string | null
  address: string | null
  saved: boolean
  message: string
}

export interface LoginResponse {
  accessToken: string
  expiresIn: number
  role: string
  fullName: string
}

export interface UserDto {
  id: string
  username: string
  fullName: string
  role: string
  active: boolean
  createdAt: string
}

export interface ClaimData {
  id: string
  sessionId: string
  customerName: string
  customerPhone: string
  topic: string
  claimContent: string
  suggestedResponse: string
  status: string
  createdAt: string
}
