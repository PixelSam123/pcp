from typing import Annotated

from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session

from .. import crud, models, schemas
from ..dependencies import get_current_user, get_db
from ..utils import openapi_http_exception

router = APIRouter(prefix="/challenges", tags=["challenges"])


@router.post(
    "/",
    response_model=schemas.challenge.ReadBrief,
    responses=openapi_http_exception(
        [
            (400, "Challenge Already Exists or User Doesn't Exist"),
            (401, "Not authenticated"),
        ]
    ),
)
def create_challenge(
    challenge: schemas.challenge.Create,
    current_user: Annotated[models.User, Depends(get_current_user)],
    db: Session = Depends(get_db),
) -> models.Challenge:
    db_challenge = crud.challenge.get_one_by_name(db=db, name=challenge.name)
    if db_challenge:
        raise HTTPException(status_code=400, detail="Challenge Already Exists")

    db_user = crud.user.get_one(db=db, user_id=challenge.user_id)
    if db_user is None:
        raise HTTPException(status_code=400, detail="User Doesn't Exist")

    return crud.challenge.create_one(db=db, challenge=challenge)


@router.get("/", response_model=list[schemas.challenge.ReadBrief])
def get_challenges(
    skip: int = 0, limit: int = 100, db: Session = Depends(get_db)
) -> list[models.Challenge]:
    return crud.challenge.get_multiple(db=db, skip=skip, limit=limit)


@router.get(
    "/{challenge_name}",
    response_model=schemas.challenge.Read,
    responses=openapi_http_exception([(404, "Challenge Not Found")]),
)
def get_challenge_by_name(
    challenge_name: str, db: Session = Depends(get_db)
) -> models.Challenge:
    db_challenge = crud.challenge.get_one_by_name(db=db, name=challenge_name)
    if db_challenge is None:
        raise HTTPException(status_code=404, detail="Challenge Not Found")

    return db_challenge
