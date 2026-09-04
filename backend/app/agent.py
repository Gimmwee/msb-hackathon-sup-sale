import contextvars
import logging
import re

from langchain.agents import create_agent
from langchain_core.tools import tool
from langchain_openai import ChatOpenAI

from .config import settings
from .crud import upsert_lead
from .database import AsyncSessionLocal
from .schemas import validate_phone

logger = logging.getLogger("sup_sale.agent")

current_session_id: contextvars.ContextVar[str | None] = contextvars.ContextVar(
    "current_session_id", default=None
)

SYSTEM_PROMPT = """Bạn là "sup-sale", tư vấn viên ngân hàng MSB thân thiện và chuyên nghiệp.

NHIỆM VỤ:
- Tư vấn, giải đáp thắc mắc sản phẩm/dịch vụ ngân hàng MSB.
- Trích xuất thông tin khách hàng tiềm năng (họ tên, số điện thoại, nhu cầu sản phẩm) và lưu vào hệ thống.

QUY TẮC:
- Luôn trả lời bằng tiếng Việt, lịch sự, gần gũi, ngắn gọn.
- KHÔNG tự bịa ra lãi suất, hạn mức, phí, hoặc sản phẩm không có trong dữ liệu được cung cấp. Nếu không chắc, nói rõ "em sẽ kiểm tra lại với cấp trên / tài liệu chính thức".
- Chỉ gọi tool `extract_and_save_lead` KHI CẢ 3 trường đều đủ và hợp lệ: họ tên, số điện thoại (định dạng VN: 09xxxxxxxx hoặc +849xxxxxxxx), và nhu cầu sản phẩm.
- Nếu thiếu bất kỳ trường nào, hoặc số điện thoại sai định dạng, hãy hỏi lại khách — KHÔNG gọi tool với dữ liệu rác.
- Trước khi kết thúc hội thoại, tóm tắt lại thông tin đã ghi nhận và cảm ơn khách.
"""


def build_llm() -> ChatOpenAI:
    return ChatOpenAI(
        model=settings.llm_model,
        base_url=settings.llm_base_url,
        api_key=settings.greennode_api_key,
    )


_MOCK_PHONE_RE = re.compile(r"(0|\+84)[3-9][0-9]{8}")
_MOCK_NAME_RE = re.compile(
    r"(?:mình tên|em tên|tôi tên|tui tên|tên)\s+([^,\.\d]+?)(?:\s*,|\s*\.|\s+\d|$)",
    re.IGNORECASE,
)
_MOCK_INTEREST_KEYWORDS = [
    ("vay tín chấp", "Vay tín chấp"),
    ("vay thế chấp", "Vay thế chấp"),
    ("vay mua xe", "Vay mua xe"),
    ("vay mua nhà", "Vay mua nhà"),
    ("vay", "Vay"),
    ("thẻ tín dụng", "Thẻ tín dụng"),
    ("thẻ", "Thẻ tín dụng"),
    ("tiết kiệm", "Gửi tiết kiệm"),
    ("mở sổ", "Gửi tiết kiệm"),
    ("chuyển tiền", "Chuyển tiền"),
    ("mua xe", "Vay mua xe"),
    ("mua nhà", "Vay mua nhà"),
]


def _mock_extract(text: str) -> tuple[str | None, str | None, str | None]:
    pm = _MOCK_PHONE_RE.search(text)
    phone = pm.group(0) if pm else None
    name = None
    nm = _MOCK_NAME_RE.search(text)
    if nm:
        parts = nm.group(1).strip().split()
        if parts:
            name = " ".join(parts[:4])
    low = text.lower()
    interest = None
    for kw, label in _MOCK_INTEREST_KEYWORDS:
        if kw in low:
            interest = label
            break
    if not interest:
        m = re.search(r"muốn\s+([^,\.\d]+?)(?:\s*,|\s*\.|$)", text, re.IGNORECASE)
        if m:
            interest = " ".join(m.group(1).strip().split()[:8]) or None
    return name, phone, interest


async def mock_run_agent(messages: list[dict]) -> str:
    last = ""
    for m in reversed(messages):
        if m.get("role") == "user":
            last = m.get("content", "")
            break
    name, phone, interest = _mock_extract(last)
    missing = []
    if not name:
        missing.append("họ tên")
    if not phone:
        missing.append("số điện thoại (vd 0912345678)")
    if not interest:
        missing.append("nhu cầu sản phẩm")
    if missing:
        return (
            "[mock] Chào bạn, em là sup-sale MSB. Để ghi nhận thông tin, em còn cần thêm: "
            + ", ".join(missing) + " ạ."
        )
    if not validate_phone(phone):
        return f"[mock] Số điện thoại {phone} không đúng định dạng VN, anh/chị kiểm tra lại giúp em nha."
    session_id = current_session_id.get()
    if not session_id:
        return "[mock] Không xác định được phiên hội thoại."
    async with AsyncSessionLocal() as db:
        lead = await upsert_lead(db, session_id, name, phone, interest)
    logger.info("[mock] Lead saved: session=%s phone=%s name=%s", session_id, phone, name)
    return (
        f"[mock] Đã lưu lead: {lead.name} - {lead.phone} - {lead.product_interest}. "
        f"Cảm ơn anh/chị, em sẽ liên hệ lại ạ!"
    )


@tool
async def extract_and_save_lead(name: str, phone: str, product_interest: str) -> str:
    """Trích xuất và lưu thông tin khách hàng tiềm năng (lead) vào database.
    Chỉ gọi khi đã có đủ họ tên, số điện thoại hợp lệ (VN), và nhu cầu sản phẩm.

    Args:
        name: Họ và tên đầy đủ của khách hàng.
        phone: Số điện thoại Việt Nam, định dạng 09xxxxxxxx hoặc +849xxxxxxxx.
        product_interest: Nhu cầu/sản phẩm khách quan tâm (vd: vay tín chấp, mở thẻ, gửi tiết kiệm).
    """
    if not validate_phone(phone):
        return (
            f"Số điện thoại '{phone}' không đúng định dạng VN "
            f"(0xx|+84xx + 8 số). Hãy hỏi khách lại số đúng trước khi lưu."
        )
    session_id = current_session_id.get()
    if not session_id:
        return "Không xác định được phiên hội thoại, không thể lưu lead."
    async with AsyncSessionLocal() as db:
        lead = await upsert_lead(db, session_id, name, phone, product_interest)
    logger.info("Lead saved/updated: session=%s phone=%s name=%s", session_id, phone, name)
    return (
        f"Đã lưu/cập nhật lead thành công: {lead.name} - {lead.phone} - {lead.product_interest}."
    )


_agent = None


def get_agent():
    global _agent
    if _agent is None:
        llm = build_llm()
        _agent = create_agent(llm, tools=[extract_and_save_lead], system_prompt=SYSTEM_PROMPT)
        logger.info("Agent built (model=%s, base_url=%s)", settings.llm_model, settings.llm_base_url)
    return _agent


async def run_agent(messages: list[dict]) -> str:
    if settings.llm_mock:
        return await mock_run_agent(messages)
    agent = get_agent()
    result = await agent.ainvoke({"messages": messages})
    return result["messages"][-1].content
