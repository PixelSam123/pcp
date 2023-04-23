from sqlalchemy.orm import Session

from .. import models, schemas


def get_one(db: Session, user_id: int) -> models.User | None:
    return db.query(models.User).filter(models.User.id == user_id).first()


def get_one_by_name(db: Session, name: str) -> models.User | None:
    return db.query(models.User).filter(models.User.name == name).first()


def get_multiple(db: Session, skip: int = 0, limit: int = 100) -> list[models.User]:
    return db.query(models.User).offset(skip).limit(limit).all()


def create_one(db: Session, user: schemas.user.Create) -> models.User:
    db_user = models.User(name=user.name, password=user.password)
    db.add(db_user)
    db.commit()
    db.refresh(db_user)

    return db_user


def add_points_to_one(db: Session, user_id: int, points: int) -> None:
    db_user = get_one(db=db, user_id=user_id)
    db_user.points += points  # type: ignore
    db.commit()
    db.expire(db_user)
