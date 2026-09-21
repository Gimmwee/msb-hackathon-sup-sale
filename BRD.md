# BRD — sup-sale: AI Customer Consultation & Lead Capture Platform
## MSB AI Hackathon 2026 — Business Requirements Document

---

## 1. Executive Summary

**sup-sale** là nền tảng AI tư vấn khách hàng cho Ngân hàng Hàng Hải Việt Nam (MSB), tích hợp 4 workspace chuyên biệt: **Customer** (khách hàng), **Sale** (nhân viên kinh doanh), **Admin** (quản trị), và **Contact Center** (xử lý khiếu nại). Hệ thống sử dụng GreenNode MaaS LLM (OpenAI-compatible) để hiểu nhu cầu khách hàng, tự động trích xuất lead, OCR căn cước công dân, tra cứu CIC, và xử lý khiếu nại — tất cả trong một kiến trúc có thể mở rộng ra hệ thống doanh nghiệp (CRM, Core Banking).

---

## 2. Business Objectives

| Mục tiêu | Đo lường |
|---|---|
| Tự động tư vấn sản phẩm ngân hàng 24/7 | Khách chat → AI hiểu intent → trả lời tự nhiên |
| Trích xuất lead không cần form thủ công | Tên + SĐT + nhu cầu → lưu DB tự động qua tool calling |
| OCR CCCD giảm thao tác thủ công | Upload ảnh → Gemma 4 vision → trích xuất thông tin |
| Nhân viên Sale theo dõi & khai thác lead | Dashboard KPI, Data UpSale, tra cứu CIC + giao dịch |
| Contact Center xử lý khiếu nại có audit | Claim detection ngầm → CC duyệt → gửi email |
| Kiến trúc mở rộng được | Interface-based: LlmClient, AgentTool, CicService, EmailService |

---

## 3. Stakeholders & Roles

| Role | Workspace | Quyền |
|---|---|---|
| **Customer** (khách hàng) | `/customer` | Chat với AI, upload CCCD — không cần login |
| **SALE** (nhân viên kinh doanh) | `/sale/*` | Data UpSale, tra cứu khách hàng/CIC, KPI cá nhân, chat nội bộ |
| **CONTACT_CENTER** (nhân viên CC) | `/cc` | Xem claims, approve/abort, xem lịch sử chat gốc, chat nội bộ |
| **ADMIN** (quản trị) | `/admin/users` | Tạo/khoá tài khoản, gán role, xem tất cả workspace |

**Demo accounts:**

| Username | Password | Role |
|---|---|---|
| `admin` | `Admin@123` | ADMIN |
| `sale01` | `Sale@123` | SALE |
| `cc01` | `Cc@12345` | CONTACT_CENTER |

---

## 4. System Architecture

### 4.1 High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                        FRONTEND (React + TS + Vite)                  │
│                                                                     │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────┐  ┌────────────┐  │
│  │  Customer     │  │  Sale         │  │  Admin   │  │  Contact   │  │
│  │  Homepage     │  │  Workspace    │  │  Users   │  │  Center    │  │
│  │  + ChatWidget │  │  (sidebar 4)  │  │          │  │  Claims    │  │
│  └──────┬───────┘  └──────┬───────┘  └────┬─────┘  └─────┬──────┘  │
│         │                 │               │              │          │
│         └────────────────┬┴───────────────┴──────────────┘          │
│                          │ fetch (JWT / public)                     │
└──────────────────────────┼──────────────────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────────────┐
│                    BACKEND (Spring Boot 3.3 + Java 21)              │
│                                                                     │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  Security Layer                                               │   │
│  │  JwtAuthFilter → SecurityConfig (RBAC) → CORS                │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  Controllers                                                  │   │
│  │  Chat | OCR | Auth | Sale | CC | Admin | Feedback | Zalo    │   │
│  └──────────────────────────┬──────────────────────────────────┘   │
│                              │                                      │
│  ┌──────────────────────────▼──────────────────────────────────┐   │
│  │  AI Agent Layer                                              │   │
│  │  ┌─────────────────┐    ┌─────────────────┐                  │   │
│  │  │ AgentOrchestrator│    │ StaffAgent      │                  │   │
│  │  │ (customer)       │    │ Orchestrator    │                  │   │
│  │  │ + Claim Detection│    │ (staff)         │                  │   │
│  │  └────────┬────────┘    └────────┬────────┘                  │   │
│  │           │                      │                            │   │
│  │  ┌────────▼──────────────────────▼────────┐                   │   │
│  │  │  LlmClient (interface)                  │                   │   │
│  │  │  └─ GreenNodeClient (WebClient)         │                   │   │
│  │  └────────────────────┬───────────────────┘                   │   │
│  │                       │                                       │   │
│  │  ┌────────────────────▼───────────────────┐                   │   │
│  │  │  Tools (AgentTool interface)            │                   │   │
│  │  │  LeadTool | ProductTool | CustomerTool  │                   │   │
│  │  │  CicTool (staff only)                   │                   │   │
│  │  └────────────────────┬───────────────────┘                   │   │
│  └───────────────────────┼───────────────────────────────────────┘   │
│                          │                                           │
│  ┌───────────────────────▼───────────────────────────────────────┐   │
│  │  Service Layer                                                 │   │
│  │  Lead | Product | Customer | Conversation | Feedback |        │   │
│  │  Ocr (Gemma vision) | Claim | Email (SMTP/Mock) |             │   │
│  │  Transaction | SaleActivity | CicService (Mock) |             │   │
│  │  ProductRecommendation (rule-based)                           │   │
│  └───────────────────────┬───────────────────────────────────────┘   │
│                          │                                           │
│  ┌───────────────────────▼───────────────────────────────────────┐   │
│  │  Repository Layer (Spring Data JPA)                           │   │
│  └───────────────────────┬───────────────────────────────────────┘   │
└──────────────────────────┼───────────────────────────────────────────┘
                           │
┌──────────────────────────▼───────────────────────────────────────────┐
│                    PostgreSQL 15 (Docker)                            │
│  12 tables: users, customers, products, leads, conversations,       │
│  messages, feedback, claims, email_log, transactions,               │
│  sale_activities, cic_records                                         │
└──────────────────────────────────────────────────────────────────────┘

                           │
┌──────────────────────────▼───────────────────────────────────────────┐
│              GreenNode MaaS (OpenAI-compatible LLM)                  │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────────┐  │
│  │ qwen3.6-flash   │  │ glm-5.2-hack    │  │ gemma-4-31b-it      │  │
│  │ (chat + tools)  │  │ (chat + tools)  │  │ (vision / OCR CCCD) │  │
│  └─────────────────┘  └─────────────────┘  └─────────────────────┘  │
│  Base: https://maas-llm-aiplatform-hcm.api.vngcloud.vn/v1           │
└──────────────────────────────────────────────────────────────────────┘
```

### 4.2 AI Agent Architecture — Tool Calling Flow

```
User message
    │
    ▼
┌──────────────────────────────────────────┐
│  AgentOrchestrator (customer)            │
│                                          │
│  1. Load conversation history (DB)       │
│  2. Build messages: [system, ...history, │
│     user_message]                        │
│  3. Define tools (JSON Schema)           │
│  4. LOOP (max 5 iterations):             │
│     ┌────────────────────────────────┐   │
│     │ GreenNodeClient.chatCompletion │   │
│     │ (messages, tools)              │   │
│     └───────────┬────────────────────┘   │
│                 │                        │
│         ┌───────▼───────┐                │
│         │ tool_calls?   │                │
│         └───┬───────┬───┘                │
│         YES │       │ NO                 │
│             ▼       ▼                    │
│    Execute tools   Return content        │
│    Add results     → Save to DB          │
│    Loop back       → Return reply        │
│                                          │
│  5. Claim detection (ngầm, async):       │
│     regex keywords → LLM classify        │
│     → save Claim (PENDING)               │
└──────────────────────────────────────────┘
```

### 4.3 Staff Agent — Rule-Based Guard

```
Staff message
    │
    ▼
┌──────────────────────────────────────────┐
│  StaffAgentOrchestrator                  │
│                                          │
│  Rule-based guard (TRƯỚC khi gọi LLM):  │
│  ┌────────────────────────────────────┐  │
│  │ Regex: CCCD (9/12 digits)?         │  │
│  │   YES → CicTool.doLookup()         │  │
│  │         + Customer lookup by phone │  │
│  │         + Leads by phone           │  │
│  │         + Product recommendations  │  │
│  │         → LLM chỉ format câu trả   │  │
│  │                                    │  │
│  │ Regex: Phone (VN format)?          │  │
│  │   YES → CustomerTool + CIC +       │  │
│  │         Leads + Products            │  │
│  │         → LLM chỉ format câu trả   │  │
│  │                                    │  │
│  │ No match → Normal LLM tool-calling │  │
│  └────────────────────────────────────┘  │
│                                          │
│  CIC audit log: cic_lookup_logs          │
│  (staffUserId, queriedValue, timestamp)  │
└──────────────────────────────────────────┘
```

---

## 5. Technology Stack

| Layer | Technology | Version | Purpose |
|---|---|---|---|
| Frontend | React + TypeScript | 18.3 | UI components, routing |
| Build FE | Vite | 5.4 | Dev server, HMR |
| Charts | Recharts | 2.15 | KPI line/bar/pie charts |
| Routing | react-router-dom | 6.28 | 4 workspace routing |
| Backend | Spring Boot | 3.3.7 | REST API, security, JPA |
| Java | OpenJDK | 21 | Runtime |
| Security | Spring Security + JWT (jjwt) | 0.12.6 | Auth, RBAC |
| LLM | GreenNode MaaS | OpenAI-compatible | Chat + tool calling + vision |
| LLM Chat | qwen/qwen3.6-flash | — | Primary agent (tool calling) |
| LLM Vision | google/gemma-4-31b-it | — | OCR CCCD images |
| Database | PostgreSQL | 15-alpine | 12 tables, UUID PKs |
| Deployment | Docker Compose | — | 3 services: db, backend, frontend |

---

## 6. Four Workspaces

### 6.1 Customer Workspace (`/customer`, public)

```
┌─────────────────────────────────────────────────────┐
│ MSB Nav: Logo | Cá nhân | Doanh nghiệp | Ưu tiên    │
├─────────────────────────────────────────────────────┤
│                                                     │
│  Hero: "Tư vấn tài chính thông minh"   [🏦]        │
│  Mô tả + CTA "Bắt đầu chat"                          │
│                                                     │
├─────────────────────────────────────────────────────┤
│ 🏦 Vay vốn  │ 💳 Thẻ tín dụng │ 💰 Tiết kiệm │ 📱 mBank │
│ (horizontal categories with dividers)               │
├─────────────────────────────────────────────────────┤
│                                          ┌────────┐ │
│                                          │ 💬 FAB │ │
│                                          └────────┘ │
└─────────────────────────────────────────────────────┘
         Click FAB → ChatWidget opens (scale animation)
```

**Features:**
- MSB-style homepage (navy #0A1628, gold #D4A72C accents)
- Floating ChatWidget (💬 bottom-right, scale+fade animation)
- AI chat with tool calling (captureLead, getProductInfo, getCustomerProfile)
- CCCD upload (📎) → Gemma 4 OCR → save to Customer table
- Claim detection ngầm (regex keywords → LLM classify → save Claim)
- Session ID persisted in localStorage (multi-turn context)

### 6.2 Sale Workspace (`/sale/*`, role SALE/ADMIN)

```
┌─────────┬───────────────────────────────────────────┐
│ Sidebar │  Content (changes per route)              │
│         │                                           │
│ MSB Sale│  ┌─────────────────────────────────────┐  │
│         │  │                                     │  │
│ 📊 Data │  │  Data UpSale / Transactions /       │  │
│   UpSale│  │  Lookup / KPI Dashboard             │  │
│         │  │                                     │  │
│ 💳 Giao  │  └─────────────────────────────────────┘  │
│   dịch  │                                           │
│         │                          ┌──────────────┐ │
│ 🔍 Tra   │                          │ 🛠️ Staff     │ │
│   cứu   │                          │ ChatWidget   │ │
│         │                          └──────────────┘ │
│ 📈 KPI  │                                           │
│         │                                           │
│ ← Home  │                                           │
│ Logout  │                                           │
└─────────┴───────────────────────────────────────────┘
```

**4 sections:**

| Route | Feature | Details |
|---|---|---|
| `/sale/upsale` | Data UpSale | Lead list (right) + chat transcript (left) + customer header + state machine (NEW→CONTACTED→CONVERTED) + activity logging |
| `/sale/transactions` | Lịch sử giao dịch | Search by phone/name/CCCD → transaction list (date, amount, category, description) |
| `/sale/lookup` | Tra cứu khách hàng | CIC score + tier (Tốt/Trung bình/Cần thận trọng) + dominant category + product recommendation (rule-based) |
| `/sale/dashboard` | KPI cá nhân | Metrics (total/contacted/converted/rate) + BarChart + PieChart + LineChart (14 days daily activity) |

**Staff ChatWidget** (🛠️ bottom-right on all /sale/* pages):
- Calls `/api/v1/staff/chat` (StaffAgentOrchestrator)
- Rule-based guard: CCCD/phone detected → direct tool lookup (bypass LLM parameter interpretation)
- Enriched lookup: CIC + customer + leads + product recommendations
- CIC audit logging

### 6.3 Admin Workspace (`/admin/users`, role ADMIN)

| Feature | Details |
|---|---|
| User list | Username, full name, role, status (active/locked) |
| Create user | Form: username, password (BCrypt), full name, role (SALE/CC/ADMIN) |
| Lock/unlock | ConfirmModal (danger style for lock) — cannot lock self |
| Role filter | Dropdown: All / ADMIN / SALE / CONTACT_CENTER |
| Toast feedback | Success/error toast after each action |

### 6.4 Contact Center Workspace (`/cc`, role CONTACT_CENTER/ADMIN)

```
┌─────────────────────────────────────────────────────┐
│ Header: Contact Center — Claims                      │
├──────────────────────┬──────────────────────────────┤
│ Claims List (left)   │  Claim Detail (right)        │
│                      │                              │
│ [Filter: All/PEND/   │  Topic + customer name       │
│  APPR/ABORT]         │  Claim content               │
│                      │  Chat history (transcript)   │
│ ● An | Lỗi GD | PEND │  Suggested response (edit)   │
│ ● Bình | Phí | PEND  │                              │
│ ● Cường | Thái độ    │  [✓ Approve] [✗ Abort]       │
│ ● Lan | GD | APPROVED│                              │
│                      │  After action: badge + hide  │
│                      │  buttons                     │
└──────────────────────┴──────────────────────────────┘
                                        ┌──────────────┐
                                        │ 🛠️ Staff     │
                                        │ ChatWidget   │
                                        └──────────────┘
```

**Claim lifecycle:**
1. Customer chat → claim detection ngầm (regex keywords) → Claim PENDING
2. CC opens `/cc` → sees PENDING claims
3. Click claim → view chat history + suggested response (editable)
4. **Approve** → send email (SMTP or Mock) → status APPROVED + email_log
5. **Abort** → ConfirmModal → status ABORTED (no email)

---

## 7. Database Schema (12 Tables)

```
┌─────────────┐   ┌─────────────┐   ┌─────────────┐
│  customers   │   │  products   │   │   users     │
│─────────────│   │─────────────│   │─────────────│
│ id (UUID)   │   │ id (UUID)   │   │ id (UUID)   │
│ name        │   │ name        │   │ username    │
│ phone       │   │ description │   │ passwordHash│
│ email       │   │ category    │   │ fullName    │
│ idNumber    │   │ eligibility │   │ role (enum) │
│ dob         │   │ documents   │   │ active      │
│ gender      │   │ createdAt   │   │ createdAt   │
│ address     │   └─────────────┘   └─────────────┘
│ createdAt   │
│ updatedAt   │   ┌─────────────┐   ┌─────────────┐
└─────────────┘   │   leads     │   │conversations│
                  │─────────────│   │─────────────│
┌─────────────┐   │ id (UUID)   │   │ id (UUID)   │
│transactions │   │ sessionId   │   │ sessionId   │
│─────────────│   │ customerName│   │ platform    │
│ id (UUID)   │   │ phone       │   │ createdAt   │
│ customerPhon│   │ productInt  │   └─────────────┘
│ txnDate     │   │ status      │
│ amount      │   │ createdAt   │   ┌─────────────┐
│ category    │   │ updatedAt   │   │  messages   │
│ description │   │ UNIQUE(sess,│   │─────────────│
└─────────────┘   │  phone)     │   │ id (UUID)   │
                  └─────────────┘   │ sessionId   │
┌─────────────┐                     │ role        │
│cic_records  │   ┌─────────────┐   │ content     │
│─────────────│   │  feedback   │   │ createdAt   │
│ id (UUID)   │   │─────────────│   └─────────────┘
│ idNumber    │   │ id (UUID)   │
│ phone       │   │ sessionId   │   ┌─────────────┐
│ creditScore │   │ rating      │   │   claims    │
│ debtGroup   │   │ comment     │   │─────────────│
│ outstanding │   │ createdAt   │   │ id (UUID)   │
│ lastUpdated │   └─────────────┘   │ sessionId   │
└─────────────┘                     │ customerName│
                  ┌─────────────┐   │ customerPhon│
┌─────────────┐   │  email_log  │   │ topic       │
│sale_activities│ │─────────────│   │ claimContent│
│─────────────│   │ id (UUID)   │   │ suggested   │
│ id (UUID)   │   │ claimId     │   │ status      │
│ leadId      │   │ toEmail     │   │ createdAt   │
│ saleUserId  │   │ subject     │   │ resolvedAt  │
│ action      │   │ body        │   │ resolvedBy  │
│ note        │   │ sentAt      │   └─────────────┘
│ createdAt   │   │ mock        │
└─────────────┘   └─────────────┘

┌─────────────┐
│cic_lookup_  │
│   logs      │
│─────────────│
│ id (UUID)   │
│ staffUserId │
│ queriedValue│
│ createdAt   │
└─────────────┘
```

**Seed data (auto-inserted on startup):**
- 4 products, 18 customers, 14 leads, 14 conversations (~56 messages)
- 140 transactions, 18 CIC records (3 tiers), 8 claims, 28 sale activities
- 3 users (admin, sale01, cc01)

---

## 8. API Endpoints

### 8.1 Public (no auth)

| Method | Path | Description |
|---|---|---|
| POST | `/api/v1/chat` | Customer chat → AI agent (tool calling + claim detection) |
| POST | `/api/v1/ocr/cccd` | Upload CCCD image (multipart) → Gemma OCR |
| POST | `/api/v1/feedback` | Customer feedback (rating 1-5) |
| POST | `/api/v1/webhooks/zalo` | Zalo OA webhook adapter |
| POST | `/api/v1/auth/login` | Login → JWT token |
| GET | `/actuator/health` | Health check (DB + disk + ping) |

### 8.2 Sale (role SALE/ADMIN, JWT)

| Method | Path | Description |
|---|---|---|
| GET | `/api/v1/sale/leads` | All leads |
| GET | `/api/v1/sale/leads/{sessionId}/messages` | Chat transcript for a lead |
| POST | `/api/v1/sale/leads/{leadId}/activity` | Log activity (CONTACTED/CONVERTED/NOTE) |
| GET | `/api/v1/sale/leads/{leadId}/activities` | Activities for a lead |
| GET | `/api/v1/sale/kpi` | KPI metrics + daily activity (14 days) |
| GET | `/api/v1/sale/lookup?query=` | Customer + CIC + transactions + product recommendation |
| GET | `/api/v1/sale/transactions?query=` | Transactions by phone/name/CCCD |
| POST | `/api/v1/staff/chat` | Staff internal chat (StaffAgentOrchestrator) |

### 8.3 Contact Center (role CONTACT_CENTER/ADMIN, JWT)

| Method | Path | Description |
|---|---|---|
| GET | `/api/v1/cc/claims?status=` | Claims list (optional filter) |
| GET | `/api/v1/cc/claims/{id}` | Claim detail |
| GET | `/api/v1/cc/claims/{id}/messages` | Original chat history for claim |
| POST | `/api/v1/cc/claims/{id}/approve` | Approve + send email |
| POST | `/api/v1/cc/claims/{id}/abort` | Abort claim |

### 8.4 Admin (role ADMIN, JWT)

| Method | Path | Description |
|---|---|---|
| GET | `/api/v1/admin/users` | List all users |
| POST | `/api/v1/admin/users` | Create user (BCrypt) |
| PATCH | `/api/v1/admin/users/{id}` | Update role/active |
| GET | `/api/v1/admin/conversations` | All conversations with messages |
| GET | `/api/v1/admin/conversations/{sessionId}` | Single conversation |

### 8.5 Auth

| Method | Path | Description |
|---|---|---|
| GET | `/api/v1/auth/me` | Current user info from JWT |

---

## 9. Security & RBAC

```
                    ┌─────────────────┐
                    │  Login Request   │
                    │  (username/pw)   │
                    └────────┬────────┘
                             │
                    ┌────────▼────────┐
                    │ AuthController   │
                    │ + BCrypt verify  │
                    └────────┬────────┘
                             │
                    ┌────────▼────────┐
                    │ JwtService       │
                    │ generate token   │
                    │ (sub, role, exp) │
                    └────────┬────────┘
                             │
                    ┌────────▼────────┐
                    │  JWT Token       │
                    │  Bearer <token>  │
                    └────────┬────────┘
                             │
           ┌─────────────────┼─────────────────┐
           │                 │                 │
    ┌──────▼──────┐  ┌──────▼──────┐  ┌──────▼──────┐
    │ /api/v1/    │  │ /api/v1/    │  │ /api/v1/    │
    │ sale/**     │  │ cc/**       │  │ admin/**    │
    │             │  │             │  │             │
    │ hasRole:    │  │ hasRole:    │  │ hasRole:    │
    │ SALE, ADMIN │  │ CC, ADMIN   │  │ ADMIN only  │
    └─────────────┘  └─────────────┘  └─────────────┘
```

- **Password**: BCrypt hash, never logged, never returned in API response
- **JWT**: HS256, configurable secret + expiration (default 1h)
- **CORS**: configurable allowed origins (default localhost:5173)
- **Customer endpoints**: no auth (public chat, OCR, feedback)
- **GREENNODE_API_KEY**: never exposed to frontend

---

## 10. Design System

### 10.1 Token System (`tokens.css`)

| Token Group | Purpose | Key Colors |
|---|---|---|
| Customer (Public) | Homepage, landing | Navy #0A1628, Red #E31837, Gold #D4A72C |
| Console (Internal) | Sale, Admin, CC, Login | Light #F5F7FA, White surface, Text #0F172A |
| Status | Badges everywhere | Success #16A34A, Warning #D97706, Error #DC2626, Info #3B82F6 |
| Typography | All | Inter (Google Fonts), weights 400-700 |

### 10.2 UI Patterns

| Pattern | Component | Usage |
|---|---|---|
| Toast | ToastContext | Success/error feedback (top-right, 3s auto-dismiss) |
| ConfirmModal | ConfirmModal.tsx | Destructive actions (lock user, abort claim) |
| ChatWidget | ChatWidget.tsx | Floating chat (customer 💬 + staff 🛠️) |
| ProtectedRoute | ProtectedRoute.tsx | Role-based route guard |
| State machine | SaleUpSale.tsx | NEW→CONTACTED→CONVERTED with badges |
| Crossfade | CSS animation | Customer switch in Data UpSale (220ms) |

---

## 11. AI Agent Details

### 11.1 Customer Agent (AgentOrchestrator)

**System Prompt (Vietnamese):**
- Role: "sup-sale", MSB bank consultant
- Rules: Vietnamese, no fabricated rates, call captureLead when 3 fields valid, ask for missing info, mention CCCD upload
- Tools: captureLead, getProductInfo, getCustomerProfile

**Claim Detection (ngầm):**
- Regex: 20+ Vietnamese keywords (khiếu nại, không hài lòng, quá tệ, kiện, lừa đảo...)
- Pattern: ALL CAPS 10+ chars, 3+ exclamation marks
- If detected → LLM classify topic + generate suggested response → save Claim (PENDING)
- Does NOT interrupt customer's normal chat response

### 11.2 Staff Agent (StaffAgentOrchestrator)

**Rule-Based Guard (before LLM):**
- CCCD regex (9/12 digits) → direct CicTool + Customer + Leads + Products → LLM formats response
- Phone regex (VN format) → direct Customer + CIC + Leads + Products → LLM formats response
- No match → normal LLM tool-calling flow

**CIC Audit:** every lookup logged to `cic_lookup_logs` (staffUserId, queriedValue, timestamp)

### 11.3 Product Recommendation (rule-based)

```
CIC Tier + Dominant Transaction Category → Product
─────────────────────────────────────────────────────
≥700 (Tốt) + Du lịch/Mua sắm/Ăn uống → Thẻ tín dụng
≥700 (Tốt) + Chuyển khoản/Tiện ích  → Vay tín chấp
≥700 (Tốt) + no category             → Vay mua ô tô
500-699 (Trung bình) + any           → Vay tín chấp
<500 (Cần thận trọng)                → No recommendation
```

### 11.4 OCR Service

- Model: `google/gemma-4-31b-it` (vision)
- Input: base64 image (multipart upload, max 5MB frontend / 10MB backend)
- Output: JSON {fullName, idNumber, dob, gender, address, issueDate, issuePlace}
- If phone provided → save to Customer table

### 11.5 Email Service

- Interface: `EmailService`
- SMTP: `SmtpEmailService` (@ConditionalOnProperty spring.mail.host)
- Mock: `MockEmailService` (@ConditionalOnMissingBean — fallback when no SMTP)
- Claim approve: use customer.email if available, fallback `{phone}@sms.msb.demo`
- All emails logged to `email_log` (claimId, toEmail, subject, body, mock flag)

---

## 12. Environment Variables

| Variable | Default | Required | Purpose |
|---|---|---|---|
| `GREENNODE_API_KEY` | — | Yes | LLM authentication |
| `LLM_BASE_URL` | `https://maas-llm-aiplatform-hcm.api.vngcloud.vn/v1` | No | LLM endpoint |
| `LLM_MODEL` | `qwen/qwen3.6-flash` | No | Chat model |
| `LLM_MOCK` | `false` | No | Mock mode (regex-based when LLM down) |
| `DASHBOARD_API_KEY` | `demo-key` | No | Protects leads endpoint |
| `POSTGRES_USER` | `postgres` | No | DB user |
| `POSTGRES_PASSWORD` | `postgres` | No | DB password |
| `POSTGRES_DB` | `supsale` | No | DB name |
| `DATABASE_URL` | `jdbc:postgresql://db:5432/supsale` | No | JDBC connection |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | No | CORS whitelist |
| `JWT_SECRET` | (change me) | Yes | JWT signing secret |
| `JWT_EXPIRATION_MS` | `3600000` | No | Token expiry (1h) |
| `SMTP_HOST` | (empty) | No | SMTP server (empty = mock) |
| `SMTP_PORT` | `587` | No | SMTP port |
| `SMTP_USER` | (empty) | No | SMTP username |
| `SMTP_PASS` | (empty) | No | SMTP password |
| `MAIL_FROM` | `noreply@sup-sale.msb.demo` | No | From email |
| `ZALO_BOT_TOKEN` | (empty) | No | Zalo Send API |
| `ZALO_OA_SECRET_KEY` | (empty) | No | Zalo webhook signature |

---

## 13. Deployment

### 13.1 Docker Compose (local demo)

```bash
cp .env.example .env  # Fill GREENNODE_API_KEY + JWT_SECRET
docker compose up -d --build
```

| Service | Port | Healthcheck | Depends On |
|---|---|---|---|
| db (postgres:15-alpine) | 5432 | pg_isready (5s, 10 retries) | — |
| backend (Spring Boot) | 8080 | curl /actuator/health (10s, 60s start) | db (healthy) |
| frontend (Vite dev) | 5173 | — | backend (healthy) |

### 13.2 GreenNode Platform (optional)

- `deploy/Dockerfile` — Python image with `greennode-agentbase` SDK
- Deploy as Custom Agent Runtime on GreenNode AgentBase
- Platform injects IAM credentials automatically
- See `deploy/README.md` for step-by-step guide

---

## 14. Demo Scenarios

### Scenario 1: Customer → Lead → Sale → CC (full flow)

```
1. Customer opens http://localhost:5173
   → MSB homepage → click 💬

2. Customer chats: "mình muốn vay mua ô tô"
   → AI asks for name + phone
   → Customer: "Nguyễn Văn A, 0912345678"
   → AI calls captureLead → lead saved → "✓ Lead captured"

3. Sale logs in (sale01/Sale@123)
   → /sale/upsale → sees new lead in list
   → Click lead → sees chat transcript
   → Click "Đã liên hệ" → badge CONTACTED
   → Click "Đã chuyển đổi" → badge CONVERTED

4. Sale uses 🛠️ ChatWidget → type CCCD "002201008550"
   → CIC lookup: score 750, Nhóm 1, Tốt
   → Customer: Nguyễn Văn Demo
   → Product recommendation: Thẻ tín dụng

5. If customer complained in chat → Claim PENDING
   → CC logs in (cc01/Cc@12345)
   → /cc → sees PENDING claim
   → Click → view chat history + suggested response
   → Edit response → Approve → email sent (mock) → APPROVED
```

### Scenario 2: OCR CCCD

```
1. Customer chats with AI
2. Click 📎 → upload CCCD photo (max 5MB)
3. Gemma 4 vision OCR extracts:
   - Họ tên, Số CCCD, Ngày sinh, Giới tính, Địa chỉ
4. Info saved to Customer table
5. "✓ CCCD saved" badge in chat
6. Sale sees CCCD info in Lead table (joined by phone)
```

---

## 15. Future Roadmap (Designed For, Not Implemented)

```
Current                          Future
────────                        ─────────
Channel: Web ✅, Zalo ✅    →   Mobile App, Telegram, Facebook
AI: Single agent ✅         →   Multi-agent, RAG, Document OCR pipeline
Tools: Lead/Product/CIC ✅  →   CRM, Core Banking, Credit Scoring
Data: PostgreSQL ✅         →   Vector DB (RAG), Redis (cache), Kafka (events)
Auth: JWT ✅                →   OAuth2, SSO, API Gateway
Email: SMTP/Mock ✅         →   SMS, Push notifications
Deploy: Docker Compose ✅   →   Kubernetes, GreenNode AgentBase Runtime
Observability: Logging ✅   →   OpenTelemetry, Prometheus/Grafana
```

**Designed interfaces for swap:**
- `LlmClient` → any LLM provider
- `AgentTool` → add tools without changing orchestrator
- `CicService` → swap mock for real CIC API
- `EmailService` → swap mock for SMTP/SES/SendGrid
- `OcrService` → swap Gemma for Tesseract/Google Vision

---

## 16. Project Structure (Current)

```
msb-hackathon-sup-sale/
├── .env / .env.example
├── docker-compose.yml
├── PROJECT_OVERVIEW.md / HANDOFF_NOTES.md / BRD.md (this file)
│
├── backend/                          # Spring Boot (Java 21)
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/java/com/msb/supsale/
│       ├── SupSaleApplication.java
│       ├── config/                   # GreenNodeConfig, CorsConfig, DataSeeder
│       ├── security/                 # JwtService, JwtAuthFilter, SecurityConfig, UserDetailsServiceImpl
│       ├── controller/               # Chat, Lead, OCR, Auth, Sale, SaleLookup, CC, Admin, AdminConversation, Feedback, Zalo, StaffChat
│       ├── agent/                    # AgentOrchestrator, StaffAgentOrchestrator, AgentTool
│       │   └── tools/                # LeadTool, ProductTool, CustomerTool, CicTool
│       ├── llm/                      # LlmClient, GreenNodeClient
│       ├── service/                  # Lead, Product, Customer, Conversation, Feedback, Ocr, Claim, Email(SMTP+Mock), Transaction, SaleActivity, Cic(Mock), ProductRecommendation
│       ├── repository/               # 12 JPA repositories
│       ├── model/                    # 12 JPA entities
│       ├── dto/                      # ChatRequest/Response, LeadDto, CccdDto, LoginRequest/Response, CreateUserRequest, UserDto, ConversationDto, FeedbackRequest, ApiError
│       ├── exception/                # GlobalExceptionHandler
│       └── util/                     # PhoneValidator
│
├── frontend/                         # React + TypeScript + Vite
│   ├── Dockerfile
│   ├── package.json
│   └── src/
│       ├── App.tsx                   # Router (4 workspaces)
│       ├── tokens.css                # Design system CSS variables
│       ├── App.css                   # All styles using tokens
│       ├── contexts/                 # AuthContext, ToastContext
│       ├── components/               # ChatWidget, ProtectedRoute, ConfirmModal, Header, Dashboard, Metrics, LeadTable
│       ├── pages/
│       │   ├── Landing.tsx           # Role selection
│       │   ├── Login.tsx             # JWT login
│       │   ├── CustomerView.tsx      # MSB homepage + ChatWidget
│       │   ├── ConversationHistory.tsx
│       │   ├── AdminUsers.tsx        # User CRUD + filter
│       │   ├── sale/                 # SaleLayout, SaleUpSale, SaleTransactions, SaleLookup, SaleKpi
│       │   └── cc/                   # CcClaims
│       ├── services/api.ts           # All API calls
│       └── types/index.ts            # TypeScript interfaces
│
├── deploy/                           # GreenNode platform (optional)
└── greennode-agentbase-skills/       # Framework SKILL.md (read-only)
```

---

*Document version: Phase 8 — reflects current codebase state after 8 phases of development.*
