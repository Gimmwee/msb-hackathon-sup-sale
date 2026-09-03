import asyncio
import logging

from sqlalchemy.ext.asyncio import AsyncSession, async_sessionmaker, create_async_engine
from sqlalchemy import text

from .config import settings

logger = logging.getLogger("sup_sale.db")

engine = create_async_engine(settings.database_url, pool_pre_ping=True, echo=False)
AsyncSessionLocal = async_sessionmaker(engine, class_=AsyncSession, expire_on_commit=False)


async def connect_with_retry(max_retries: int = 5, base_delay: float = 3.0) -> bool:
    last_exc: Exception | None = None
    for attempt in range(1, max_retries + 1):
        try:
            async with engine.connect() as conn:
                await conn.execute(text("SELECT 1"))
            logger.info("DB connected on attempt %d/%d", attempt, max_retries)
            return True
        except Exception as exc:
            last_exc = exc
            delay = base_delay * (2 ** (attempt - 1))
            logger.warning(
                "DB connect attempt %d/%d failed: %s — retrying in %.1fs",
                attempt, max_retries, exc, delay,
            )
            await asyncio.sleep(delay)
    logger.error("DB connect failed after %d attempts: %s", max_retries, last_exc)
    return False


async def get_db():
    async with AsyncSessionLocal() as session:
        yield session
