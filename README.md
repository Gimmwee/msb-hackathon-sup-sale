# sup-sale — MSB AI Customer Assistant (Hackathon 2026)

AI Agent tư vấn khách hàng MSB, tự động trích xuất lead (Tên, SĐT, Nhu cầu) và lưu vào PostgreSQL.

## Stack

- **Frontend**: React + TypeScript + Vite
- **Backend**: Java 21 + Spring Boot 3.3
- **AI**: GreenNode MaaS (OpenAI-compatible, tool calling)
- **DB**: PostgreSQL
- **Deploy**: Docker Compose

## Architecture

```
React → POST /api/v1/chat → Spring Boot → AgentOrchestrator → GreenNode MaaS
                                        ↓ tool calls
                                   LeadTool / ProductTool / CustomerTool
                                        ↓
                                   Service → Repository → PostgreSQL
```

Agent KHÔNG trực tiếp access DB. Tool → Service → Repository → DB.

## Quick Start

```bash
cp .env.example .env  # điền GREENNODE_API_KEY
docker compose up -d --build
```

- Frontend: http://localhost:5173
- Backend: http://localhost:8080
- Health: http://localhost:8080/actuator/health

## Demo Flow

1. Mở http://localhost:5173
2. Chat: "Chào shop, mình muốn vay mua ô tô"
3. Agent hỏi tên + SĐT
4. Chat: "Mình tên Nguyễn Văn A, SĐT 0912345678"
5. Agent gọi captureLead → lưu vào DB → "✓ Lead captured"
6. Dashboard bên phải tự refresh → hiện lead mới

## API

| Method | Path | Mô tả |
|---|---|---|
| POST | `/api/v1/chat` | `{sessionId, platform, message}` → `{sessionId, message, intent, leadCaptured}` |
| GET | `/api/v1/leads` | Danh sách leads (header `X-API-Key`) |
| GET | `/api/v1/leads/metrics` | Metrics (header `X-API-Key`) |
| POST | `/api/v1/feedback` | `{sessionId, rating, comment}` |
| POST | `/api/v1/webhooks/zalo` | Zalo OA webhook |
| GET | `/actuator/health` | Health check |

## Tools

| Tool | Chức năng |
|---|---|
| `captureLead` | Lưu lead (validate phone VN, upsert by session+phone) |
| `getProductInfo` | Tra sản phẩm MSB (không trả lãi suất) |
| `getCustomerProfile` | Tra khách hàng theo SĐT |

## Config (.env)

| Biến | Ý nghĩa |
|---|---|
| `GREENNODE_API_KEY` | API key GreenNode MaaS |
| `LLM_BASE_URL` | `https://maas-llm-aiplatform-hcm.api.vngcloud.vn/v1` |
| `LLM_MODEL` | Model id (vd `qwen/qwen3.6-flash`) |
| `DASHBOARD_API_KEY` | Bảo vệ GET /api/v1/leads |
| `DATABASE_URL` | `jdbc:postgresql://db:5432/supsale` |

## Lấy model id

```bash
docker compose exec backend curl -s -H "Authorization: Bearer $GREENNODE_API_KEY" \
  https://maas-llm-aiplatform-hcm.api.vngcloud.vn/v1/models
```

Chọn model có tool calling (vd `qwen/qwen3.6-flash`), điền vào `LLM_MODEL`.
