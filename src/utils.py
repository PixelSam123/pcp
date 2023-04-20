from typing import Any


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
