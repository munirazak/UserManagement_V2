# UserManagement

Spring Boot service that owns borrowers and authentication (`borrowers`, `borrower_credentials`).

**Maven:** `com.kopibru:UserManagement`  
**Port:** `8082`  
**Database:** `book_catalogue` (shared with BookManagement)  
**JWT:** same `JWT_SECRET` as IndexShelf so tokens issued here can be validated there

IndexShelf can still **register** a borrower by calling this service. Only UserManagement can **list** borrowers.

## Run

```bash
mvn spring-boot:run
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
