from sqlalchemy.orm import Session

from .. import models, schemas


def get_one(db: Session, submission_vote_id: int) -> models.SubmissionVote | None:
    return (
        db.query(models.SubmissionVote)
        .filter(models.SubmissionVote.id == submission_vote_id)
        .first()
    )


def get_one_for_user_and_submission(
    db: Session, user_id: int, submission_id: int
) -> models.SubmissionVote | None:
    return (
        db.query(models.SubmissionVote)
        .filter(models.SubmissionVote.user_id == user_id)
        .filter(models.SubmissionVote.submission_id == submission_id)
        .first()
    )


def get_multiple_for_submission(
    db: Session, submission_id: int, skip: int = 0, limit: int = 100
) -> list[models.SubmissionVote]:
    return (
        db.query(models.SubmissionVote)
        .filter(models.SubmissionVote.submission_id == submission_id)
        .offset(skip)
        .limit(limit)
        .all()
    )


def create_one(
    db: Session, submission_vote: schemas.submission_vote.Create
) -> models.SubmissionVote:
    db_submission_vote = models.SubmissionVote(
        is_upvote=submission_vote.is_upvote,
        user_id=submission_vote.user_id,
        submission_id=submission_vote.submission_id,
    )
    db.add(db_submission_vote)
    db.commit()
    db.refresh(db_submission_vote)

    return db_submission_vote
