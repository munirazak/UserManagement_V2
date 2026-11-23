# UserManagement

Spring Boot service that owns borrowers and authentication (`borrowers`, `borrower_credentials`).

**Maven:** `com.kopibru:UserManagement`  
**Port:** `8082`  
**Database:** `book_catalogue` (shared with BookManagement)  
**JWT:** same `JWT_SECRET` as IndexShelf so tokens issued here can be validated there

IndexShelf can still **register** a borrower by calling this service. Only UserManagement can **list** borrowers.

## Run

### Option A — Local (Maven)

```bash
# Dev profile (default) — requires local PostgreSQL
mvn spring-boot:run

# Prod profile
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

### Option B — Docker Compose (app → Neon)

```bash
docker compose up --build
```

This starts UserManagement on port `8082` against Neon PostgreSQL.
Default profile is **`dev`**. Neon host/password are **not** in properties files — set them via `.env` or Secret Manager.

```bash
cp .env.example .env   # then edit NEON_HOST / DB_PASSWORD
docker compose up --build

# Production Neon branch
SPRING_PROFILES_ACTIVE=prod docker compose up --build
```

On Windows PowerShell:

```powershell
Copy-Item .env.example .env
$env:SPRING_PROFILES_ACTIVE="prod"; docker compose up --build
```

Stop with `docker compose down`.

Base URL: `http://localhost:8082`

### Port and profile (local vs prod)

| Setting | Local default | Production |
|---------|---------------|------------|
| Port | `8082` (`PORT` unset) | Set env `PORT` (Cloud Run sets this, often `8080`) |
| Profile | `dev` | Set env `SPRING_PROFILES_ACTIVE=prod` |
| Database | Neon (via `NEON_HOST`) | Neon (via `NEON_HOST`) |

`application.properties` uses:

```properties
server.port=${PORT:8082}
spring.profiles.active=${SPRING_PROFILES_ACTIVE:dev}
```

Datasource env vars (dev + prod) — never commit real values:

| Env | Purpose | Example |
|-----|---------|---------|
| `NEON_HOST` | Neon pooler hostname only | `ep-....neon.tech` |
| `DB_NAME` | Database name | `book_catalogue` (default) |
| `DB_USERNAME` | DB user | `neondb_owner` (default) |
| `DB_PASSWORD` | DB password | from Secret Manager / `.env` |
| `DB_URL` | Optional full JDBC URL override | if set, replaces the URL built from `NEON_HOST` |

On Cloud Run, configure:

- `SPRING_PROFILES_ACTIVE=prod`
- `NEON_HOST` / `DB_USERNAME` / `DB_PASSWORD` (from Secret Manager)
- `JWT_SECRET` — same value as IndexShelf
- Optional SUPER_ADMIN bootstrap secrets (see below)

Do **not** put Neon host or password in committed properties files.

### One-time SUPER_ADMIN bootstrap

On first startup (dev or prod), if no `SUPER_ADMIN` borrower exists, UserManagement creates one.
Later starts skip creation — changing secrets afterward does **not** update the existing account.

| Setting | Env / Secret Manager | Default |
|---------|----------------------|---------|
| Enabled | `SUPER_ADMIN_BOOTSTRAP_ENABLED` | `true` |
| libraryId | `SUPER_ADMIN_LIBRARY_ID` | `MASTER_ADMIN` |
| name | `SUPER_ADMIN_NAME` | `MASTER_ADMIN` |
| username | `SUPER_ADMIN_USERNAME` | `MASTER_ADMIN` |
| password | `SUPER_ADMIN_PASSWORD` | `superadmin` |
| email | `SUPER_ADMIN_EMAIL` | `superadmin@example.com` |

On Cloud Run, map these from Secret Manager as env vars (or volume mounts that export the same names).
`SUPER_ADMIN` cannot be assigned via `POST /api/borrowers` — registration only allows `ADMIN`, `LIBRARIAN`, or `BORROWER`.

### Neon PostgreSQL

JDBC URL is built from env (when `DB_URL` is unset):

```text
jdbc:postgresql://${NEON_HOST}/${DB_NAME}?sslmode=require&channelBinding=require
```

Create the `book_catalogue` database in the Neon console before the first deploy.
`SchemaBootstrap` skips `CREATE DATABASE` for Neon hosts and only applies `schema.sql` + migrations.

Docker Compose runs the app only (no local Postgres container). Copy `.env.example` → `.env` and set `NEON_HOST` / `DB_PASSWORD`.

### CI/CD (GitHub Actions)

Declarative pipeline in `.github/workflows/ci.yml`:

1. On push/PR to `main`: run `mvn verify` (tests + coverage gate)
2. On push to `main`: build and publish the Docker image to GitHub Container Registry (`ghcr.io`)

Pull the published image (after a successful push to `main`):

```bash
docker pull ghcr.io/<your-github-username>/usermanagement:latest
```

## APIs

| Method | Path | Auth | Purpose |
|--------|------|------|---------|
| `POST` | `/api/borrowers` | public | Register borrower |
| `GET` | `/api/borrowers` | JWT | List all borrowers |
| `GET` | `/api/borrowers/{libraryId}` | public | Lookup by libraryId (used by IndexShelf) |
| `POST` | `/api/auth/login` | public | Issue JWT |

### Register borrower

`role` is optional. Allowed via registration: `ADMIN`, `LIBRARIAN`, `BORROWER` (default `BORROWER`).
`SUPER_ADMIN` is created only by one-time bootstrap (not this API).

```bash
curl -X POST http://localhost:8082/api/borrowers \
  -H "Content-Type: application/json" \
  -d '{
    "libraryId": "LIB-001",
    "name": "Alice Tan",
    "email": "alice@example.com",
    "username": "alice",
    "password": "password123",
    "role": "BORROWER"
  }'
```

### Login

```bash
curl -X POST http://localhost:8082/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","password":"password123"}'
```

### List borrowers (JWT required)

```bash
curl http://localhost:8082/api/borrowers \
  -H "Authorization: Bearer <token>"
```

## Tests

```bash
mvn test
mvn verify
```
