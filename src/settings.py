from pydantic import BaseSettings


class Settings(BaseSettings):
    secret_key: str = ""
    code_exec_server_url: str = ""

    class Config:
        env_file = ".env"
