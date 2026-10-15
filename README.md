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

### Option B — Docker Compose (app + Postgres)

```bash
docker compose up --build
```

This starts:

- PostgreSQL on host port `5434` (container still uses `5432`)
- UserManagement API on port `8082` (`prod` profile)

Stop with `docker compose down` (add `-v` to also remove the DB volume).

Base URL: `http://localhost:8082`

### Port and profile (local vs GCP)

| Setting | Local default | Production / GCP |
|---------|---------------|------------------|
| Port | `8082` (`PORT` unset) | Set env `PORT` (Cloud Run sets this, often `8080`) |
| Profile | `dev` | Set env `SPRING_PROFILES_ACTIVE=prod` |

`application.properties` uses:

```properties
server.port=${PORT:8082}
spring.profiles.active=${SPRING_PROFILES_ACTIVE:dev}
```

On GCP (Cloud Run), configure env vars such as:

- `SPRING_PROFILES_ACTIVE=prod`
- `CLOUD_SQL_INSTANCE=project:region:instance` (**required** — from Cloud SQL instance overview)
- `DB_NAME=book_catalogue` (optional; default)
- `DB_USERNAME` / `DB_PASSWORD`
- `JWT_SECRET` — same value as IndexShelf
- Attach the same Cloud SQL instance under Cloud Run **Cloud SQL connections**
- Grant the Cloud Run service account `roles/cloudsql.client`

Do **not** set `DB_URL` to `jdbc:postgresql://localhost:5432/...` on Cloud Run.
Do **not** bake `prod` into the JAR; override with env vars at deploy time.

### Cloud Run → Cloud SQL (recommended)

UserManagement uses the **Cloud SQL Java Connector** (`postgres-socket-factory`), not a public internet Postgres host.

Prod JDBC default (when `DB_URL` is unset):

```text
jdbc:postgresql:///<DB_NAME>?cloudSqlInstance=<CLOUD_SQL_INSTANCE>
  &socketFactory=com.google.cloud.sql.postgres.SocketFactory
  &cloudSqlRefreshStrategy=lazy
```

Create the database in Cloud SQL Console before the first deploy. Local Docker Compose still sets `DB_URL=jdbc:postgresql://postgres:5432/book_catalogue`.

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

```bash
curl -X POST http://localhost:8082/api/borrowers \
  -H "Content-Type: application/json" \
  -d '{
    "libraryId": "LIB-001",
    "name": "Alice Tan",
    "email": "alice@example.com",
    "username": "alice",
    "password": "password123"
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
