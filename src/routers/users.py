from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session

from .. import crud, models, schemas
from ..dependencies import get_db, openapi_http_exception

router = APIRouter(prefix="/users", tags=["users"])


@router.post(
    "/",
    response_model=schemas.user.ReadBrief,
    responses=openapi_http_exception([(400, "User Already Exists")]),
)
def create_user(
    user: schemas.user.Create, db: Session = Depends(get_db)
) -> models.User:
    db_user = crud.user.get_one_by_name(db=db, name=user.name)
    if db_user:
        raise HTTPException(status_code=400, detail="User Already Exists")

    return crud.user.create_one(db=db, user=user)


@router.get("/", response_model=list[schemas.user.ReadBrief])
def get_users(
    skip: int = 0, limit: int = 100, db: Session = Depends(get_db)
) -> list[models.User]:
    return crud.user.get_multiple(db=db, skip=skip, limit=limit)


@router.get(
    "/{user_name}",
    response_model=schemas.user.ReadBrief,
    responses=openapi_http_exception([(404, "User Not Found")]),
)
def get_user_by_name(user_name: str, db: Session = Depends(get_db)) -> models.User:
    db_user = crud.user.get_one_by_name(db=db, name=user_name)
    if db_user is None:
        raise HTTPException(status_code=404, detail="User Not Found")

    return db_user
