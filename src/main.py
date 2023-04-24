from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from . import models
from .database import engine
from .routers import (
    challenge_comments,
    challenge_votes,
    challenges,
    submission_comments,
    submission_votes,
    submissions,
    token,
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
            "name": "challenge_votes",
            "description": "Challenge vote creation, viewing and editing",
        },
        {
            "name": "submissions",
            "description": "Submission creation, viewing and editing",
        },
        {
            "name": "submission_comments",
            "description": "Submission comment creation, viewing and editing",
        },
        {
            "name": "submission_votes",
            "description": "Submission vote creation, viewing and editing",
        },
    ],
    servers=[
        {"url": "https://pcp.azurewebsites.net", "description": "Azure deployment"},
    ],
)

origins = [
    "https://pixelsam123.github.io",
]

app.add_middleware(
    CORSMiddleware,
    allow_origins=origins,
    allow_credentials=True,
    allow_methods=["GET", "POST", "DELETE"],
    allow_headers=["Authorization", "Content-Type"],
)

app.include_router(users.router)
app.include_router(challenges.router)
app.include_router(challenge_comments.router)
app.include_router(challenge_votes.router)
app.include_router(submissions.router)
app.include_router(submission_comments.router)
app.include_router(submission_votes.router)
app.include_router(token.router)
