"""List available model ids from GreenNode MaaS (GET /v1/models).

Usage (inside backend container):
    docker compose exec backend python /scripts/list_models.py
Or locally with backend deps installed:
    python scripts/list_models.py

Then copy the desired model id into .env as LLM_MODEL.
"""
import asyncio
import os
import sys

from dotenv import load_dotenv

load_dotenv()

import httpx

API_KEY = os.environ.get("GREENNODE_API_KEY", "")
BASE_URL = os.environ.get("LLM_BASE_URL", "https://maas-llm-aiplatform-hcm.api.vngcloud.vn/v1")


async def main() -> int:
    if not API_KEY:
        print("GREENNODE_API_KEY is empty. Set it in .env first.", file=sys.stderr)
        return 1
    url = f"{BASE_URL.rstrip('/')}/models"
    print(f"GET {url}")
    async with httpx.AsyncClient(timeout=30.0) as client:
        try:
            resp = await client.get(url, headers={"Authorization": f"Bearer {API_KEY}"})
        except Exception as exc:
            print(f"Request failed: {exc}", file=sys.stderr)
            print("If you see getaddrinfo ENOTFOUND, this is a DNS/network issue in your "
                  "environment (e.g. need VPN/corporate DNS). Fix connectivity and retry.",
                  file=sys.stderr)
            return 2
    print(f"HTTP {resp.status_code}")
    if resp.status_code != 200:
        print(resp.text, file=sys.stderr)
        return 3
    data = resp.json()
    models = data.get("data") or data.get("models") or []
    if not models:
        print("No models returned. Raw response:")
        print(resp.text)
        return 0
    print(f"Found {len(models)} model(s):\n")
    for m in models:
        mid = m.get("id") or m.get("path") or m.get("code")
        owner = m.get("owned_by", "")
        print(f"  - {mid}" + (f"   ({owner})" if owner else ""))
    print("\n=> Chọn 1 id và điền vào .env:  LLM_MODEL=<id>")
    return 0


if __name__ == "__main__":
    sys.exit(asyncio.run(main()))
