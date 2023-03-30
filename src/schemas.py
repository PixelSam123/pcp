from pydantic import BaseModel


class GroupBase(BaseModel):
    name: str


class UserBase(BaseModel):
    name: str


class GroupCreate(GroupBase):
    pass


class UserCreate(UserBase):
    password: str


class GroupReadBrief(GroupBase):
    id: int


class UserReadBrief(UserBase):
    id: int
    group: GroupReadBrief

    class Config:
        orm_mode = True
