from collections.abc import Generator
from typing import Any

from sqlalchemy.orm import Session

from .database import SessionLocal


def get_db() -> Generator[Session, None, None]:
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()


def openapi_http_exception(
    status_code_and_details: list[tuple[int, str]]
) -> dict[int | str, dict[str, Any]]:
    return {
        status_code: {
            "description": detail,
            "content": {"application/json": {"example": {"detail": detail}}},
        }
        for (status_code, detail) in status_code_and_details
    }
