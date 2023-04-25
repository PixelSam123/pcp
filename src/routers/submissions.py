from typing import Annotated

import httpx
from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session

from .. import crud, models, schemas
from ..dependencies import get_current_user, get_db, get_settings
from ..settings import Settings
from ..utils import openapi_http_exception

router = APIRouter(prefix="/submissions", tags=["submissions"])


@router.post(
    "",
    response_model=schemas.submission.Read,
    responses=openapi_http_exception(
        [
            (400, "Challenge Doesn't Exist or User Doesn't Exist"),
            (
                401,
                "Not authenticated or Not allowed to create on another user's behalf",
            ),
        ]
    ),
)
def create_submission_for_challenge(
    submission: schemas.submission.Create,
    current_user: Annotated[models.User, Depends(get_current_user)],
    settings: Annotated[Settings, Depends(get_settings)],
    db: Session = Depends(get_db),
) -> models.Submission:
    db_challenge = crud.challenge.get_one(db=db, challenge_id=submission.challenge_id)
    if db_challenge is None:
        raise HTTPException(status_code=400, detail="Challenge Doesn't Exist")

    db_user = crud.user.get_one(db=db, user_id=submission.user_id)
    if db_user is None:
        raise HTTPException(status_code=400, detail="User Doesn't Exist")

    if current_user.id != db_user.id:
        raise HTTPException(
            status_code=401, detail="Not allowed to create on another user's behalf"
        )

    code_to_exec = f"{db_challenge.test_case}\n{submission.code}"
    code_exec_request = httpx.post(
        settings.code_exec_server_url,
        json={"lang": "js", "code": code_to_exec},
    )
    code_exec_request.raise_for_status()

    code_exec_result = code_exec_request.json()
    if code_exec_result["status"] != 0:
        raise HTTPException(
            status_code=400,
            detail=f"Code execution failed:\n{code_exec_result['output']}",
        )

    db_submissions = crud.submission.get_multiple_for_user_and_challenge(
        db=db, user_id=submission.user_id, challenge_id=submission.challenge_id
    )
    if len(db_submissions) < 1:
        crud.user.add_points_to_one(
            db=db,
            user_id=submission.user_id,
            points=db_challenge.tier,  # type: ignore
        )

    return crud.submission.create_one(db=db, submission=submission)


@router.get(
    "",
    response_model=list[schemas.submission.Read],
    responses=openapi_http_exception(
        [(404, "Challenge Not Found"), (401, "Not authenticated")]
    ),
)
def get_submissions_for_challenge_by_name(
    challenge_name: str,
    current_user: Annotated[models.User, Depends(get_current_user)],
    skip: int = 0,
    limit: int = 100,
    db: Session = Depends(get_db),
) -> list[models.Submission]:
    db_challenge = crud.challenge.get_one_by_name(db=db, name=challenge_name)
    if db_challenge is None:
        raise HTTPException(status_code=404, detail="Challenge Not Found")

    return crud.submission.get_multiple_for_challenge(
        db=db, challenge_id=db_challenge.id, skip=skip, limit=limit  # type: ignore
    )
