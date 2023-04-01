from sqlalchemy.orm import Session

from .. import models, schemas


def get_one(db: Session, challenge_comment_id: int) -> models.ChallengeComment:
    return (
        db.query(models.ChallengeComment)
        .filter(models.ChallengeComment.id == challenge_comment_id)
        .first()
    )


def get_multiple(
    db: Session, skip: int = 0, limit: int = 100
) -> list[models.ChallengeComment]:
    return db.query(models.ChallengeComment).offset(skip).limit(limit).all()


def create_one(
    db: Session, challenge_comment: schemas.challenge_comment.Create
) -> models.ChallengeComment:
    db_challenge_comment = models.ChallengeComment(
        content=challenge_comment.content,
        user_id=challenge_comment.user_id,
        challenge_id=challenge_comment.challenge_id,
    )
    db.add(db_challenge_comment)
    db.commit()
    db.refresh(db_challenge_comment)

    return db_challenge_comment
