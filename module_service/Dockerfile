FROM python:3.12-slim AS build

COPY --from=ghcr.io/astral-sh/uv:0.12.17 /uv /bin/uv

ENV UV_COMPILE_BYTECODE=1 \
    UV_LINK_MODE=copy \
    UV_PYTHON_DOWNLOADS=never

WORKDIR /app

# Lockfile only, so this layer survives every edit under app/.
COPY pyproject.toml uv.lock ./
RUN uv sync --frozen --no-dev --no-install-project

FROM python:3.12-slim

RUN useradd --system --create-home appuser

WORKDIR /app

COPY --from=build /app/.venv .venv/
COPY app/ app/

# The project itself is never installed as a package; app.main is imported
# from the working directory, the venv only carries the dependencies.
ENV PATH="/app/.venv/bin:$PATH"

USER appuser

EXPOSE 8080

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8080"]
