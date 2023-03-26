from sqlalchemy.orm import Session

from .. import models, schemas


def get_one(db: Session, user_id: int) -> models.User:
    return db.query(models.User).filter(models.User.id == user_id).first()


def get_one_by_name(db: Session, name: str) -> models.User:
    return db.query(models.User).filter(models.User.name == name).first()


def get_multiple(db: Session, skip: int = 0, limit: int = 100) -> list[models.User]:
    return db.query(models.User).offset(skip).limit(limit).all()


def create_one(db: Session, user: schemas.UserCreate) -> models.User:
    db_user = models.User(name=user.name, password=user.password)
    db.add(db_user)
    db.commit()
    db.refresh(db_user)

    return db_user
