import re

from pydantic import BaseModel

PHONE_RE = re.compile(r"^(0|\+84)[3-9][0-9]{8}$")


class ChatRequest(BaseModel):
    session_id: str
    platform: str = "web"
    message: str


class ChatResponse(BaseModel):
    reply: str


class LeadOut(BaseModel):
    id: str
    session_id: str
    name: str
    phone: str
    product_interest: str
    status: str
    extracted_at: str | None = None


def validate_phone(phone: str) -> bool:
    return bool(PHONE_RE.match(phone or ""))
