import hashlib
import hmac
import logging

import httpx

from .config import settings

logger = logging.getLogger("sup_sale.zalo")

ZALO_SEND_URL = "https://openapi.zalo.me/v2.0/oa/message"


def verify_zalo_signature(raw_body: bytes, signature: str) -> bool:
    if not settings.zalo_oa_secret_key or not signature:
        return False
    expected = hmac.new(
        settings.zalo_oa_secret_key.encode("utf-8"),
        raw_body,
        hashlib.sha256,
    ).hexdigest()
    return hmac.compare_digest(expected, signature)


def parse_zalo_event(body: dict) -> tuple[str, str] | None:
    event = body.get("event_name") or body.get("event")
    if event != "usersendmsg":
        return None
    sender = body.get("sender") or {}
    user_id = str(sender.get("id") or body.get("userid") or "")
    text = (body.get("message") or {}).get("text", "")
    if not user_id or not text:
        return None
    return user_id, text


async def send_zalo_message(user_id: str, text: str) -> dict:
    if not settings.zalo_bot_token:
        logger.warning("ZALO_BOT_TOKEN not set; skipping send to user %s", user_id)
        return {"error": "ZALO_BOT_TOKEN not set"}
    headers = {"access_token": settings.zalo_bot_token, "Content-Type": "application/json"}
    payload = {"recipient": {"user_id": user_id}, "message": {"text": text}}
    async with httpx.AsyncClient(timeout=10.0) as client:
        resp = await client.post(ZALO_SEND_URL, headers=headers, json=payload)
        logger.info("Zalo send response: %s %s", resp.status_code, resp.text)
        return resp.json()
