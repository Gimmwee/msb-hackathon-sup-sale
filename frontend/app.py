import os
import time

import requests
import streamlit as st
from dotenv import load_dotenv
from streamlit_autorefresh import st_autorefresh

load_dotenv()

API_BASE_URL = os.environ.get("API_BASE_URL", "http://localhost:8000")
API_KEY = os.environ.get("DASHBOARD_API_KEY", "")

st.set_page_config(page_title="sup-sale Dashboard", page_icon="💼", layout="wide")


def fetch_leads():
    try:
        resp = requests.get(
            f"{API_BASE_URL}/api/leads",
            headers={"X-API-Key": API_KEY},
            timeout=10,
        )
        if resp.status_code == 200:
            return resp.json()
        st.toast(f"GET /api/leads -> {resp.status_code}: {resp.text[:200]}")
        return []
    except Exception as exc:
        st.toast(f"Không kết nối được backend: {exc}")
        return []


with st.sidebar:
    st.header("💬 Chat với sup-sale")
    st.caption("Tư vấn viên ngân hàng MSB")

    if "session_id" not in st.session_state:
        st.session_state.session_id = f"web-{int(time.time())}"
    if "chat_history" not in st.session_state:
        st.session_state.chat_history = []

    st.caption(f"Session: `{st.session_state.session_id}`")

    for msg in st.session_state.chat_history:
        with st.chat_message(msg["role"]):
            st.markdown(msg["content"])

    user_input = st.chat_input("Nhập tin nhắn cho sup-sale...")
    if user_input:
        st.session_state.chat_history.append({"role": "user", "content": user_input})
        with st.chat_message("user"):
            st.markdown(user_input)
        try:
            resp = requests.post(
                f"{API_BASE_URL}/api/chat",
                json={
                    "session_id": st.session_state.session_id,
                    "platform": "web",
                    "message": user_input,
                },
                timeout=60,
            )
            reply = resp.json().get("reply", f"(lỗi {resp.status_code})")
        except Exception as exc:
            reply = f"Lỗi kết nối backend: {exc}"
        st.session_state.chat_history.append({"role": "assistant", "content": reply})
        with st.chat_message("assistant"):
            st.markdown(reply)


st_autorefresh(interval=5000, key="leads_refresh")

st.title("sup-sale — Dashboard Leads")
st.caption("Tự làm mới mỗi 5 giây.")

leads = fetch_leads()

col1, col2, col3 = st.columns(3)
col1.metric("Total Leads Captured", len(leads))
new_count = sum(1 for l in leads if l.get("status") == "new")
col2.metric("Leads mới (new)", new_count)
zalo_count = sum(1 for l in leads if str(l.get("session_id", "")).startswith("zalo:"))
col3.metric("Leads từ Zalo", zalo_count)

st.subheader("Danh sách Leads")
if leads:
    table_rows = [
        {
            "Tên": l.get("name"),
            "SĐT": l.get("phone"),
            "Nhu cầu": l.get("product_interest"),
            "Session": l.get("session_id"),
            "Trạng thái": l.get("status"),
            "Trích xuất lúc": l.get("extracted_at"),
        }
        for l in leads
    ]
    st.table(table_rows)
else:
    st.info("Chưa có lead nào. Hãy chat ở sidebar và cung cấp tên + SĐT + nhu cầu, hoặc chạy `scripts/seed_demo_data.py`.")
