package com.kopibru.usermanagement.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * One-time SUPER_ADMIN bootstrap credentials.
 * Override via env / Secret Manager (e.g. Cloud Run secret refs).
 */
@ConfigurationProperties(prefix = "app.super-admin")
public class SuperAdminBootstrapProperties {

    /**
     * When false, skip bootstrap entirely (useful in tests).
     */
    private boolean enabled = true;

    private String libraryId = "MASTER_ADMIN";
    private String name = "MASTER_ADMIN";
    private String username = "MASTER_ADMIN";
    private String password = "superadmin";
    private String email = "superadmin@example.com";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getLibraryId() {
        return libraryId;
    }

    public void setLibraryId(String libraryId) {
        this.libraryId = libraryId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
