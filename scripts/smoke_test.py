"""Smoke test end-to-end: health -> leads -> chat (thiếu trường) -> chat (đủ trường) -> lead xuất hiện.

Chạy trong backend container:
    docker compose exec backend python /scripts/smoke_test.py
Hoặc host (backend đã expose :8000):
    python scripts/smoke_test.py

Cần set DASHBOARD_API_KEY (đọc từ .env). Hoạt động ở cả 2 mode: LLM_MOCK=true hoặc LLM thật.
"""
import asyncio
import os
import sys

from dotenv import load_dotenv

load_dotenv()

import httpx

BASE = os.environ.get("API_BASE_URL", "http://localhost:8000")
API_KEY = os.environ.get("DASHBOARD_API_KEY", "")
SESSION = "smoke-test"
PHONE = "0912345678"


def step(ok, name, detail=""):
    tag = "PASS" if ok else "FAIL"
    print(f"[{tag}] {name}" + (f" — {detail}" if detail else ""))
    return 0 if ok else 1


async def main() -> int:
    fails = 0
    print(f"Target: {BASE} | DASHBOARD_API_KEY={'set' if API_KEY else 'EMPTY'}\n")
    async with httpx.AsyncClient(timeout=60.0) as c:
        r = await c.get(f"{BASE}/health")
        body = r.json() if r.status_code == 200 else {}
        fails += step(r.status_code == 200 and body.get("db") is True, "health", f"{r.status_code} {body}")

        r = await c.get(f"{BASE}/api/leads", headers={"X-API-Key": API_KEY})
        before = len(r.json()) if r.status_code == 200 else None
        fails += step(r.status_code == 200, "leads(before)", f"status={r.status_code} count={before}")

        r = await c.post(
            f"{BASE}/api/chat",
            json={"session_id": SESSION, "platform": "web", "message": "chào shop"},
        )
        reply_missing = r.json().get("reply", "") if r.status_code == 200 else ""
        fails += step(r.status_code == 200 and bool(reply_missing), "chat(thiếu trường)", reply_missing[:90])

        r = await c.post(
            f"{BASE}/api/chat",
            json={
                "session_id": SESSION,
                "platform": "web",
                "message": f"mình tên Nguyễn Smoke Test, SĐT {PHONE}, muốn vay tín chấp mua xe",
            },
        )
        reply_full = r.json().get("reply", "") if r.status_code == 200 else ""
        fails += step(r.status_code == 200, "chat(đủ trường)", reply_full[:90])

        r = await c.get(f"{BASE}/api/leads", headers={"X-API-Key": API_KEY})
        after = r.json() if r.status_code == 200 else []
        found = any(l.get("phone") == PHONE and l.get("session_id") == SESSION for l in after)
        fails += step(found, "lead xuất hiện trong DB", f"count={len(after)} found={found}")

    print(f"\n{'✓ ALL PASS' if fails == 0 else f'✗ {fails} STEP(S) FAILED'}")
    return 1 if fails else 0


if __name__ == "__main__":
    sys.exit(asyncio.run(main()))
