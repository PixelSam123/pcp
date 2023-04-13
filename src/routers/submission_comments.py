from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session

from .. import crud, models, schemas
from ..dependencies import get_db, openapi_http_exception

router = APIRouter(prefix="/submission_comments", tags=["submission_comments"])


@router.post(
    "/",
    response_model=schemas.submission_comment.Read,
    responses=openapi_http_exception(
        [(400, "Submission Doesn't Exist or User Doesn't Exist")]
    ),
)
def create_submission_comment(
    submission_comment: schemas.submission_comment.Create,
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

    return crud.submission_comment.create_one(
        db=db, submission_comment=submission_comment
    )


@router.get(
    "/{submission_id}",
    response_model=list[schemas.submission_comment.Read],
    responses=openapi_http_exception([(404, "Submission Not Found")]),
)
def get_submission_comments_for_submission_by_id(
    submission_id: int, skip: int = 0, limit: int = 100, db: Session = Depends(get_db)
) -> list[models.SubmissionComment]:
    db_submission = crud.submission.get_one(db=db, submission_id=submission_id)
    if db_submission is None:
        raise HTTPException(status_code=404, detail="Submission Not Found")

    return crud.submission_comment.get_multiple_for_submission(
        db=db, submission_id=submission_id, skip=skip, limit=limit
    )
