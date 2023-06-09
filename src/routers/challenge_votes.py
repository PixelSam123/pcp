from typing import Annotated

from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session

from .. import crud, models, schemas
from ..dependencies import get_current_user, get_db
from ..utils import openapi_http_exception

router = APIRouter(prefix="/challenge_votes", tags=["challenge_votes"])


@router.post(
    "",
    response_model=schemas.challenge_vote.Read,
    responses=openapi_http_exception(
        [
            (
                400,
                "Challenge Doesn't Exist or User Doesn't Exist or User Already Voted",
            ),
            (
                401,
                "Not authenticated or Not allowed to create on another user's behalf",
            ),
        ]
    ),
)
def create_challenge_vote(
    challenge_vote: schemas.challenge_vote.Create,
    current_user: Annotated[models.User, Depends(get_current_user)],
    db: Session = Depends(get_db),
) -> models.ChallengeVote:
    db_challenge = crud.challenge.get_one(
        db=db, challenge_id=challenge_vote.challenge_id
    )
    if db_challenge is None:
        raise HTTPException(status_code=400, detail="Challenge Doesn't Exist")

    db_user = crud.user.get_one(db=db, user_id=challenge_vote.user_id)
    if db_user is None:
        raise HTTPException(status_code=400, detail="User Doesn't Exist")

    if current_user.id != db_user.id:
        raise HTTPException(
            status_code=401, detail="Not allowed to create on another user's behalf"
        )

    db_challenge_vote = crud.challenge_vote.get_one_for_user_and_challenge(
        db=db,
        user_id=challenge_vote.user_id,
        challenge_id=challenge_vote.challenge_id,
    )
    if db_challenge_vote:
        raise HTTPException(
            status_code=400, detail="User Already Voted on this Challenge"
        )

    return crud.challenge_vote.create_one(db=db, challenge_vote=challenge_vote)


@router.get(
    "",
    response_model=list[schemas.challenge_vote.Read],
    responses=openapi_http_exception([(404, "Challenge Not Found")]),
)
def get_challenge_votes_for_challenge_by_name(
    challenge_name: str, skip: int = 0, limit: int = 100, db: Session = Depends(get_db)
) -> list[models.ChallengeVote]:
    db_challenge = crud.challenge.get_one_by_name(db=db, name=challenge_name)
    if db_challenge is None:
        raise HTTPException(status_code=404, detail="Challenge Not Found")

    return crud.challenge_vote.get_multiple_for_challenge(
        db=db, challenge_id=int(str(db_challenge.id)), skip=skip, limit=limit
    )


@router.delete(
    "/{challenge_vote_id}",
    responses=openapi_http_exception(
        [
            (404, "Challenge Vote Not Found"),
            (
                401,
                "Not authenticated or Not allowed to delete on another user's behalf",
            ),
        ]
    ),
    status_code=204,
)
def delete_challenge_vote(
    challenge_vote_id: int,
    current_user: Annotated[models.User, Depends(get_current_user)],
    db: Session = Depends(get_db),
) -> None:
    db_challenge_vote = crud.challenge_vote.get_one(
        db=db, challenge_vote_id=challenge_vote_id
    )
    if db_challenge_vote is None:
        raise HTTPException(status_code=404, detail="Challenge Vote Not Found")

    if current_user.id != db_challenge_vote.user_id:
        raise HTTPException(
            status_code=401, detail="Not allowed to delete on another user's behalf"
        )

    crud.challenge_vote.delete_one(db=db, challenge_vote_id=challenge_vote_id)
