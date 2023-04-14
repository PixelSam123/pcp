from sqlalchemy.orm import Session

from .. import models, schemas


def get_one(db: Session, challenge_vote_id: int) -> models.ChallengeVote:
    return (
        db.query(models.ChallengeVote)
        .filter(models.ChallengeVote.id == challenge_vote_id)
        .first()
    )


def get_multiple_for_challenge(
    db: Session, challenge_id: int, skip: int = 0, limit: int = 100
) -> list[models.ChallengeVote]:
    return (
        db.query(models.ChallengeVote)
        .filter(models.ChallengeVote.challenge_id == challenge_id)
        .offset(skip)
        .limit(limit)
        .all()
    )


def create_one(
    db: Session, challenge_vote: schemas.challenge_vote.Create
) -> models.ChallengeVote:
    db_challenge_vote = models.ChallengeVote(
        is_upvote=challenge_vote.is_upvote,
        user_id=challenge_vote.user_id,
        challenge_id=challenge_vote.challenge_id,
    )
    db.add(db_challenge_vote)
    db.commit()
    db.refresh(db_challenge_vote)

    return db_challenge_vote
