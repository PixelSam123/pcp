from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session

from .. import crud, models, schemas
from ..dependencies import get_db, openapi_http_exception

router = APIRouter(prefix="/challenges", tags=["challenges"])


@router.post(
    "/",
    response_model=schemas.challenge.ReadBrief,
    responses=openapi_http_exception([(400, "Challenge Already Exists")]),
)
def create_challenge(
    challenge: schemas.challenge.Create, db: Session = Depends(get_db)
) -> models.Challenge:
    db_challenge = crud.challenge.get_one_by_name(db=db, name=challenge.name)
    if db_challenge:
        raise HTTPException(status_code=400, detail="Challenge Already Exists")

    return crud.challenge.create_one(db=db, challenge=challenge)
