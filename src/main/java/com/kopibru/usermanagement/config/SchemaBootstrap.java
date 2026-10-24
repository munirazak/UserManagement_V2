package com.kopibru.usermanagement.config;

import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Ensures the database from {@code spring.datasource.url} exists (local/Docker TCP only),
 * then runs table DDL from {@code schema.sql}.
 * <p>
 * Cloud Run → Cloud SQL uses the Java Connector ({@code cloudSqlInstance}). In that case
 * {@code CREATE DATABASE} is skipped — create the database in Cloud SQL Console first.
 */
public class SchemaBootstrap implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    private static final Pattern SAFE_DB_NAME = Pattern.compile("[a-zA-Z_][a-zA-Z0-9_]*");

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        Environment env = event.getEnvironment();
        if (!env.getProperty("app.schema-bootstrap.enabled", Boolean.class, true)) {
            return;
        }

        String jdbcUrl = env.getRequiredProperty("spring.datasource.url");
        String username = env.getRequiredProperty("spring.datasource.username");
        String password = env.getProperty("spring.datasource.password", "");

        validateCloudRunDatasource(env, jdbcUrl);

        String databaseName = extractDatabaseName(jdbcUrl);
        if (!SAFE_DB_NAME.matcher(databaseName).matches()) {
            throw new IllegalStateException("Unsafe database name in datasource URL: " + databaseName);
        }

        boolean cloudSql = isCloudSqlConnectorUrl(jdbcUrl);

        try {
            if (!cloudSql) {
                String adminUrl = buildAdminUrl(jdbcUrl);
                ensureDatabaseExists(adminUrl, username, password, databaseName);
            }
            executeSchemaScript(jdbcUrl, username, password);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to initialize database schema", ex);
        }
    }

    /**
     * Fail fast on Cloud Run when prod still points at localhost (missing CLOUD_SQL_INSTANCE).
     */
    private void validateCloudRunDatasource(Environment env, String jdbcUrl) {
        boolean prod = false;
        for (String profile : env.getActiveProfiles()) {
            if ("prod".equalsIgnoreCase(profile)) {
                prod = true;
                break;
            }
        }
        if (!prod) {
            return;
        }

        String lower = jdbcUrl.toLowerCase();
        if (lower.contains("localhost") || lower.contains("127.0.0.1")) {
            throw new IllegalStateException(
                    "Prod datasource points at localhost. On Cloud Run set CLOUD_SQL_INSTANCE="
                            + "project:region:instance (and do not set DB_URL to localhost). "
                            + "Example JDBC default: jdbc:postgresql:///book_catalogue"
                            + "?cloudSqlInstance=PROJECT:REGION:INSTANCE"
                            + "&socketFactory=com.google.cloud.sql.postgres.SocketFactory");
        }

        if (isCloudSqlConnectorUrl(jdbcUrl)) {
            String instance = env.getProperty("CLOUD_SQL_INSTANCE", "");
            if (!StringUtils.hasText(instance) && !jdbcUrl.matches("(?i).*cloudSqlInstance=[^&]+.*")) {
                throw new IllegalStateException(
                        "CLOUD_SQL_INSTANCE is required for Cloud SQL Java Connector "
                                + "(format: project:region:instance).");
            }
            if (lower.contains("cloudsqlinstance=&") || lower.endsWith("cloudsqlinstance=")) {
                throw new IllegalStateException(
                        "CLOUD_SQL_INSTANCE is empty. Set it to your Cloud SQL instance "
                                + "connection name (project:region:instance).");
            }
        }
    }

    private boolean isCloudSqlConnectorUrl(String jdbcUrl) {
        String lower = jdbcUrl.toLowerCase();
        return lower.contains("cloudsqlinstance=")
                || lower.contains("socketfactory=com.google.cloud.sql");
    }

    private String buildAdminUrl(String jdbcUrl) {
        String[] parts = jdbcUrl.split("\\?", 2);
        String base = parts[0];
        String query = parts.length > 1 ? "?" + parts[1] : "";
        int lastSlash = base.lastIndexOf('/');
        return base.substring(0, lastSlash + 1) + "postgres" + query;
    }

    private void ensureDatabaseExists(
            String adminUrl, String username, String password, String databaseName) throws Exception {
        try (Connection conn = DriverManager.getConnection(adminUrl, username, password);
             Statement stmt = conn.createStatement()) {
            if (!databaseExists(stmt, databaseName)) {
                stmt.execute("CREATE DATABASE " + databaseName);
            }
        }
    }

    private void executeSchemaScript(String jdbcUrl, String username, String password) throws Exception {
        List<String> statements = parseStatements(loadSchemaSql());
        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password);
             Statement stmt = conn.createStatement()) {
            for (String sql : statements) {
                stmt.execute(sql);
            }
            migrateCredentialsFkToBorrowerId(conn, stmt);
            migrateEnsureBorrowerRole(conn, stmt);
        }
    }

    /**
     * Existing borrowers tables created before role was added need the column.
     * {@code CREATE TABLE IF NOT EXISTS} does not alter existing tables.
     */
    private void migrateEnsureBorrowerRole(Connection conn, Statement stmt) throws Exception {
        if (!tableExists(conn, "borrowers")) {
            return;
        }
        if (!columnExists(conn, "borrowers", "role")) {
            stmt.execute("""
                    ALTER TABLE borrowers
                    ADD COLUMN role VARCHAR(50) NOT NULL DEFAULT 'BORROWER'
                    """);
        } else {
            stmt.execute("""
                    UPDATE borrowers
                    SET role = 'BORROWER'
                    WHERE role IS NULL OR TRIM(role) = ''
                    """);
            stmt.execute("ALTER TABLE borrowers ALTER COLUMN role SET DEFAULT 'BORROWER'");
            stmt.execute("ALTER TABLE borrowers ALTER COLUMN role SET NOT NULL");
        }
        stmt.execute("""
                ALTER TABLE borrowers
                DROP CONSTRAINT IF EXISTS borrowers_role_check
                """);
        stmt.execute("""
                ALTER TABLE borrowers
                ADD CONSTRAINT borrowers_role_check
                CHECK (role IN ('SUPER_ADMIN', 'ADMIN', 'LIBRARIAN', 'BORROWER'))
                """);
    }

    /**
     * Older schema stored library_id on borrower_credentials (duplicating borrowers.library_id).
     * Migrate to borrower_id → borrowers.id when the legacy column is still present.
     */
    private void migrateCredentialsFkToBorrowerId(Connection conn, Statement stmt) throws Exception {
        if (!tableExists(conn, "borrower_credentials")) {
            return;
        }
        boolean hasLegacyLibraryId = columnExists(conn, "borrower_credentials", "library_id");
        boolean hasBorrowerId = columnExists(conn, "borrower_credentials", "borrower_id");
        if (!hasLegacyLibraryId) {
            return;
        }

        if (!hasBorrowerId) {
            stmt.execute("ALTER TABLE borrower_credentials ADD COLUMN borrower_id BIGINT");
        }

        stmt.execute("""
                UPDATE borrower_credentials bc
                SET borrower_id = b.id
                FROM borrowers b
                WHERE bc.library_id = b.library_id
                  AND (bc.borrower_id IS NULL OR bc.borrower_id <> b.id)
                """);

        dropForeignKeysOnColumn(conn, stmt, "borrower_credentials", "library_id");
        stmt.execute("ALTER TABLE borrower_credentials DROP COLUMN IF EXISTS library_id");

        stmt.execute("ALTER TABLE borrower_credentials ALTER COLUMN borrower_id SET NOT NULL");
        dropForeignKeysOnColumn(conn, stmt, "borrower_credentials", "borrower_id");
        stmt.execute("""
                ALTER TABLE borrower_credentials
                ADD CONSTRAINT borrower_credentials_borrower_id_fkey
                FOREIGN KEY (borrower_id) REFERENCES borrowers (id) ON DELETE CASCADE
                """);
        stmt.execute("""
                ALTER TABLE borrower_credentials
                DROP CONSTRAINT IF EXISTS borrower_credentials_borrower_id_key
                """);
        stmt.execute("""
                ALTER TABLE borrower_credentials
                ADD CONSTRAINT borrower_credentials_borrower_id_key UNIQUE (borrower_id)
                """);
    }

    private boolean tableExists(Connection conn, String table) throws Exception {
        try (ResultSet rs = conn.getMetaData().getTables(null, "public", table, new String[]{"TABLE"})) {
            return rs.next();
        }
    }

    private boolean columnExists(Connection conn, String table, String column) throws Exception {
        try (ResultSet rs = conn.getMetaData().getColumns(null, "public", table, column)) {
            return rs.next();
        }
    }

    private void dropForeignKeysOnColumn(
            Connection conn, Statement stmt, String table, String column) throws Exception {
        try (ResultSet rs = conn.getMetaData().getImportedKeys(null, "public", table)) {
            while (rs.next()) {
                if (column.equalsIgnoreCase(rs.getString("FKCOLUMN_NAME"))) {
                    String fkName = rs.getString("FK_NAME");
                    if (fkName != null && !fkName.isBlank()) {
                        stmt.execute("ALTER TABLE " + table + " DROP CONSTRAINT IF EXISTS " + fkName);
                    }
                }
            }
        }
    }

    private boolean databaseExists(Statement stmt, String databaseName) throws Exception {
        try (ResultSet rs = stmt.executeQuery(
                "SELECT 1 FROM pg_database WHERE datname = '" + databaseName + "'")) {
            return rs.next();
        }
    }

    private String extractDatabaseName(String jdbcUrl) {
        String withoutParams = jdbcUrl.split("\\?", 2)[0];
        int lastSlash = withoutParams.lastIndexOf('/');
        if (lastSlash < 0 || lastSlash == withoutParams.length() - 1) {
            throw new IllegalStateException("Cannot parse database name from JDBC URL: " + jdbcUrl);
        }
        return withoutParams.substring(lastSlash + 1);
    }

    private String loadSchemaSql() throws Exception {
        return StreamUtils.copyToString(
                new ClassPathResource("schema.sql").getInputStream(), StandardCharsets.UTF_8);
    }

    private List<String> parseStatements(String script) {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String line : script.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("--")) {
                continue;
            }
            current.append(trimmed).append(' ');
            if (trimmed.endsWith(";")) {
                String sql = current.toString().trim();
                statements.add(sql.substring(0, sql.length() - 1).trim());
                current.setLength(0);
            }
        }
        return statements;
    }
}
