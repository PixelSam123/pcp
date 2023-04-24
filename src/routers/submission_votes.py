from typing import Annotated

from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session

from .. import crud, models, schemas
from ..dependencies import get_current_user, get_db
from ..utils import openapi_http_exception

router = APIRouter(prefix="/submission_votes", tags=["submission_votes"])


@router.post(
    "",
    response_model=schemas.submission_vote.Read,
    responses=openapi_http_exception(
        [
            (
                400,
                "Submission Doesn't Exist or User Doesn't Exist or User Already Voted",
            ),
            (
                401,
                "Not authenticated or Not allowed to create on another user's behalf",
            ),
        ]
    ),
)
def create_submission_vote(
    submission_vote: schemas.submission_vote.Create,
    current_user: Annotated[models.User, Depends(get_current_user)],
    db: Session = Depends(get_db),
) -> models.SubmissionVote:
    db_submission = crud.submission.get_one(
        db=db, submission_id=submission_vote.submission_id
    )
    if db_submission is None:
        raise HTTPException(status_code=400, detail="Submission Doesn't Exist")

    db_user = crud.user.get_one(db=db, user_id=submission_vote.user_id)
    if db_user is None:
        raise HTTPException(status_code=400, detail="User Doesn't Exist")

    if current_user.id != db_user.id:
        raise HTTPException(
            status_code=401, detail="Not allowed to create on another user's behalf"
        )

    db_submission_vote = crud.submission_vote.get_one_for_user_and_submission(
        db=db,
        user_id=submission_vote.user_id,
        submission_id=submission_vote.submission_id,
    )
    if db_submission_vote:
        raise HTTPException(
            status_code=400, detail="User Already Voted on this Submission"
        )

    return crud.submission_vote.create_one(db=db, submission_vote=submission_vote)


@router.get(
    "",
    response_model=list[schemas.submission_vote.Read],
    responses=openapi_http_exception(
        [(404, "Submission Not Found"), (401, "Not authenticated")]
    ),
)
def get_submission_votes_for_submission_by_id(
    submission_id: int,
    current_user: Annotated[models.User, Depends(get_current_user)],
    skip: int = 0,
    limit: int = 100,
    db: Session = Depends(get_db),
) -> list[models.SubmissionVote]:
    db_submission = crud.submission.get_one(db=db, submission_id=submission_id)
    if db_submission is None:
        raise HTTPException(status_code=404, detail="Submission Not Found")

    return crud.submission_vote.get_multiple_for_submission(
        db=db, submission_id=submission_id, skip=skip, limit=limit
    )


@router.delete(
    "/{submission_vote_id}",
    responses=openapi_http_exception(
        [
            (404, "Submission Vote Not Found"),
            (
                401,
                "Not authenticated or Not allowed to delete on another user's behalf",
            ),
        ]
    ),
    status_code=204,
)
def delete_submission_vote(
    submission_vote_id: int,
    current_user: Annotated[models.User, Depends(get_current_user)],
    db: Session = Depends(get_db),
) -> None:
    db_submission_vote = crud.submission_vote.get_one(
        db=db, submission_vote_id=submission_vote_id
    )
    if db_submission_vote is None:
        raise HTTPException(status_code=404, detail="Submission Vote Not Found")

    if current_user.id != db_submission_vote.user_id:
        raise HTTPException(
            status_code=401, detail="Not allowed to delete on another user's behalf"
        )

    crud.submission_vote.delete_one(db=db, submission_vote_id=submission_vote_id)
