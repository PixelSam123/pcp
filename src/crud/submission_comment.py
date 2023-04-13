from sqlalchemy.orm import Session

from .. import models, schemas


def get_one(db: Session, submission_comment_id: int) -> models.SubmissionComment:
    return (
        db.query(models.SubmissionComment)
        .filter(models.SubmissionComment.id == submission_comment_id)
        .first()
    )


def get_multiple_for_submission(
    db: Session, submission_id: int, skip: int = 0, limit: int = 100
) -> list[models.SubmissionComment]:
    return (
        db.query(models.SubmissionComment)
        .filter(models.SubmissionComment.submission_id == submission_id)
        .offset(skip)
        .limit(limit)
        .all()
    )


def create_one(
    db: Session, submission_comment: schemas.submission_comment.Create
) -> models.SubmissionComment:
    db_submission_comment = models.SubmissionComment(
        content=submission_comment.content,
        user_id=submission_comment.user_id,
        submission_id=submission_comment.submission_id,
    )
    db.add(db_submission_comment)
    db.commit()
    db.refresh(db_submission_comment)

    return db_submission_comment
