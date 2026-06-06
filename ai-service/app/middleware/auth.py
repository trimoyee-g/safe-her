from typing import Optional
from uuid import UUID

from fastapi import Header, HTTPException


def require_authenticated(x_auth_user_id: Optional[str] = Header(None)) -> UUID:
    """FastAPI dependency — extracts caller identity from gateway-forwarded header."""
    if not x_auth_user_id:
        raise HTTPException(status_code=401, detail="Authentication required")
    try:
        return UUID(x_auth_user_id)
    except ValueError:
        raise HTTPException(status_code=401, detail="Invalid user ID in token")


def get_caller_user_id(x_auth_user_id: Optional[str] = Header(None)) -> Optional[UUID]:
    if not x_auth_user_id:
        return None
    try:
        return UUID(x_auth_user_id)
    except ValueError:
        return None


def require_role(*roles: str):
    """FastAPI dependency factory — checks X-Auth-Role against allowed roles.

    Handles both 'ADMIN' and 'ROLE_ADMIN' formats from the JWT claim.
    """
    normalized = {r.removeprefix("ROLE_").upper() for r in roles}

    def _dependency(
        x_auth_user_id: Optional[str] = Header(None),
        x_auth_role: Optional[str] = Header(None),
    ) -> str:
        if not x_auth_user_id:
            raise HTTPException(status_code=401, detail="Authentication required")
        role = (x_auth_role or "").removeprefix("ROLE_").upper()
        if role not in normalized:
            raise HTTPException(status_code=403, detail="Insufficient permissions")
        return role

    return _dependency
