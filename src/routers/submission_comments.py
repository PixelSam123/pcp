from typing import Annotated

from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session

from .. import crud, models, schemas
from ..dependencies import get_current_user, get_db
from ..utils import openapi_http_exception

router = APIRouter(prefix="/submission_comments", tags=["submission_comments"])


@router.post(
    "/",
    response_model=schemas.submission_comment.Read,
    responses=openapi_http_exception(
        [
            (400, "Submission Doesn't Exist or User Doesn't Exist"),
            (
                401,
                "Not authenticated or Not allowed to create on another user's behalf",
            ),
        ]
    ),
)
def create_submission_comment(
    submission_comment: schemas.submission_comment.Create,
    current_user: Annotated[models.User, Depends(get_current_user)],
    db: Session = Depends(get_db),
) -> models.SubmissionComment:
    db_submission = crud.submission.get_one(
        db=db, submission_id=submission_comment.submission_id
    )
    if db_submission is None:
        raise HTTPException(status_code=400, detail="Submission Doesn't Exist")

    db_user = crud.user.get_one(db=db, user_id=submission_comment.user_id)
    if db_user is None:
        raise HTTPException(status_code=400, detail="User Doesn't Exist")

    if current_user.id != db_user.id:
        raise HTTPException(
            status_code=401, detail="Not allowed to create on another user's behalf"
        )

    return crud.submission_comment.create_one(
        db=db, submission_comment=submission_comment
    )


@router.get(
    "/{submission_id}",
    response_model=list[schemas.submission_comment.Read],
    responses=openapi_http_exception(
        [(404, "Submission Not Found"), (401, "Not authenticated")]
    ),
)
def get_submission_comments_for_submission_by_id(
    submission_id: int,
    current_user: Annotated[models.User, Depends(get_current_user)],
    skip: int = 0,
    limit: int = 100,
    db: Session = Depends(get_db),
) -> list[models.SubmissionComment]:
    db_submission = crud.submission.get_one(db=db, submission_id=submission_id)
    if db_submission is None:
        raise HTTPException(status_code=404, detail="Submission Not Found")

    return crud.submission_comment.get_multiple_for_submission(
        db=db, submission_id=submission_id, skip=skip, limit=limit
    )
