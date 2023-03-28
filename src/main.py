from fastapi import Depends, FastAPI, HTTPException
from sqlalchemy.orm import Session
from typing import Any

from . import crud, models, schemas
from .database import SessionLocal, engine

models.Base.metadata.create_all(bind=engine)

app = FastAPI(
    title="Pixel Code Platform",
    description="Self-hostable coding courses/problems platform",
    openapi_tags=[
        {
            "name": "users",
            "description": "User creation, viewing and editing",
        },
    ],
)


def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()


def openapi_http_exception(
    status_code_and_details: list[tuple[int, str]]
) -> dict[int | str, dict[str, Any]]:
    return {
        status_code: {
            "description": detail,
            "content": {"application/json": {"example": {"detail": detail}}},
        }
        for (status_code, detail) in status_code_and_details
    }


@app.post(
    "/users",
    response_model=schemas.UserRead,
    tags=["users"],
    responses=openapi_http_exception([(400, "User Already Exists")]),
)
def create_user(user: schemas.UserCreate, db: Session = Depends(get_db)):
    db_user = crud.user.get_one_by_name(db=db, name=user.name)
    if db_user:
        raise HTTPException(status_code=400, detail="User Already Exists")

    return crud.user.create_one(db=db, user=user)


@app.get("/users", response_model=list[schemas.UserRead], tags=["users"])
def get_users(skip: int = 0, limit: int = 100, db: Session = Depends(get_db)):
    return crud.user.get_multiple(db=db, skip=skip, limit=limit)


@app.get(
    "/users/{user_name}",
    response_model=schemas.UserRead,
    tags=["users"],
    responses=openapi_http_exception([(404, "User Not Found")]),
)
def get_user_by_name(user_name: str, db: Session = Depends(get_db)):
    db_user = crud.user.get_one_by_name(db=db, name=user_name)
    if db_user is None:
        raise HTTPException(status_code=404, detail="User Not Found")

    return db_user
