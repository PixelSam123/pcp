from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session

from .. import crud, models, schemas
from ..dependencies import get_db, openapi_http_exception

router = APIRouter(prefix="/submissions", tags=["submissions"])


@router.post(
    "/",
    response_model=schemas.submission.Read,
    responses=openapi_http_exception(
        [(400, "Challenge Doesn't Exist or User Doesn't Exist")]
    ),
)
def create_submission_for_challenge(
    submission: schemas.submission.Create,
    db: Session = Depends(get_db),
) -> models.Submission:
    db_challenge = crud.challenge.get_one(db=db, challenge_id=submission.challenge_id)
    if db_challenge is None:
        raise HTTPException(status_code=400, detail="Challenge Doesn't Exist")

    # ...insert checks against code checker here

    return crud.submission.create_one(db=db, submission=submission)


@router.get(
    "/{challenge_name}",
    response_model=list[schemas.submission.Read],
    responses=openapi_http_exception([(404, "Challenge Not Found")]),
)
def get_submissions_for_challenge_by_name(
    challenge_name: str, skip: int = 0, limit: int = 0, db: Session = Depends(get_db)
) -> list[models.Submission]:
    db_challenge = crud.challenge.get_one_by_name(db=db, name=challenge_name)
    if db_challenge is None:
        raise HTTPException(status_code=404, detail="Challenge Not Found")

    return crud.submission.get_multiple_for_challenge(
        db=db, challenge_id=int(str(db_challenge.id)), skip=skip, limit=limit
    )
