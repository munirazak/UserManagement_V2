package com.kopibru.usermanagement.config;

import com.kopibru.usermanagement.model.Borrower;
import com.kopibru.usermanagement.model.BorrowerCredentials;
import com.kopibru.usermanagement.model.BorrowerRole;
import com.kopibru.usermanagement.repository.BorrowerCredentialsRepository;
import com.kopibru.usermanagement.repository.BorrowerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates the single SUPER_ADMIN account once, on first startup when none exists.
 * Values come from {@link SuperAdminBootstrapProperties} (defaults or Secret Manager / env).
 * Subsequent starts are no-ops — password/name changes in secrets do not rewrite an existing admin.
 */
@Component
public class SuperAdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SuperAdminBootstrap.class);

    private final SuperAdminBootstrapProperties properties;
    private final BorrowerRepository borrowerRepository;
    private final BorrowerCredentialsRepository borrowerCredentialsRepository;
    private final PasswordEncoder passwordEncoder;

    public SuperAdminBootstrap(
            SuperAdminBootstrapProperties properties,
            BorrowerRepository borrowerRepository,
            BorrowerCredentialsRepository borrowerCredentialsRepository,
            PasswordEncoder passwordEncoder) {
        this.properties = properties;
        this.borrowerRepository = borrowerRepository;
        this.borrowerCredentialsRepository = borrowerCredentialsRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!properties.isEnabled()) {
            log.info("SUPER_ADMIN bootstrap disabled");
            return;
        }

        if (borrowerRepository.existsByRole(BorrowerRole.SUPER_ADMIN)) {
            log.info("SUPER_ADMIN already present — skipping one-time bootstrap");
            return;
        }

        String libraryId = properties.getLibraryId();
        String username = properties.getUsername();

        if (borrowerRepository.existsByLibraryId(libraryId)) {
            log.error(
                    "Cannot bootstrap SUPER_ADMIN: libraryId '{}' already exists without SUPER_ADMIN role",
                    libraryId);
            return;
        }
        if (borrowerCredentialsRepository.existsByUsername(username)) {
            log.error(
                    "Cannot bootstrap SUPER_ADMIN: username '{}' already exists without SUPER_ADMIN role",
                    username);
            return;
        }

        Borrower admin = new Borrower(
                libraryId,
                properties.getName(),
                properties.getEmail(),
                BorrowerRole.SUPER_ADMIN);
        Borrower saved = borrowerRepository.save(admin);

        BorrowerCredentials credentials = new BorrowerCredentials(
                saved, username, passwordEncoder.encode(properties.getPassword()));
        borrowerCredentialsRepository.save(credentials);

        log.info(
                "One-time SUPER_ADMIN bootstrap completed libraryId={} username={}",
                libraryId, username);
    }
}
