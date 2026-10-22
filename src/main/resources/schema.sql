-- Borrower tables owned by UserManagement.
-- library_id is the business key on borrowers only.
-- borrower_credentials references borrowers.id (no duplicated library_id column).

CREATE TABLE IF NOT EXISTS borrowers (
    id         BIGSERIAL PRIMARY KEY,
    library_id VARCHAR(100) NOT NULL UNIQUE,
    name       VARCHAR(255) NOT NULL,
    email      VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS borrower_credentials (
    id          BIGSERIAL PRIMARY KEY,
    borrower_id BIGINT NOT NULL UNIQUE REFERENCES borrowers (id) ON DELETE CASCADE,
    username    VARCHAR(100) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL
);
