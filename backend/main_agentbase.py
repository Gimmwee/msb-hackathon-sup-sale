import asyncio
import logging
import os

from dotenv import load_dotenv

load_dotenv()

from app.agent import current_session_id, get_agent, run_agent
from app.crud import get_recent_chat, save_chat
from app.database import AsyncSessionLocal, connect_with_retry, engine
from app.models import Base
from greennode_agentbase import GreenNodeAgentBaseApp, PingStatus, RequestContext

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("sup_sale.agentbase")


DB_READY = False


def _ensure_tables():
    async def _run():
        global DB_READY
        ok = await connect_with_retry()
        if not ok:
            logger.warning("DB unavailable — running without persistence")
            return
        async with engine.begin() as conn:
            await conn.run_sync(Base.metadata.create_all)
        DB_READY = True
    asyncio.run(_run())


_ensure_tables()
get_agent()

app = GreenNodeAgentBaseApp()


async def _run(message: str, session_id: str) -> str:
    history: list[dict] = []
    if DB_READY:
        try:
            async with AsyncSessionLocal() as db:
                history = await get_recent_chat(db, session_id, 10)
        except Exception:
            pass
    messages = history + [{"role": "user", "content": message}]
    token = current_session_id.set(session_id)
    try:
        reply = await run_agent(messages)
    finally:
        current_session_id.reset(token)
    if DB_READY:
        try:
            async with AsyncSessionLocal() as db:
                await save_chat(db, session_id, "web", message, reply)
        except Exception:
            pass
    return reply


@app.entrypoint
def handler(payload: dict, context: RequestContext) -> dict:
    message = payload.get("message", "")
    session_id = context.session_id or payload.get("session_id", "default")
    reply = asyncio.run(_run(message, session_id))
    return {"reply": reply}


@app.ping
def health_check() -> PingStatus:
    return PingStatus.HEALTHY


if __name__ == "__main__":
    app.run(port=8080, host="0.0.0.0")
