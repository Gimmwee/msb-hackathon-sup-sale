from sqlalchemy import Column, DateTime, String, Text, UniqueConstraint, func
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import DeclarativeBase


class Base(DeclarativeBase):
    pass


class ChatHistory(Base):
    __tablename__ = "chat_history"

    id = Column(UUID(as_uuid=True), primary_key=True, server_default=func.gen_random_uuid())
    session_id = Column(String(64), index=True, nullable=False)
    platform = Column(String(16), nullable=False)
    user_message = Column(Text, nullable=False)
    agent_response = Column(Text, nullable=False)
    created_at = Column(DateTime(timezone=True), server_default=func.now(), nullable=False)


class CustomerLead(Base):
    __tablename__ = "customer_leads"
    __table_args__ = (UniqueConstraint("session_id", "phone", name="uq_session_phone"),)

    id = Column(UUID(as_uuid=True), primary_key=True, server_default=func.gen_random_uuid())
    session_id = Column(String(64), index=True, nullable=False)
    name = Column(String(255), nullable=False)
    phone = Column(String(32), index=True, nullable=False)
    product_interest = Column(Text, nullable=False)
    status = Column(String(32), default="new", nullable=False)
    extracted_at = Column(DateTime(timezone=True), server_default=func.now(), nullable=False)
