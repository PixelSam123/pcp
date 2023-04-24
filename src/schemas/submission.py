from pydantic import BaseModel

from . import user


class Base(BaseModel):
    code: str


class Create(Base):
    user_id: int
    challenge_id: int


class Read(Base):
    id: int
    challenge_id: int
    user: user.ReadBrief

    class Config:
        orm_mode = True
