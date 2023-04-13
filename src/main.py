from fastapi import FastAPI

from . import models
from .database import engine
from .routers import (
    challenge_comments,
    challenges,
    submission_comments,
    submissions,
    users,
)

models.Base.metadata.create_all(bind=engine)

app = FastAPI(
    title="Pixel Code Platform",
    description="Self-hostable coding courses/problems platform",
    openapi_tags=[
        {
            "name": "users",
            "description": "User creation, viewing and editing",
        },
        {
            "name": "challenges",
            "description": "Challenge creation, viewing and editing",
        },
        {
            "name": "challenge_comments",
            "description": "Challenge comment creation, viewing and editing",
        },
        {
            "name": "submissions",
            "description": "Submission creation, viewing and editing",
        },
        {
            "name": "submission_comments",
            "description": "Submission comment creation, viewing and editing",
        },
    ],
)

app.include_router(users.router)
app.include_router(challenges.router)
app.include_router(challenge_comments.router)
app.include_router(submissions.router)
app.include_router(submission_comments.router)
