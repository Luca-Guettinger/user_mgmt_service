# Build & Run Guide

## Prerequisites

- Docker & Docker Compose
- A `.env` file in the project root (see below)

## Environment Variables

Create a `.env` file:

```env
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/userdb
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres
SPRING_JPA_HIBERNATE_DDL_AUTO=update
JWT_ISSUER=user-mgmt-service
JWT_SECRET=change-me-to-a-secure-secret
JWT_EXPIRATION_MILLIS=3600000

POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres
POSTGRES_DB=userdb

ACME_EMAIL=admin@example.com

HTTP_PORT=8880
HTTPS_PORT=8443
DASHBOARD_PORT=8889
```

All ports have defaults (`8880`, `8443`, `8889`) and can be omitted from `.env` if the defaults are fine. For deployment, change them to e.g. `80`, `443`, `8080`.

## Build & Start

```bash
docker compose up --build
```

This starts 3 services:
- **traefik** — Reverse proxy with TLS termination
- **postgres** — PostgreSQL 16 database
- **backend** — Spring Boot app (only reachable via Traefik)

To stop:

```bash
docker compose down
```

To stop and delete the database volume:

```bash
docker compose down -v
```

## Accessing the Application

The URLs below use the default ports. Replace with your `.env` values if changed.

### Traefik Dashboard

```
http://localhost:8889/dashboard/
```

Shows all registered routes, services, and middlewares.

### Health Endpoint (HTTPS)

```bash
curl -k https://localhost:8443/api/actuator/health
```

The `-k` flag is required because Let's Encrypt cannot issue certificates for localhost, so Traefik uses a self-signed certificate. In Postman, disable SSL verification under Settings.

### HTTP to HTTPS Redirect

```bash
curl -v http://localhost:8880/api/actuator/health
```

Returns a `301 Moved Permanently` redirect to `https://localhost:8443/api/actuator/health`.

### Backend is NOT Directly Accessible

The backend has no exposed ports. Direct access does not work:

```bash
# These will NOT work - backend is only reachable through Traefik
curl http://localhost:8080/actuator/health    # Connection refused
curl http://localhost:8080/users/register     # Connection refused
```

The only way to reach the backend is through Traefik via `/api`:

```bash
# This works
curl -k https://localhost:8443/api/actuator/health

# This also works (register endpoint)
curl -k -X POST https://localhost:8443/api/users/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"secret"}'
```

### Database is NOT Exposed

PostgreSQL is only reachable by the backend via the internal Docker network. There is no port mapping — you cannot connect to it from your host machine.
