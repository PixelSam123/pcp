from datetime import UTC, datetime, timedelta
from typing import Any

from argon2 import PasswordHasher
from argon2.exceptions import VerifyMismatchError
from jose import jwt

password_hasher = PasswordHasher()


def verify_password(hashed_password: str, plain_password: str) -> bool:
    try:
        return password_hasher.verify(hashed_password, plain_password)
    except VerifyMismatchError:
        return False


def get_password_hash(password: str) -> str:
    return password_hasher.hash(password)


def create_access_token(
    secret_key: str, data: dict, expires_delta: timedelta | None = None
) -> str:
    to_encode = data.copy()

    if expires_delta:
        expire = datetime.now(tz=UTC) + expires_delta
    else:
        expire = datetime.now(tz=UTC) + timedelta(minutes=30)

    to_encode.update({"exp": expire})

    encoded_jwt = jwt.encode(to_encode, secret_key, algorithm="HS256")
    return encoded_jwt


def openapi_http_exception(
    status_code_and_details: list[tuple[int, str]]
) -> dict[int | str, dict[str, Any]]:
    """Convenience function for defining non-successful OpenAPI responses."""
    return {
        status_code: {
            "description": detail,
            "content": {"application/json": {"example": {"detail": detail}}},
        }
        for (status_code, detail) in status_code_and_details
    }
