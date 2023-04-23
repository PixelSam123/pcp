from sqlalchemy.orm import Session

from .. import models, schemas


def get_one(db: Session, submission_id: int) -> models.Submission | None:
    return (
        db.query(models.Submission)
        .filter(models.Submission.id == submission_id)
        .first()
    )


def get_multiple_for_challenge(
    db: Session, challenge_id: int, skip: int = 0, limit: int = 100
) -> list[models.Submission]:
    return (
        db.query(models.Submission)
        .filter(models.Submission.challenge_id == challenge_id)
        .offset(skip)
        .limit(limit)
        .all()
    )


def get_multiple_for_user_and_challenge(
    db: Session, user_id: int, challenge_id: int, skip: int = 0, limit: int = 100
) -> list[models.Submission]:
    return (
        db.query(models.Submission)
        .filter(models.Submission.user_id == user_id)
        .filter(models.Submission.challenge_id == challenge_id)
        .offset(skip)
        .limit(limit)
        .all()
    )


def create_one(db: Session, submission: schemas.submission.Create) -> models.Submission:
    db_submission = models.Submission(
        code=submission.code,
        result=submission.result,
        time=submission.time,
        memory=submission.memory,
        user_id=submission.user_id,
        challenge_id=submission.challenge_id,
    )
    db.add(db_submission)
    db.commit()
    db.refresh(db_submission)

    return db_submission
