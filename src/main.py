from fastapi import Depends, FastAPI, HTTPException
from sqlalchemy.orm import Session

from . import crud, models, schemas
from .database import SessionLocal, engine

models.Base.metadata.create_all(bind=engine)

app = FastAPI(
    title="Pixel Code Platform",
    description="Self-hostable coding courses/problems platform",
    openapi_tags=[
        {
            "name": "users",
            "description": "Operations with users",
        },
    ],
)


def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()


@app.post("/users", response_model=schemas.UserRead, tags=["users"])
def create_user(user: schemas.UserCreate, db: Session = Depends(get_db)):
    db_user = crud.user.get_one_by_name(db=db, name=user.name)
    if db_user:
        raise HTTPException(status_code=400, detail="User already exists")

    return crud.user.create_one(db=db, user=user)


@app.get("/users", response_model=list[schemas.UserRead], tags=["users"])
def get_users(skip: int = 0, limit: int = 100, db: Session = Depends(get_db)):
    return crud.user.get_multiple(db=db, skip=skip, limit=limit)


@app.get("/users/{user_id}", response_model=schemas.UserRead, tags=["users"])
def get_user(user_id: int, db: Session = Depends(get_db)):
    db_user = crud.user.get_one(db=db, user_id=user_id)
    if db_user is None:
        raise HTTPException(status_code=404, detail="User not found")

    return db_user
