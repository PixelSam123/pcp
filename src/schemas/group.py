from pydantic import BaseModel


class Base(BaseModel):
    name: str


class Create(Base):
    pass


class ReadBrief(Base):
    id: int

    class Config:
        orm_mode = True
