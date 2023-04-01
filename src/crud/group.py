from sqlalchemy.orm import Session

from .. import models, schemas


def create_one(db: Session, group: schemas.group.Create) -> models.Group:
    db_group = models.Group(name=group.name)
    db.add(db_group)
    db.commit()
    db.refresh(db_group)

    return db_group
