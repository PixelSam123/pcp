from fastapi import FastAPI

from . import models
from .database import engine
from .routers import users

models.Base.metadata.create_all(bind=engine)

app = FastAPI(
    title="Pixel Code Platform",
    description="Self-hostable coding courses/problems platform",
    openapi_tags=[
        {
            "name": "users",
            "description": "User creation, viewing and editing",
        },
    ],
)

app.include_router(users.router)
