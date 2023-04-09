from pydantic import BaseModel

from . import user


class Base(BaseModel):
    name: str
    tier: int


class Create(Base):
    user_id: int
    description: str
    initial_code: str
    test_case: str


class ReadBrief(Base):
    id: int
    user: user.ReadBrief

    class Config:
        orm_mode = True


class Read(ReadBrief):
    description: str
    initial_code: str

    class Config:
        orm_mode = True
