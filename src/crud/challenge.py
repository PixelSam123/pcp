from sqlalchemy.orm import Session

from .. import models, schemas


def get_one(db: Session, challenge_id: int) -> models.Challenge:
    return (
        db.query(models.Challenge).filter(models.Challenge.id == challenge_id).first()
    )


def get_one_by_name(db: Session, name: str) -> models.Challenge:
    return db.query(models.Challenge).filter(models.Challenge.name == name).first()


def get_multiple(
    db: Session, skip: int = 0, limit: int = 100
) -> list[models.Challenge]:
    return db.query(models.Challenge).offset(skip).limit(limit).all()


def create_one(db: Session, challenge: schemas.challenge.Create) -> models.Challenge:
    db_challenge = models.Challenge(
        name=challenge.name,
        tier=challenge.tier,
        user_id=challenge.user_id,
        description=challenge.description,
        initial_code=challenge.initial_code,
        test_case=challenge.test_case,
    )
    db.add(db_challenge)
    db.commit()
    db.refresh(db_challenge)

    return db_challenge
