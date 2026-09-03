# sup-sale — AI Agent tư vấn khách hàng (MSB AI Hackathon 2026)

AI Agent "sup-sale" tư vấn khách hàng, tự động trích xuất thông tin lead (Tên, SĐT, Nhu cầu sản phẩm) và lưu vào PostgreSQL. Giao tiếp qua **Web Dashboard (Streamlit)** và **Zalo Bot**. Lõi agent dùng pattern framework `greennode-agentbase-skills` (LangChain `create_agent` + `@tool` + `ChatOpenAI` → GreenNode MaaS, endpoint OpenAI-compatible).

## Kiến trúc

```
┌──────────────┐   POST /api/chat        ┌──────────────┐    LangChain agent    ┌───────────────┐
│  Streamlit   │ ─────────────────────▶  │   FastAPI    │ ───────────────────▶ │ GreenNode MaaS│
│  (frontend)  │   GET  /api/leads       │   (backend)  │   extract_and_save_  │  (LLM, OpenAI │
│  :8501       │ ◀─────────────────────  │   :8000      │   lead tool ──▶ DB   │  compatible)  │
└──────────────┘   X-API-Key             └──────┬───────┘                      └───────────────┘
                                                │ async SQLAlchemy (asyncpg)
                                                ▼
                                        ┌──────────────┐
                                        │ PostgreSQL   │  chat_history + customer_leads
                                        │  :5432       │
                                        └──────────────┘
Zalo OA ──webhook──▶ POST /api/webhook/zalo (verify HMAC-SHA256) ──▶ cùng pipeline agent (platform=zalo) ──▶ Zalo Send API
```

**Về framework `greennode-agentbase-skills`:** thư mục đó là bộ SKILL.md (slash-command skills cho Claude Code/Codex) điều khiển lifecycle platform GreenNode AgentBase, **không phải** package Python chứa base class. Interface agent thật được trích xuất từ template chính thức `skills/agentbase-wizard/assets/langchain_main.py`:
- LLM: `ChatOpenAI(model, base_url, api_key)` từ `langchain_openai` (OpenAI-compatible, trỏ tới GreenNode MaaS).
- Tool: decorator `@tool` từ `langchain_core.tools`, đăng ký qua `create_agent(llm, tools=[...], system_prompt=...)` từ `langchain.agents`.
- HTTP platform wrapper: `GreenNodeAgentBaseApp` từ `greennode_agentbase` (chỉ dùng trong `backend/main_agentbase.py` — đường deploy lên AgentBase Runtime; backend FastAPI dùng pattern agent làm lõi, theo lựa chọn kiến trúc đã chốt).

## Cấu trúc thư mục

```
msb-hackathon-sup-sale/
├── backend/
│   ├── Dockerfile
│   ├── requirements.txt
│   ├── main_agentbase.py        # (tuỳ chọn) wrapper GreenNodeAgentBaseApp cho AgentBase Runtime
│   └── app/
│       ├── main.py              # FastAPI: lifespan, CORS, /health, /api/chat, /api/leads, /api/webhook/zalo
│       ├── config.py            # pydantic-settings (.env)
│       ├── database.py          # async engine + retry exponential backoff (5 lần / 3s)
│       ├── models.py            # ChatHistory, CustomerLead (UUID PK, gen_random_uuid)
│       ├── schemas.py           # Pydantic + validate_phone regex VN
│       ├── crud.py              # save_chat, get_recent_chat, upsert_lead (on conflict), list_leads
│       ├── agent.py             # Lõi agent: ChatOpenAI→GreenNode MaaS + tool extract_and_save_lead
│       └── zalo.py              # verify HMAC-SHA256 + parse event + send message
├── frontend/
│   ├── Dockerfile
│   ├── requirements.txt
│   └── app.py                   # Streamlit: sidebar chat + metric + bảng leads (autorefresh 5s)
├── scripts/
│   ├── list_models.py           # GET /v1/models → in ra id thật để điền LLM_MODEL
│   └── seed_demo_data.py        # chèn lead mẫu cho demo
├── docker-compose.yml
├── .env.example
└── greennode-agentbase-skills/  # framework SKILL.md (có sẵn, không sửa)
```

## Yêu cầu

- Docker + Docker Compose
- (Tuỳ chọn) Python 3.11+ để chạy script local

## 1. Cấu hình `.env`

```bash
cp .env.example .env
```

Sửa `.env`:

| Biến | Ý nghĩa |
|---|---|
| `GREENNODE_API_KEY` | API key GreenNode MaaS (OpenAI-compatible) |
| `LLM_BASE_URL` | Mặc định `https://maas-llm-aiplatform-hcm.api.vngcloud.vn/v1` |
| `LLM_MODEL` | **Model id thật** — xem bước 2 |
| `ZALO_BOT_TOKEN` | Access token Zalo OA (gửi tin nhắn) |
| `ZALO_OA_SECRET_KEY` | Secret key Zalo OA (verify webhook signature) |
| `DASHBOARD_API_KEY` | Khảu bảo vệ `GET /api/leads` (header `X-API-Key`) |
| `POSTGRES_USER/PASSWORD/DB` | Thông tin DB |
| `DATABASE_URL` | `postgresql+asyncpg://user:pass@db:5432/dbname` |
| `CORS_ALLOWED_ORIGINS` | Origin cho phép, phân tách bởi dấu phẩy |

## 2. Lấy model id thật điền vào `LLM_MODEL`

KHÔNG hardcode tên marketing. Chạy sau khi đã set `GREENNODE_API_KEY` và start backend:

```bash
docker compose up -d --build backend db
docker compose exec backend python /scripts/list_models.py
```

Script gọi `GET {LLM_BASE_URL}/models` và in ra danh sách `id`. Copy 1 id và điền vào `.env`:

```
LLM_MODEL=<id-thật-vừa-in>
```

> **Lỗi `getaddrinfo ENOTFOUND maas-llm-aiplatform-hcm.api.vngcloud.vn`**: đây là lỗi phân giải DNS của môi trường (thiếu VPN / DNS nội bộ / firewall). Code không sai — bạn cần sửa connectivity (VPN, DNS, proxy) rồi retry. Phần này bạn tự xử lý.

Restart backend sau khi đổi `.env`:

```bash
docker compose up -d --build backend
```

## 3. Chạy toàn bộ

```bash
docker compose up -d --build
```

- Frontend: http://localhost:8501
- Backend API: http://localhost:8000
- Health: http://localhost:8000/health

## 4. Seed data demo

```bash
docker compose exec backend python /scripts/seed_demo_data.py
```

Chèn 4 lead mẫu vào `customer_leads` để bảng dashboard không trống.

## 5. Demo flow

### Web Dashboard
1. Mở http://localhost:8501.
2. Sidebar trái: chat với "sup-sale". Ví dụ:
   - Bạn: "Chào shop, mình muốn vay tín chấp mua xe."
   - sup-sale: hỏi thêm tên + SĐT.
   - Bạn: "Mình tên Nguyễn Văn A, SĐT 0912345678."
   - sup-sale: đủ 3 trường → gọi tool `extract_and_save_lead` → lưu lead → xác nhận.
3. Khu vực chính: metric "Total Leads Captured" + bảng leads tự refresh mỗi 5 giây.

### Zalo Bot
1. Cấu hình Zalo OA Webhook trỏ tới `https://<domain-public>/api/webhook/zalo`, set `ZALO_OA_SECRET_KEY` và `ZALO_BOT_TOKEN`.
2. Khách nhắn tin qua Zalo OA → webhook (verify HMAC-SHA256) → pipeline agent (`platform=zalo`) → lưu lead + chat history → gửi phản hồi qua Zalo Send API.

## API endpoints

| Method | Path | Mô tả |
|---|---|---|
| GET | `/health` | Trạng thái backend + DB (Docker healthcheck) |
| POST | `/api/chat` | Body `{session_id, platform, message}` → `{reply}` |
| GET | `/api/leads` | Toàn bộ leads, header `X-API-Key` = `DASHBOARD_API_KEY` |
| POST | `/api/webhook/zalo` | Zalo OA webhook (verify signature) |

## Lõi agent (`backend/app/agent.py`)

- System prompt tiếng Việt, giọng tư vấn viên MSB, không bịa lãi suất/sản phẩm, chỉ gọi tool khi đủ 3 trường hợp lệ.
- Tool `extract_and_save_lead(name, phone, product_interest)`: validate phone regex `^(0|\+84)[3-9][0-9]{8}$`; nếu sai → trả message báo agent hỏi lại (không lưu rác); upsert theo `(session_id, phone)` tránh trùng lead trong cùng phiên.
- `session_id` truyền vào tool qua `contextvars` (set mỗi request).

## (Tuỳ chọn) Deploy lên GreenNode AgentBase Runtime

`backend/main_agentbase.py` wrap cùng lõi agent trong `GreenNodeAgentBaseApp` (port 8080, `/invocations` + `/health`). Dùng khi deploy lên platform AgentBase (không dùng cho docker-compose local). Nếu `pip install greennode-agentbase` thất bại, có thể bỏ package khỏi `backend/requirements.txt` — backend FastAPI vẫn chạy bình thường vì chỉ `main_agentbase.py` mới import SDK đó.

## Troubleshooting

| Triệu chứng | Nguyên nhân / Fix |
|---|---|
| `getaddrinfo ENOTFOUND maas-llm-aiplatform-hcm.api.vngcloud.vn` | DNS/mạng môi trường — cần VPN/DNS/proxy. Code không sai. |
| `/api/chat` trả 502 | `LLM_MODEL` trống hoặc sai id, hoặc API key sai. Chạy `list_models.py` lại. |
| Bảng leads trống | Chạy `seed_demo_data.py`, hoặc chat cung cấp đủ tên+SĐT+nhu cầu. |
| `GET /api/leads` 401 | `X-API-Key` không khớp `DASHBOARD_API_KEY`. |
| Backend healthcheck fail | Đợi `start_period: 30s` (DB retry + tạo bảng). `docker compose logs backend`. |
