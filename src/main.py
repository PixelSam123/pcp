from fastapi import FastAPI

from . import models
from .database import engine
from .routers import challenges, users

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
    ],
)

app.include_router(users.router)
app.include_router(challenges.router)
