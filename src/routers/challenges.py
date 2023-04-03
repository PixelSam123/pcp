from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session

from .. import crud, models, schemas
from ..dependencies import get_db, openapi_http_exception

router = APIRouter(prefix="/challenges", tags=["challenges"])


@router.post(
    "/",
    response_model=schemas.challenge.ReadBrief,
    responses=openapi_http_exception(
        [(400, "Challenge Already Exists or User Doesn't Exist")]
    ),
)
def create_challenge(
    challenge: schemas.challenge.Create, db: Session = Depends(get_db)
) -> models.Challenge:
    db_challenge = crud.challenge.get_one_by_name(db=db, name=challenge.name)
    if db_challenge:
        raise HTTPException(status_code=400, detail="Challenge Already Exists")

    db_user = crud.user.get_one(db=db, user_id=challenge.user_id)
    if not db_user:
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
