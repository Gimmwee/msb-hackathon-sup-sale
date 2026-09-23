# AGENTS.md — sup-sale Coding Conventions

Hướng dẫn cho AI agents (opencode, Claude, Cursor) khi làm việc trong project này.

## Project Overview

**sup-sale** — AI Agent tư vấn khách hàng MSB, tự động trích xuất lead (Tên, SĐT, Nhu cầu) qua chat.

## Architecture

```
Frontend (React + TypeScript + Vite)
    ↓ POST /api/v1/chat
Backend (Python LangChain / Java Spring Boot)
    ↓ AgentOrchestrator → GreenNode MaaS (LLM)
    ↓ tool calls
extract_and_save_lead → Service → Repository → PostgreSQL
```

**Nguyên tắc cốt lõi:** Agent KHÔNG trực tiếp access DB. Tool → Service → Repository → DB.

## Two Backend Variants

| Path | Stack | When |
|---|---|---|
| `backend/app/` + `backend/main_agentbase.py` | Python + LangChain + GreenNode AgentBase SDK | Deploy lên AgentBase runtime (cloud) |
| `backend/` (Java) | Java 21 + Spring Boot 3.3 | Local Docker Compose (port 8080) |

Both share the same agent logic (`app/agent.py`) and DB schema.

## Frontend Structure

```
frontend/src/
  pages/           → Route-level (CustomerView, Login, StaffDashboard, sale/, cc/)
  components/      → Reusable UI (ChatWidget, Dashboard, LeadTable, Metrics, Header)
  contexts/        → React Context (AuthContext, ToastContext)
  services/api.ts  → API client (fetch wrapper, auth headers)
  types/index.ts   → TypeScript interfaces
  App.css          → Global styles (MSB design tokens)
  tokens.css       → CSS variables (colors, radius, transitions)
```

## Coding Standards

### Frontend
- **State**: React hooks (`useState`, `useEffect`), Context cho auth/toast
- **Routing**: `react-router-dom` (NavLink, useNavigate, Outlet)
- **Styling**: CSS classes (KHÔNG Tailwind), design tokens trong `tokens.css`
- **Session**: `sessionStorage` cho customer (mỗi visit = 1 session mới), `localStorage` cho staff auth
- **API**: Tất cả gọi qua `services/api.ts`, auth header tự động từ `localStorage.getItem('token')`
- **TypeScript**: Strict types, interface cho mọi props, `type` cho union/intersection

### Backend (Python)
- **Agent**: LangChain `create_agent` + tool calling, `contextvars` cho session_id
- **DB**: SQLAlchemy async + asyncpg, `connect_with_retry()` với exponential backoff
- **Config**: Pydantic Settings (`config.py`), env-driven, `extra="ignore"`
- **Prompt**: `SYSTEM_PROMPT` trong `agent.py`, tiếng Việt, chống hallucination
- **Tools**: `@tool` decorator, docstring rõ ràng, validate input trước khi lưu

### Deploy
- **Local**: `docker compose up -d --build` (frontend:5173, backend:8080, postgres:5432)
- **Cloud**: Build từ `deploy/Dockerfile` → push AgentBase CR → `runtime.sh update`
- **Port**: Container phải listen 8080, health check `/health`
- **Env vars auto-injected** (KHÔNG set manually): `GREENNODE_CLIENT_ID`, `GREENNODE_CLIENT_SECRET`, `GREENNODE_AGENT_IDENTITY`, `GREENNODE_ENDPOINT_URL`

## Key Files

| File | Role |
|---|---|
| `backend/app/agent.py` | System prompt, tool definitions, agent creation |
| `backend/app/config.py` | Pydantic settings (env-driven) |
| `backend/app/database.py` | SQLAlchemy engine, connect_with_retry |
| `backend/app/crud.py` | DB operations (save_chat, get_recent_chat, upsert_lead) |
| `backend/main_agentbase.py` | GreenNode AgentBase entrypoint (port 8080) |
| `frontend/src/pages/CustomerView.tsx` | Customer homepage + chat |
| `frontend/src/components/ChatWidget.tsx` | Chat UI (customer + staff mode) |
| `frontend/src/services/api.ts` | API client |
| `deploy/Dockerfile` | Cloud deploy image |

## Commands

```bash
# Local dev
docker compose up -d --build

# Lint/typecheck frontend
cd frontend && npm run lint && npm run typecheck

# Deploy to AgentBase
docker build --platform linux/amd64 -f deploy/Dockerfile -t sup-sale:latest .
docker tag sup-sale:latest vcr.vngcloud.vn/<repo>/sup-sale:latest
docker push vcr.vngcloud.vn/<repo>/sup-sale:latest
bash greennode-agentbase-skills/skills/agentbase/scripts/runtime.sh update <RUNTIME_ID> \
  --image "vcr.vngcloud.vn/<repo>/sup-sale:latest" --flavor runtime-s2-general-2x4 --from-cr
```

## Anti-Hallucination Rules (Agent)

1. KHÔNG bịa lãi suất, hạn mức, phí — nói "em sẽ kiểm tra lại" nếu không chắc
2. Chỉ gọi `extract_and_save_lead` khi đủ 3 trường: name + phone (VN format) + product_interest
3. Validate phone trước khi lưu (`validate_phone` trong `schemas.py`)
4. Trả lời tiếng Việt, lịch sự, ngắn gọn
5. Tóm tắt + cảm ơn trước khi kết thúc hội thoại
