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
