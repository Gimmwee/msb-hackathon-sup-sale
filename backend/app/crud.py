from sqlalchemy import select
from sqlalchemy.dialects.postgresql import insert as pg_insert
from sqlalchemy.ext.asyncio import AsyncSession

from .models import ChatHistory, CustomerLead


async def save_chat(
    db: AsyncSession,
    session_id: str,
    platform: str,
    user_message: str,
    agent_response: str,
) -> ChatHistory:
    row = ChatHistory(
        session_id=session_id,
        platform=platform,
        user_message=user_message,
        agent_response=agent_response,
    )
    db.add(row)
    await db.commit()
    return row


async def get_recent_chat(db: AsyncSession, session_id: str, limit: int = 10) -> list[dict]:
    result = await db.execute(
        select(ChatHistory)
        .where(ChatHistory.session_id == session_id)
        .order_by(ChatHistory.created_at.desc())
        .limit(limit)
    )
    rows = list(reversed(result.scalars().all()))
    messages: list[dict] = []
    for r in rows:
        messages.append({"role": "user", "content": r.user_message})
        messages.append({"role": "assistant", "content": r.agent_response})
    return messages


async def upsert_lead(
    db: AsyncSession,
    session_id: str,
    name: str,
    phone: str,
    product_interest: str,
) -> CustomerLead:
    stmt = pg_insert(CustomerLead).values(
        session_id=session_id,
        name=name,
        phone=phone,
        product_interest=product_interest,
        status="new",
    )
    stmt = stmt.on_conflict_do_update(
        constraint="uq_session_phone",
        set_={
            "name": stmt.excluded.name,
            "product_interest": stmt.excluded.product_interest,
        },
    ).returning(CustomerLead)
    result = await db.execute(stmt)
    await db.commit()
    return result.scalar_one()


async def list_leads(db: AsyncSession) -> list[CustomerLead]:
    result = await db.execute(
        select(CustomerLead).order_by(CustomerLead.extracted_at.desc())
    )
    return list(result.scalars().all())
