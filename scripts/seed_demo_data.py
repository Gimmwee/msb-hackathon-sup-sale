"""Insert demo customer leads so the dashboard table is not empty during demo.

Usage (inside backend container):
    docker compose exec backend python /scripts/seed_demo_data.py
"""
import asyncio
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
for candidate in ("/app", os.path.join(HERE, "..", "backend")):
    if os.path.isdir(os.path.join(candidate, "app")):
        if candidate not in sys.path:
            sys.path.insert(0, candidate)
        break

from dotenv import load_dotenv

load_dotenv()

from app.crud import upsert_lead
from app.database import AsyncSessionLocal, connect_with_retry, engine
from app.models import Base

DEMOS = [
    ("web-demo-1", "Nguyễn Văn An", "0912345678", "Vay tín chấp mua xe"),
    ("web-demo-2", "Trần Thị Bình", "0987654321", "Mở thẻ tín dụng MSB Mastercard"),
    ("zalo-demo-1", "Lê Hoàng Cường", "+84901234567", "Gửi tiết kiệm kỳ hạn 6 tháng"),
    ("web-demo-3", "Phạm Thị Dung", "0977123456", "Vay thế chấp bất động sản"),
]


async def main() -> int:
    ok = await connect_with_retry()
    if not ok:
        print("Cannot connect to DB.", file=sys.stderr)
        return 1
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)
    for session_id, name, phone, interest in DEMOS:
        async with AsyncSessionLocal() as db:
            lead = await upsert_lead(db, session_id, name, phone, interest)
        print(f"  seeded: {lead.name} | {lead.phone} | {lead.product_interest}")
    print(f"\nInserted/updated {len(DEMOS)} demo leads.")
    return 0


if __name__ == "__main__":
    sys.exit(asyncio.run(main()))
