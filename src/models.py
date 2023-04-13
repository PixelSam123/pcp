from sqlalchemy import Boolean, Column, ForeignKey, Integer, String
from sqlalchemy.orm import relationship

from .database import Base


class User(Base):
    __tablename__ = "users"

    id = Column(Integer, primary_key=True, index=True)
    name = Column(String, unique=True, index=True)
    password = Column(String)

    group_id = Column(Integer, ForeignKey("groups.id"))
    group = relationship("Group", back_populates="users", lazy="joined")

    challenges = relationship("Challenge", back_populates="user")
    submissions = relationship("Submission", back_populates="user")
    challenge_comments = relationship("ChallengeComment", back_populates="user")
    submission_comments = relationship("SubmissionComment", back_populates="user")
    challenge_votes = relationship("ChallengeVote", back_populates="user")
    submission_votes = relationship("SubmissionVote", back_populates="user")


class Group(Base):
    __tablename__ = "groups"

    id = Column(Integer, primary_key=True, index=True)
    name = Column(String, unique=True, index=True)

    users = relationship("User", back_populates="group")


class Challenge(Base):
    __tablename__ = "challenges"

    id = Column(Integer, primary_key=True, index=True)
    name = Column(String, unique=True, index=True)
    description = Column(String)
    initial_code = Column(String)
    test_case = Column(String)
    tier = Column(Integer)

    user_id = Column(Integer, ForeignKey("users.id"))
    user = relationship("User", back_populates="challenges", lazy="joined")

    submissions = relationship("Submission", back_populates="challenge")
    comments = relationship("ChallengeComment", back_populates="challenge")
    votes = relationship("ChallengeVote", back_populates="challenge")


class Submission(Base):
    __tablename__ = "submissions"

    id = Column(Integer, primary_key=True, index=True)
    code = Column(String)
    result = Column(String)
    time = Column(Integer)
    memory = Column(Integer)

    user_id = Column(Integer, ForeignKey("users.id"))
    user = relationship("User", back_populates="submissions", lazy="joined")
    challenge_id = Column(Integer, ForeignKey("challenges.id"))
    challenge = relationship("Challenge", back_populates="submissions")

    comments = relationship("SubmissionComment", back_populates="submission")
    votes = relationship("SubmissionVote", back_populates="submission")


class ChallengeComment(Base):
    __tablename__ = "challenge_comments"

    id = Column(Integer, primary_key=True, index=True)
    content = Column(String)

    user_id = Column(Integer, ForeignKey("users.id"))
    user = relationship("User", back_populates="challenge_comments", lazy="joined")
    challenge_id = Column(Integer, ForeignKey("challenges.id"))
    challenge = relationship("Challenge", back_populates="comments")


class SubmissionComment(Base):
    __tablename__ = "submission_comments"

    id = Column(Integer, primary_key=True, index=True)
    content = Column(String)

    user_id = Column(Integer, ForeignKey("users.id"))
    user = relationship("User", back_populates="submission_comments", lazy="joined")
    submission_id = Column(Integer, ForeignKey("submissions.id"))
    submission = relationship("Submission", back_populates="comments")


class ChallengeVote(Base):
    __tablename__ = "challenge_votes"

    id = Column(Integer, primary_key=True, index=True)
    is_upvote = Column(Boolean)

    user_id = Column(Integer, ForeignKey("users.id"))
    user = relationship("User", back_populates="challenge_votes", lazy="joined")
    challenge_id = Column(Integer, ForeignKey("challenges.id"))
    challenge = relationship("Challenge", back_populates="votes")


class SubmissionVote(Base):
    __tablename__ = "submission_votes"

    id = Column(Integer, primary_key=True, index=True)
    is_upvote = Column(Boolean)

    user_id = Column(Integer, ForeignKey("users.id"))
    user = relationship("User", back_populates="submission_votes", lazy="joined")
    submission_id = Column(Integer, ForeignKey("submissions.id"))
    submission = relationship("Submission", back_populates="votes")
