from pydantic import BaseModel

from . import group


class Base(BaseModel):
    name: str


class Create(Base):
    password: str


class ReadBrief(Base):
    id: int
    group: group.ReadBrief | None

    class Config:
        orm_mode = True
