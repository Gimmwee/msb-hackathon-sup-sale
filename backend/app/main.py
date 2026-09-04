import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI, Header, HTTPException, Request
from fastapi.middleware.cors import CORSMiddleware
from sqlalchemy import text

from .agent import current_session_id, get_agent, run_agent
from .config import settings
from .crud import get_recent_chat, list_leads, save_chat
from .database import AsyncSessionLocal, connect_with_retry, engine
from .models import Base
from .schemas import ChatRequest, ChatResponse
from .zalo import parse_zalo_event, send_zalo_message, verify_zalo_signature

logger = logging.getLogger("sup_sale")
logging.basicConfig(level=logging.INFO)


@asynccontextmanager
async def lifespan(app: FastAPI):
    ok = await connect_with_retry()
    if ok:
        async with engine.begin() as conn:
            await conn.run_sync(Base.metadata.create_all)
        logger.info("DB tables ensured.")
    if settings.llm_mock:
        logger.warning("LLM_MOCK=true — chạy mock agent (không gọi LLM/DNS). Chỉ cho demo/test.")
    else:
        if not settings.llm_model:
            logger.warning("LLM_MODEL is empty — /api/chat will fail until you set a real model id.")
        if not settings.greennode_api_key:
            logger.warning("GREENNODE_API_KEY is empty — LLM calls will be unauthorized.")
        try:
            get_agent()
        except Exception as exc:
            logger.warning("Agent build failed at startup (will retry on first chat): %s", exc)
    yield


app = FastAPI(title="sup-sale", version="1.0.0", lifespan=lifespan)

origins = [o.strip() for o in settings.cors_allowed_origins.split(",") if o.strip()]
app.add_middleware(
    CORSMiddleware,
    allow_origins=origins,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.get("/health")
async def health():
    db_ok = False
    try:
        async with AsyncSessionLocal() as db:
            await db.execute(text("SELECT 1"))
        db_ok = True
    except Exception as exc:
        logger.warning("health DB probe failed: %s", exc)
    return {"status": "healthy" if db_ok else "degraded", "db": db_ok}


@app.post("/api/chat", response_model=ChatResponse)
async def chat(req: ChatRequest):
    async with AsyncSessionLocal() as db:
        history = await get_recent_chat(db, req.session_id, settings.chat_history_limit)
    messages = history + [{"role": "user", "content": req.message}]

    token = current_session_id.set(req.session_id)
    try:
        reply = await run_agent(messages)
    except Exception as exc:
        logger.exception("agent invocation failed")
        raise HTTPException(status_code=502, detail=f"Agent error: {exc}")
    finally:
        current_session_id.reset(token)

    async with AsyncSessionLocal() as db:
        await save_chat(db, req.session_id, req.platform, req.message, reply)
    return ChatResponse(reply=reply)


@app.get("/api/leads")
async def get_leads(x_api_key: str = Header(default="", alias="X-API-Key")):
    if not settings.dashboard_api_key or x_api_key != settings.dashboard_api_key:
        raise HTTPException(status_code=401, detail="Invalid or missing API key")
    async with AsyncSessionLocal() as db:
        leads = await list_leads(db)
    return [
        {
            "id": str(l.id),
            "session_id": l.session_id,
            "name": l.name,
            "phone": l.phone,
            "product_interest": l.product_interest,
            "status": l.status,
            "extracted_at": l.extracted_at.isoformat() if l.extracted_at else None,
        }
        for l in leads
    ]


@app.post("/api/webhook/zalo")
async def zalo_webhook(request: Request):
    raw = await request.body()
    signature = (
        request.headers.get("X-ZEvent-Signature")
        or request.headers.get("x-zevent-signature")
        or ""
    )
    if not verify_zalo_signature(raw, signature):
        raise HTTPException(status_code=401, detail="Invalid webhook signature")

    try:
        body = await request.json()
    except Exception:
        raise HTTPException(status_code=400, detail="Invalid JSON body")

    parsed = parse_zalo_event(body)
    if not parsed:
        return {"status": "ignored"}
    user_id, text = parsed
    session_id = f"zalo:{user_id}"

    async with AsyncSessionLocal() as db:
        history = await get_recent_chat(db, session_id, settings.chat_history_limit)
    messages = history + [{"role": "user", "content": text}]

    token = current_session_id.set(session_id)
    try:
        reply = await run_agent(messages)
    except Exception as exc:
        logger.exception("agent invocation failed (zalo)")
        raise HTTPException(status_code=502, detail=f"Agent error: {exc}")
    finally:
        current_session_id.reset(token)

    async with AsyncSessionLocal() as db:
        await save_chat(db, session_id, "zalo", text, reply)
    await send_zalo_message(user_id, reply)
    return {"status": "ok"}
