from pydantic import BaseModel

from . import user


class Base(BaseModel):
    content: str


class Create(Base):
    user_id: int
    submission_id: int


class Read(Base):
    id: int
    submission_id: int
    user: user.ReadBrief

    class Config:
        orm_mode = True
