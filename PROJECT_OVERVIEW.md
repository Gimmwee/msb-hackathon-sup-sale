# sup-sale — Project Overview (for AI Assistants)

> This document provides a complete technical reference of the "sup-sale" project for AI assistants (ChatGPT, Claude, etc.) to understand the codebase, architecture, and current state.

---

## 1. Project Summary

**sup-sale** is an AI customer consultation agent for **MSB (Maritime Bank of Vietnam)**, built for the MSB AI Hackathon 2026. The agent chats with customers, understands their banking needs, asks for missing information, captures leads (name + phone + product interest), and optionally OCRs CCCD (Citizen ID Card) images to extract customer data — all powered by GreenNode MaaS LLM.

**Primary demo flow:**
1. Customer asks about a banking product (e.g., "I want a car loan")
2. AI understands intent, asks for name + phone
3. Customer provides info
4. AI validates, calls `captureLead` tool → saves to PostgreSQL
5. Dashboard updates in real-time with the new lead
6. Customer can also upload a CCCD photo → Gemma 4 vision model OCRs it → info saved to Customer table

---

## 2. Technology Stack

| Layer | Technology | Version |
|---|---|---|
| Frontend | React + TypeScript + Vite | React 18, Vite 5 |
| Backend | Java + Spring Boot | Java 21, Spring Boot 3.3.7 |
| AI / LLM | GreenNode MaaS (OpenAI-compatible) | qwen/qwen3.6-flash (chat), google/gemma-4-31b-it (vision/OCR) |
| Database | PostgreSQL | 15-alpine |
| Build (BE) | Maven | 3.9 |
| Build (FE) | npm | Node 20 |
| Deployment | Docker Compose | 3 services: db, backend, frontend |

---

## 3. Architecture

```
┌──────────────────────┐
│  React Frontend       │  http://localhost:5173
│  (TypeScript + Vite)  │
└──────────┬───────────┘
           │ HTTP (fetch)
           ▼
┌──────────────────────┐
│  Spring Boot Backend  │  http://localhost:8080
│  (Java 21)            │
│                       │
│  ┌─────────────────┐  │
│  │ Controllers      │  │  /api/v1/chat, /api/v1/leads, /api/v1/ocr/cccd, etc.
│  └────────┬────────┘  │
│           │            │
│  ┌────────▼────────┐  │
│  │ AgentOrchestrator│  │  Tool-calling loop (max 5 iterations)
│  └────────┬────────┘  │
│           │            │
│  ┌────────▼────────┐  │
│  │ GreenNodeClient  │  │  WebClient → POST /v1/chat/completions
│  │ (LlmClient)      │  │  OpenAI-compatible API with tools
│  └────────┬────────┘  │
│           │            │
│  ┌────────▼────────┐  │
│  │ Tools            │  │  LeadTool, ProductTool, CustomerTool
│  │ (AgentTool)      │  │  Each tool → Service → Repository → DB
│  └────────┬────────┘  │
│           │            │
│  ┌────────▼────────┐  │
│  │ OcrService       │  │  Gemma 4 vision model → extract CCCD info
│  └────────┬────────┘  │
│           │            │
│  ┌────────▼────────┐  │
│  │ Services         │  │  LeadService, ProductService, CustomerService,
│  │                  │  │  ConversationService, FeedbackService
│  └────────┬────────┘  │
│           │            │
│  ┌────────▼────────┐  │
│  │ Repositories     │  │  Spring Data JPA
│  └────────┬────────┘  │
│           │            │
│  ┌────────▼────────┐  │
│  │ PostgreSQL       │  │  6 tables: customers, products, leads,
│  │                  │  │  conversations, messages, feedback
│  └─────────────────┘  │
└───────────────────────┘
```

**Key architectural principle:** The LLM/Agent NEVER directly accesses the database. It only calls controlled tools, which call services, which call repositories, which access the DB. This makes the architecture extensible to enterprise systems (CRM, Core Banking, etc.) without changing the agent.

---

## 4. Project Structure

```
msb-hackathon-sup-sale/
│
├── .env                          # Live credentials (GreenNode API key, DB, etc.)
├── .env.example                  # Template (no secrets)
├── .greennode.json               # IAM credentials for GreenNode platform
├── .gitignore
├── docker-compose.yml            # 3 services: db, backend, frontend
├── README.md                     # Quick start guide
├── PROJECT_OVERVIEW.md           # This file
│
├── backend/                      # Spring Boot (Java 21)
│   ├── Dockerfile                # Multi-stage: Maven build → JRE Alpine
│   ├── pom.xml                   # Dependencies: Spring Web, WebFlux, JPA, Validation, Actuator
│   ├── main_agentbase.py         # [LEGACY] Python wrapper for GreenNode platform deploy
│   ├── requirements.txt          # [LEGACY] Python deps (not used by Spring Boot)
│   ├── app/                      # [LEGACY] Python FastAPI code (not used by Spring Boot)
│   │
│   └── src/main/
│       ├── resources/
│       │   └── application.yml   # Server config, DB, GreenNode, CORS, multipart
│       │
│       └── java/com/msb/supsale/
│           ├── SupSaleApplication.java          # @SpringBootApplication entry point
│           │
│           ├── config/
│           │   ├── GreenNodeConfig.java         # @ConfigurationProperties for greennode.*
│           │   ├── CorsConfig.java              # CORS filter (allowed origins from env)
│           │   └── DataSeeder.java              # CommandLineRunner: seeds 4 products, 2 customers, 3 leads
│           │
│           ├── controller/
│           │   ├── ChatController.java          # POST /api/v1/chat
│           │   ├── LeadController.java          # GET /api/v1/leads, GET /api/v1/leads/metrics
│           │   ├── OcrController.java           # POST /api/v1/ocr/cccd (multipart file upload)
│           │   ├── FeedbackController.java      # POST /api/v1/feedback
│           │   └── ZaloWebhookController.java   # POST /api/v1/webhooks/zalo
│           │
│           ├── agent/
│           │   ├── AgentTool.java               # Interface: getName, getDescription, getParametersSchema, execute
│           │   ├── AgentOrchestrator.java       # Core: system prompt, tool-calling loop, mock mode, intent detection
│           │   └── tools/
│           │       ├── LeadTool.java            # captureLead(name, phone, productInterest) → LeadService
│           │       ├── ProductTool.java         # getProductInfo(productName) → ProductService
│           │       └── CustomerTool.java        # getCustomerProfile(phone) → CustomerService
│           │
│           ├── llm/
│           │   ├── LlmClient.java               # Interface: chatCompletion(messages, tools) → JsonNode
│           │   └── GreenNodeClient.java         # Implementation: WebClient → GreenNode MaaS /chat/completions
│           │
│           ├── service/
│           │   ├── LeadService.java             # captureLead (upsert by session+phone), getAllLeads, count
│           │   ├── ProductService.java          # findByName, getAllProducts
│           │   ├── CustomerService.java         # findByPhone, saveOrUpdateFromCccd
│           │   ├── ConversationService.java     # ensureConversation, saveMessage, getHistory
│           │   ├── FeedbackService.java         # save feedback
│           │   └── OcrService.java              # Gemma 4 vision model → extract CCCD → save to Customer
│           │
│           ├── repository/
│           │   ├── LeadRepository.java          # findBySessionIdAndPhone, findAllByOrderByCreatedAtDesc
│           │   ├── ProductRepository.java       # findByNameContainingIgnoreCase
│           │   ├── CustomerRepository.java      # findByPhone
│           │   ├── ConversationRepository.java  # findBySessionId
│           │   ├── MessageRepository.java       # findBySessionIdOrderByCreatedAtAsc
│           │   └── FeedbackRepository.java
│           │
│           ├── model/                           # JPA Entities (UUID PKs, @PrePersist timestamps)
│           │   ├── Customer.java                # name, phone, email, idNumber, dob, gender, address
│           │   ├── Product.java                 # name, description, category, eligibilityInfo, requiredDocuments
│           │   ├── Lead.java                    # sessionId, customerName, phone, productInterest, status
│           │   ├── Conversation.java            # sessionId, platform
│           │   ├── Message.java                 # sessionId, role, content
│           │   └── Feedback.java                # sessionId, rating, comment
│           │
│           ├── dto/
│           │   ├── ChatRequest.java             # sessionId, platform, message (validated)
│           │   ├── ChatResponse.java            # sessionId, message, intent, leadCaptured
│           │   ├── LeadDto.java                 # Lead + CCCD fields (idNumber, dob, gender, address)
│           │   ├── CccdDto.java                 # fullName, idNumber, dob, gender, address, saved, message
│           │   ├── FeedbackRequest.java         # sessionId, rating (1-5), comment
│           │   └── ApiError.java                # timestamp, status, error, message, requestId
│           │
│           ├── exception/
│           │   └── GlobalExceptionHandler.java  # @RestControllerAdvice, handles validation + generic errors
│           │
│           └── util/
│               └── PhoneValidator.java          # Vietnamese phone regex: ^(0|\+84)[3-9][0-9]{8}$
│
├── frontend/                     # React + TypeScript + Vite
│   ├── Dockerfile                # Node 20 Alpine, Vite dev server
│   ├── package.json              # react, react-dom, @vitejs/plugin-react, typescript
│   ├── vite.config.ts            # Proxy /api → backend, VITE_API_BASE_URL for direct calls
│   ├── tsconfig.json
│   ├── tsconfig.node.json
│   ├── index.html                # MSB-branded favicon, title
│   │
│   └── src/
│       ├── main.tsx              # React root
│       ├── App.tsx               # Layout: Header + ChatPanel (left) + Dashboard (right)
│       ├── App.css               # MSB brand colors (#E31837), responsive layout
│       │
│       ├── components/
│       │   ├── Header.tsx        # MSB AI Customer Assistant branding
│       │   ├── ChatPanel.tsx     # Chat UI: messages, typing indicator, quick buttons, 📎 CCCD upload
│       │   ├── Dashboard.tsx     # Metrics + LeadTable, auto-refresh every 5s
│       │   ├── Metrics.tsx       # Total Leads, New, Contacted, Conversion Rate
│       │   └── LeadTable.tsx     # Name, Phone, Product, CCCD, Address, Status, Created At
│       │
│       ├── services/
│       │   └── api.ts            # sendChat, uploadCccd (FormData), getLeads, getMetrics, sendFeedback
│       │
│       └── types/
│           └── index.ts          # ChatRequest, ChatResponse, Lead, Metrics, FeedbackRequest, CccdInfo, ChatMessage
│
├── deploy/                       # GreenNode platform deployment (optional)
│   ├── Dockerfile                # Python image with greennode-agentbase for AgentBase Runtime
│   ├── requirements.txt          # greennode-agentbase + langchain (for platform deploy only)
│   └── README.md                 # Step-by-step deploy guide
│
├── scripts/                      # [LEGACY] Python utility scripts
│   ├── list_models.py            # GET /v1/models to print available model IDs
│   ├── seed_demo_data.py         # Insert demo leads (Python, for old FastAPI backend)
│   └── smoke_test.py             # End-to-end test (Python, for old FastAPI backend)
│
└── greennode-agentbase-skills/   # Framework SKILL.md files (read-only, not modified)
    └── skills/                   # agentbase, agentbase-deploy, agentbase-llm, etc.
```

> **Note:** `backend/app/` and `backend/main_agentbase.py` are **legacy Python files** from the original FastAPI implementation. They are NOT used by the current Spring Boot backend. The active backend is in `backend/src/main/java/`.

---

## 5. Backend Details (Spring Boot)

### 5.1 AgentOrchestrator — Core AI Logic

**File:** `backend/src/main/java/com/msb/supsale/agent/AgentOrchestrator.java`

This is the heart of the application. It:

1. **Receives** user message via `ChatController`
2. **Loads** conversation history from PostgreSQL (`ConversationService.getHistory`)
3. **Builds** messages array: `[system_prompt, ...history, user_message]`
4. **Defines** tools: `captureLead`, `getProductInfo`, `getCustomerProfile` (OpenAI function-calling format)
5. **Loops** (max 5 iterations):
   - Calls `GreenNodeClient.chatCompletion(messages, tools)`
   - If response has `tool_calls` → executes each tool → adds tool results to messages → loops
   - If response has `content` → returns as final reply
6. **Saves** user message + assistant reply to `messages` table
7. **Returns** `AgentResponse(message, intent, leadCaptured)`

**System Prompt (Vietnamese):**
- Role: "sup-sale", MSB bank consultant
- Rules: respond in Vietnamese, don't fabricate interest rates/loan approvals, call `captureLead` only when all 3 fields (name + phone + product interest) are valid, ask for missing info, mention CCCD upload option
- Phone format: Vietnamese (09xxxxxxxx, 03xxxxxxxx, etc.)

**Mock Mode (`LLM_MOCK=true`):**
- When GreenNode API is unavailable, the agent uses regex-based extraction
- Loads conversation history to combine info from multiple messages (e.g., name from message 1 + phone from message 2)
- Detects intent by keywords (xe → CAR_LOAN, nhà → HOME_LOAN, thẻ → CREDIT_CARD, etc.)
- Calls `LeadService.captureLead` directly (no LLM involved)

**Intent Detection:**
- Keyword-based: `CAR_LOAN`, `HOME_LOAN`, `CREDIT_CARD`, `UNSECURED_LOAN`, `SAVINGS`, `GENERAL`

### 5.2 GreenNodeClient — LLM Integration

**File:** `backend/src/main/java/com/msb/supsale/llm/GreenNodeClient.java`

- Implements `LlmClient` interface
- Uses Spring `WebClient` to call `POST {LLM_BASE_URL}/chat/completions`
- Sends: `model`, `messages`, `max_tokens=4096`, `temperature=1`, `top_p=0.95`, `tools` (if any), `tool_choice=auto`
- Returns: Jackson `JsonNode` (full response JSON)
- Logs: LLM call latency

**GreenNode MaaS Configuration:**
- Base URL: `https://maas-llm-aiplatform-hcm.api.vngcloud.vn/v1`
- API Key: from `GREENNODE_API_KEY` env var
- Model: from `LLM_MODEL` env var (currently `qwen/qwen3.6-flash`)
- OpenAI-compatible API (chat completions + function/tool calling)

**Available Models (on GreenNode MaaS):**
| Model ID | Type | Use Case |
|---|---|---|
| `qwen/qwen3.6-flash` | Chat + tool calling | Primary agent LLM |
| `z-ai/glm-5.2-hackathon` | Chat + tool calling | Alternative (has content moderation on phone numbers) |
| `google/gemma-4-31b-it` | Vision (text + image) | OCR for CCCD images |

### 5.3 Tools

Each tool implements the `AgentTool` interface:
```java
String getName();
String getDescription();
Map<String, Object> getParametersSchema();  // JSON Schema for OpenAI function calling
String execute(JsonNode arguments, String sessionId);
```

#### LeadTool (`captureLead`)
- **Parameters:** `customerName` (string, required), `phone` (string, required), `productInterest` (string, required)
- **Validation:** Vietnamese phone regex `^(0|\+84)[3-9][0-9]{8}$`
- **Normalization:** `+84` → `0`, strip spaces/dashes
- **Upsert:** `findBySessionIdAndPhone` → update if exists, create if new
- **Returns:** `{"success":true,"leadId":"...","message":"..."}` or `{"success":false,"reason":"INVALID_PHONE"}`

#### ProductTool (`getProductInfo`)
- **Parameters:** `productName` (string, required)
- **Search:** `findByNameContainingIgnoreCase` in PostgreSQL
- **Returns:** `{"found":true,"name":"...","description":"...","eligibility":"...","documents":"..."}` or `{"found":false,...}`
- **Guardrail:** Does NOT return interest rates (not stored in DB)

#### CustomerTool (`getCustomerProfile`)
- **Parameters:** `phone` (string, required)
- **Search:** `findByPhone` in Customer table
- **Returns:** `{"found":true,"name":"...","phone":"..."}` or `{"found":false,...}`

### 5.4 OcrService — CCCD Image OCR

**File:** `backend/src/main/java/com/msb/supsale/service/OcrService.java`

- Uses `google/gemma-4-31b-it` (vision model) via GreenNode MaaS
- Receives base64 image from `OcrController`
- Sends to Gemma with a structured prompt asking for JSON output:
  ```json
  {"fullName":"...","idNumber":"...","dob":"...","gender":"...","address":"...","issueDate":"...","issuePlace":"..."}
  ```
- Parses the response (handles markdown code blocks ```json ... ```)
- If phone is provided → saves extracted info to `Customer` table via `CustomerService.saveOrUpdateFromCccd`
- Returns `CccdDto` with extracted fields + `saved` flag + `message`

### 5.5 Database Schema

6 JPA entities with UUID primary keys (`@GeneratedValue`), auto-timestamps (`@PrePersist`/`@PreUpdate`):

| Table | Fields | Notes |
|---|---|---|
| `customers` | id, name, phone, email, idNumber, dob, gender, address, createdAt, updatedAt | CCCD fields nullable (only filled after OCR) |
| `products` | id, name, description, category, eligibilityInfo, requiredDocuments, createdAt | Seeded: 4 products (Vay tín chấp, Vay mua ô tô, Vay mua nhà, Thẻ tín dụng) |
| `leads` | id, sessionId, customerName, phone, productInterest, status, createdAt, updatedAt | Unique constraint: (sessionId, phone) — prevents duplicate leads per session |
| `conversations` | id, sessionId, platform, createdAt, updatedAt | One per session |
| `messages` | id, sessionId, role, content, createdAt | Chat history (user/assistant) |
| `feedback` | id, sessionId, rating, comment, createdAt | Customer rating (1-5) |

**Seed Data (auto-inserted on startup via `DataSeeder`):**
- 4 products: Vay tín chấp, Vay mua ô tô, Vay mua nhà, Thẻ tín dụng
- 2 demo customers
- 3 demo leads

### 5.6 REST API Endpoints

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/v1/chat` | None | `{sessionId, platform, message}` → `{sessionId, message, intent, leadCaptured}` |
| GET | `/api/v1/leads` | `X-API-Key` header | Returns all leads with CCCD info (joined from Customer table) |
| GET | `/api/v1/leads/metrics` | `X-API-Key` header | `{totalLeads, newLeads, contactedLeads}` |
| POST | `/api/v1/ocr/cccd` | None | Multipart file upload → OCR → `{fullName, idNumber, dob, gender, address, saved, message}` |
| POST | `/api/v1/feedback` | None | `{sessionId, rating, comment}` → `{status: "success"}` |
| POST | `/api/v1/webhooks/zalo` | None | Zalo OA webhook adapter → same agent pipeline |
| GET | `/actuator/health` | None | Spring Boot Actuator health (DB + disk + ping) |

**Error Format (GlobalExceptionHandler):**
```json
{
  "timestamp": "2026-09-15T...",
  "status": 400,
  "error": "INVALID_REQUEST",
  "message": "sessionId: must not be blank",
  "requestId": "uuid"
}
```

---

## 6. Frontend Details (React + TypeScript + Vite)

### 6.1 Layout

```
┌─────────────────────────────────────────────────────┐
│ Header: "MSB AI Customer Assistant" | "sup-sale"     │
├──────────────────────┬──────────────────────────────┤
│                      │                              │
│  Chat Panel (left)   │  Dashboard (right)           │
│  ~42% width          │  ~58% width                  │
│                      │                              │
│  ┌────────────────┐  │  ┌────────────────────────┐  │
│  │ Message list    │  │  │ Metrics (4 cards)      │  │
│  │ (scrollable)    │  │  │ Total | New | Contacted│  │
│  │                 │  │  │       | Conversion     │  │
│  │ User: right     │  │  └────────────────────────┘  │
│  │ AI: left        │  │                              │
│  │ ✓ Lead captured │  │  ┌────────────────────────┐  │
│  │ ✓ CCCD saved    │  │  │ Lead Table             │  │
│  └────────────────┘  │  │ Name|Phone|Product|CCCD │  │
│                      │  │     |Address|Status|Time│  │
│  Quick buttons:      │  └────────────────────────┘  │
│  "Vay mua ô tô"      │                              │
│  "Vay tín chấp"      │  Auto-refresh: every 5s     │
│  "Mở thẻ tín dụng"   │                              │
│                      │                              │
│  ┌────────────────┐  │                              │
│  │ 📎 [input...]  │  │                              │
│  │    [Gửi]       │  │                              │
│  └────────────────┘  │                              │
└──────────────────────┴──────────────────────────────┘
```

### 6.2 Components

| Component | File | Responsibility |
|---|---|---|
| `App` | `App.tsx` | Layout, session ID generation, lead capture callback |
| `Header` | `Header.tsx` | MSB branding, hackathon badge |
| `ChatPanel` | `ChatPanel.tsx` | Chat messages, typing indicator, quick buttons, 📎 file upload, phone extraction from chat |
| `Dashboard` | `Dashboard.tsx` | Metrics + LeadTable, auto-refresh every 5s via `setInterval` |
| `Metrics` | `Metrics.tsx` | 4 metric cards: Total Leads, New, Contacted, Conversion Rate |
| `LeadTable` | `LeadTable.tsx` | Table with CCCD columns (null shown as "—") |

### 6.3 API Layer

**File:** `frontend/src/services/api.ts`

All backend calls go through this layer:
- `sendChat(request)` → POST `/api/v1/chat`
- `uploadCccd(file, sessionId, phone)` → POST `/api/v1/ocr/cccd` (FormData/multipart)
- `getLeads()` → GET `/api/v1/leads` (with `X-API-Key` header)
- `getMetrics()` → GET `/api/v1/leads/metrics`
- `sendFeedback(request)` → POST `/api/v1/feedback`

**Configuration:**
- `API_BASE`: from `VITE_API_BASE_URL` env (set to `http://localhost:8080` in Docker for direct backend calls)
- `API_KEY`: from `VITE_DASHBOARD_API_KEY` env (for leads endpoint auth)
- **GREENNODE_API_KEY is NEVER exposed to the frontend** — only the backend uses it

### 6.4 Chat Panel Features

- **Typing indicator**: animated dots while waiting for LLM response
- **Quick demo buttons**: pre-filled messages for instant demo
- **📎 CCCD upload**: file picker → size check (max 5MB) → FormData upload → display extracted info
- **Lead captured badge**: green "✓ Lead captured" when `leadCaptured=true`
- **CCCD saved badge**: green "✓ CCCD saved" when OCR info saved
- **Phone auto-extraction**: regex matches phone from user messages → passed to OCR endpoint for customer matching
- **Conversation context**: `sessionId` maintained in `App.tsx` state, passed to all API calls

---

## 7. Configuration

### 7.1 Environment Variables (`.env`)

| Variable | Value | Used By |
|---|---|---|
| `GREENNODE_API_KEY` | `vn-IV7ESnJ_...` | Backend: LLM authentication |
| `LLM_BASE_URL` | `https://maas-llm-aiplatform-hcm.api.vngcloud.vn/v1` | Backend: LLM endpoint |
| `LLM_MODEL` | `qwen/qwen3.6-flash` | Backend: chat model (tool calling) |
| `LLM_MOCK` | `false` | Backend: mock mode (regex-based when LLM unavailable) |
| `DASHBOARD_API_KEY` | `demo-key` | Backend: protects GET /api/v1/leads |
| `POSTGRES_USER` | `postgres` | DB |
| `POSTGRES_PASSWORD` | `postgres` | DB |
| `POSTGRES_DB` | `supsale` | DB |
| `DATABASE_URL` | `jdbc:postgresql://db:5432/supsale` | Backend: JDBC connection |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173,http://localhost:3000` | Backend: CORS filter |
| `ZALO_BOT_TOKEN` | (empty) | Backend: Zalo Send API (future) |
| `ZALO_OA_SECRET_KEY` | (empty) | Backend: Zalo webhook signature verification (future) |

### 7.2 Frontend Environment (docker-compose)

| Variable | Value | Used By |
|---|---|---|
| `VITE_API_BASE_URL` | `http://localhost:8080` | Frontend: direct backend URL (bypasses Vite proxy for large uploads) |
| `VITE_DASHBOARD_API_KEY` | `demo-key` | Frontend: X-API-Key header for leads endpoint |
| `API_PROXY_TARGET` | `http://backend:8080` | Vite dev server proxy target (fallback) |

### 7.3 Spring Boot Config (`application.yml`)

- Server port: 8080
- Tomcat max-swallow-size: 20MB (for large uploads)
- Multipart max-file-size: 10MB, max-request-size: 12MB
- JPA: `ddl-auto: update` (auto-creates/updates tables)
- Actuator: health endpoint exposes DB status
- Logging: `com.msb.supsale` at INFO level

---

## 8. Docker Setup

### 8.1 docker-compose.yml

3 services:

| Service | Image | Port | Depends On | Healthcheck |
|---|---|---|---|---|
| `db` | postgres:15-alpine | 5432 | — | `pg_isready` (5s interval, 10 retries) |
| `backend` | Built from `backend/Dockerfile` | 8080 | db (healthy) | `curl /actuator/health` (10s interval, 60s start period) |
| `frontend` | Built from `frontend/Dockerfile` | 5173 | backend (healthy) | — |

### 8.2 Backend Dockerfile

Multi-stage build:
1. **Build stage**: `maven:3.9-eclipse-temurin-21` → `mvn package -DskipTests`
2. **Runtime stage**: `eclipse-temurin:21-jre-alpine` + curl → runs JAR

### 8.3 Frontend Dockerfile

- `node:20-alpine` → `npm install` → `npm run dev` (Vite dev server)
- Exposes port 5173

---

## 9. Demo Flow (Primary Acceptance Criteria)

### Scenario: Car Loan Lead Capture

1. **Open** http://localhost:5173
2. **Click** quick button: "Chào shop, mình muốn vay mua ô tô"
3. **AI responds** (LLM-driven, natural language):
   > "Chào anh/chị! Em có thể hỗ trợ tư vấn sản phẩm vay mua ô tô. Để tư vấn phù hợp, anh/chị cho em xin tên và số điện thoại nhé. Anh/chị cũng có thể tải ảnh CCCD để hệ thống tự trích xuất thông tin nhanh hơn nhé."
4. **Type**: "Mình tên Nguyễn Văn A, SĐT 0912345678"
5. **AI responds**:
   - LLM calls `captureLead` tool with `{customerName: "Nguyễn Văn A", phone: "0912345678", productInterest: "Vay mua ô tô"}`
   - Lead saved to PostgreSQL
   - Response: "Cảm ơn anh Nguyễn Văn A. Em đã ghi nhận nhu cầu vay mua ô tô..."
   - Badge: "✓ Lead captured"
6. **Dashboard** (right panel) auto-refreshes → new lead appears in table
7. **Optional**: Click 📎 → upload CCCD image → Gemma OCR extracts:
   - Họ tên, Số CCCD, Ngày sinh, Giới tính, Địa chỉ
   - Saved to Customer table → badge: "✓ CCCD saved"
   - Lead table shows CCCD columns filled

### Multi-turn Context

The agent remembers conversation history:
- Turn 1: "mình muốn vay mua xe" → AI asks for name + phone
- Turn 2: "mình tên Nguyễn Văn A" → AI asks for phone (remembers name)
- Turn 3: "SĐT 0912345678" → AI has all info → calls `captureLead` → lead saved

This works in both LLM mode (history sent to LLM) and mock mode (history combined for regex extraction).

---

## 10. GreenNode Platform Deployment (Optional)

The `deploy/` directory contains files for deploying the agent to the GreenNode AgentBase platform:

- `deploy/Dockerfile`: Python image with `greennode-agentbase` SDK, uses `main_agentbase.py` (legacy Python wrapper)
- `deploy/README.md`: Step-by-step guide for building, pushing to AgentBase Container Registry, and creating a runtime

**Note:** The platform deployment uses the legacy Python wrapper because the GreenNode AgentBase platform expects containers running `GreenNodeAgentBaseApp` (port 8080, `/invocations` + `/health`). The Spring Boot backend runs on port 8080 with `/api/v1/chat` + `/actuator/health` for the local Docker Compose demo.

---

## 11. Legacy Files (Not Used by Current Stack)

The following files remain from the original Python/FastAPI implementation and are **not used** by the current Spring Boot + React stack:

| Path | Status | Notes |
|---|---|---|
| `backend/app/` (all .py files) | Legacy | Original FastAPI backend (replaced by Spring Boot) |
| `backend/main_agentbase.py` | Legacy | Python wrapper for GreenNode platform deploy |
| `backend/requirements.txt` | Legacy | Python dependencies (Spring Boot uses pom.xml) |
| `frontend/app.py` | Legacy | Original Streamlit frontend (replaced by React) |
| `frontend/requirements.txt` | Legacy | Python dependencies for Streamlit |
| `scripts/list_models.py` | Legacy | Python script to list GreenNode models |
| `scripts/seed_demo_data.py` | Legacy | Python script to seed demo data (Spring Boot uses DataSeeder) |
| `scripts/smoke_test.py` | Legacy | Python end-to-end test |

These files can be safely deleted if cleanup is needed.

---

## 12. Known Issues & Notes

1. **GreenNode API intermittent**: The `/v1/chat/completions` endpoint sometimes returns "no Route matched with those values" (Kong gateway error). When this happens, set `LLM_MOCK=true` in `.env` and rebuild the backend.

2. **GLM-5.2 content moderation**: The `z-ai/glm-5.2-hackathon` model has content moderation that blocks tool calls containing phone numbers (`ModelArts.81011: Output text May contain sensitive information`). Use `qwen/qwen3.6-flash` instead for tool calling with personal data.

3. **OCR response format**: Gemma 4 returns JSON wrapped in markdown code blocks (```json ... ```). The `OcrService.extractJson()` method handles this by finding the first `{` and last `}`.

4. **Image upload size**: Frontend limits uploads to 5MB. Backend accepts up to 10MB (multipart). Images are sent as `FormData` (not base64 JSON) to avoid proxy body size issues.

5. **CORS**: Frontend sends directly to `http://localhost:8080` (set via `VITE_API_BASE_URL`), bypassing the Vite proxy. CORS is configured on the backend to allow `http://localhost:5173`.

6. **Hibernate DDL**: `spring.jpa.hibernate.ddl-auto=update` auto-creates/updates tables on startup. For production, use Flyway/Liquibase migrations.

7. **No authentication on chat endpoint**: `POST /api/v1/chat` is open (no auth). Only `GET /api/v1/leads` requires `X-API-Key` header. This is acceptable for a hackathon MVP.

---

## 13. How to Run

```bash
# 1. Configure
cp .env.example .env
# Edit .env: set GREENNODE_API_KEY, LLM_MODEL

# 2. Start
docker compose up -d --build

# 3. Access
# Frontend: http://localhost:5173
# Backend:  http://localhost:8080
# Health:   http://localhost:8080/actuator/health

# 4. Test
# Open http://localhost:5173, chat with the AI agent
```

---

## 14. Future Architecture (Designed For, Not Implemented)

The current architecture has clear boundaries for future expansion:

```
Channel Layer:     Web (✅) → Zalo (✅ adapter) → Mobile App → Internal Portal
API Layer:         REST (✅) → API Gateway → Auth
AI Agent:          AgentOrchestrator (✅) → Multi-agent → RAG → Document OCR (✅)
Tool Layer:        LeadTool (✅) → CRM → Core Banking → Credit Scoring → Product Service (✅)
Data Layer:        PostgreSQL (✅) → Vector DB → Redis → Kafka
Observability:     Structured logging (✅) → OpenTelemetry → Prometheus/Grafana
```

**Designed interfaces for future:**
- `LlmClient` interface → swap GreenNode for any LLM provider
- `AgentTool` interface → add new tools without changing AgentOrchestrator
- `OcrService` → extensible to Tesseract, Google Vision, AWS Textract
- Zalo webhook adapter → pattern for adding Telegram, Facebook Messenger, etc.
