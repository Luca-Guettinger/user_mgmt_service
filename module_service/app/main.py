import logging

from fastapi import FastAPI, HTTPException, Request
from fastapi.responses import JSONResponse
from prometheus_fastapi_instrumentator import Instrumentator

from app.api import module_router, user_module_router
from app.config import get_settings

settings = get_settings()
logging.basicConfig(
    level=settings.log_level, format="%(asctime)s %(levelname)s %(name)s %(message)s"
)

app = FastAPI(
    title="Module Service",
    description="Manages modules.",
    version=settings.app_version,
)
app.include_router(module_router)
app.include_router(user_module_router)

# Serves the Prometheus text format on /metrics, which the ServiceMonitor in
# the ops repo scrapes: http_requests_total and http_request_duration_seconds.
Instrumentator().instrument(app).expose(app, endpoint="/metrics", include_in_schema=False)


@app.get("/health", include_in_schema=False)
def health() -> dict[str, str]:
    """Liveness and readiness for Kubernetes.

    Deliberately does not touch MySQL: a lost database has to surface as 5xx
    and an alert, not as pods going NotReady while they are merely waiting.
    """
    return {"status": "ok"}


@app.exception_handler(HTTPException)
async def http_exception_handler(_: Request, exc: HTTPException) -> JSONResponse:
    detail = exc.detail
    if not isinstance(detail, dict) or "code" not in detail:
        detail = {"code": "HTTP_ERROR", "message": str(detail)}
    return JSONResponse(status_code=exc.status_code, content=detail, headers=exc.headers)