from typing import Annotated

from fastapi import APIRouter, Depends, HTTPException
from fastapi.security import OAuth2PasswordRequestForm
from sqlalchemy.orm import Session

from .. import crud, schemas
from ..dependencies import get_db, get_settings
from ..settings import Settings
from ..utils import create_access_token, verify_password

router = APIRouter(prefix="/token")


# Note: 401 response not documented yet as the
# convenience function may need a revision to
# support additional properties like "headers"
@router.post("", response_model=schemas.token.Token)
def login_for_access_token(
    form_data: Annotated[OAuth2PasswordRequestForm, Depends()],
    settings: Annotated[Settings, Depends(get_settings)],
    db: Session = Depends(get_db),
) -> dict[str, str]:
    db_user = crud.user.get_one_by_name(db=db, name=form_data.username)
    if db_user is None or not verify_password(
        str(db_user.password), form_data.password
    ):
        raise HTTPException(
            status_code=401,
            detail="Incorrect username or password",
            headers={"WWW-Authenticate": "Bearer"},
        )

    access_token = create_access_token(
        secret_key=settings.secret_key,
        data={"sub": str(db_user.id), "name": db_user.name},
    )

    return {"access_token": access_token, "token_type": "bearer"}
