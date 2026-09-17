# Deploy lên GreenNode AgentBase Platform

## Yêu cầu

- **IAM credentials** đã có trong `.greennode.json` (client_id + client_secret từ BTC)
- **Docker** chạy
- **Bash** (Git Bash trên Windows, hoặc WSL)
- **Cloud PostgreSQL** accessible từ internet (vd Supabase, Neon, Aiven) — platform runtime cần DB URL công khai. Nếu chỉ demo chat không cần lưu lead, bỏ qua DB.

## Cách 1 — Dùng framework scripts (tự động)

```bash
# 0. Set IAM credentials (đã có trong .greennode.json)
export GREENNODE_CLIENT_ID="194bd51c-873c-4bd8-bd2d-f542033c346a"
export GREENNODE_CLIENT_SECRET="c1ac567f-cf31-4b90-94f6-f202a22fb686"

SCRIPTS=greennode-agentbase-skills/skills/agentbase/scripts

# 1. Build image (port 8080, GreenNodeAgentBaseApp)
docker build --platform linux/amd64 -f deploy/Dockerfile -t sup-sale-agent:latest .

# 2. Login vào AgentBase Container Registry (CR)
bash $SCRIPTS/cr.sh credentials docker-login

# 3. Lấy repo info
bash $SCRIPTS/cr.sh repo get
# → {"name":"<repo-name>","registryUrl":"vcr.vngcloud.vn",...}

# 4. Tag + push (thay <repo-name> từ bước 3)
docker tag sup-sale-agent:latest vcr.vngcloud.vn/<repo-name>/sup-sale-agent:latest
docker push vcr.vngcloud.vn/<repo-name>/sup-sale-agent:latest

# 5. List flavors (chọn 1)
bash $SCRIPTS/runtime.sh flavors

# 6. Check POC wallet
bash $SCRIPTS/billing.sh can-use-poc

# 7. Create runtime
bash $SCRIPTS/runtime.sh create \
  --name "sup-sale-agent" \
  --image "vcr.vngcloud.vn/<repo-name>/sup-sale-agent:latest" \
  --flavor "1x1-general" \
  --env-file .env \
  --poc true \
  --from-cr

# 8. Lấy endpoint URL
RUNTIME_ID=$(bash $SCRIPTS/runtime.sh list | jq -r '.listData[0].id')
bash $SCRIPTS/runtime.sh endpoints list $RUNTIME_ID

# 9. Test health
curl -s -o /dev/null -w "%{http_code}" "<endpoint-url>/health"

# 10. Test chat
curl -X POST "<endpoint-url>/invocations" \
  -H "Content-Type: application/json" \
  -d '{"message":"chào shop, mình muốn vay mua xe"}'
```

## Cách 2 — Dùng Console (web UI)

1. Build + push image (bước 1-4 ở trên).
2. Mở https://aiplatform.console.vngcloud.vn/agent-runtime?tab=runtime
3. **Create Runtime** → chọn image từ CR → chọn flavor → set env vars → create.
4. Đợi status **ACTIVE** → lấy endpoint URL.
5. Test: `curl <endpoint-url>/health` và `curl -X POST <endpoint-url>/invocations -H "Content-Type: application/json" -d '{"message":"chào"}'`

## Env vars cho platform

| Biến | Giá trị | Ghi chú |
|---|---|---|
| `GREENNODE_API_KEY` | API key từ BTC | LLM inference |
| `LLM_BASE_URL` | `https://maas-llm-aiplatform-hcm.api.vngcloud.vn/v1` | |
| `LLM_MODEL` | `qwen/qwen3.6-flash` | Model có tool calling |
| `DATABASE_URL` | `postgresql+asyncpg://...` | Cloud DB URL (cần accessible từ runtime) |
| `DASHBOARD_API_KEY` | `demo-key` | |

**Tự động inject** (KHÔNG set manually):
- `GREENNODE_CLIENT_ID`, `GREENNODE_CLIENT_SECRET`, `GREENNODE_AGENT_IDENTITY`, `GREENNODE_ENDPOINT_URL`

## Kiến trúc

```
Platform Endpoint (public URL)
    → /invocations  → GreenNodeAgentBaseApp → agent (LangChain + GreenNode MaaS)
    → /health       → 200 OK
```

Container listen port **8080** (bắt buộc). Agent core dùng cùng `app/agent.py` như backend FastAPI local.
