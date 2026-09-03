from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", extra="ignore")

    greennode_api_key: str = ""
    llm_base_url: str = "https://maas-llm-aiplatform-hcm.api.vngcloud.vn/v1"
    llm_model: str = ""

    zalo_bot_token: str = ""
    zalo_oa_secret_key: str = ""

    dashboard_api_key: str = ""

    postgres_user: str = "postgres"
    postgres_password: str = "postgres"
    postgres_db: str = "supsale"
    database_url: str = "postgresql+asyncpg://postgres:postgres@db:5432/supsale"

    cors_allowed_origins: str = "http://localhost:8501"
    chat_history_limit: int = 10


settings = Settings()
