package com.kopibru.usermanagement.config;

import com.kopibru.usermanagement.model.Borrower;
import com.kopibru.usermanagement.model.BorrowerCredentials;
import com.kopibru.usermanagement.model.BorrowerRole;
import com.kopibru.usermanagement.repository.BorrowerCredentialsRepository;
import com.kopibru.usermanagement.repository.BorrowerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SuperAdminBootstrapTest {

    @Mock
    private BorrowerRepository borrowerRepository;

    @Mock
    private BorrowerCredentialsRepository borrowerCredentialsRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private SuperAdminBootstrapProperties properties;
    private SuperAdminBootstrap bootstrap;

    @BeforeEach
    void setUp() {
        properties = new SuperAdminBootstrapProperties();
        bootstrap = new SuperAdminBootstrap(
                properties, borrowerRepository, borrowerCredentialsRepository, passwordEncoder);
    }

    @Test
    void run_createsSuperAdminWhenMissing() {
        when(borrowerRepository.existsByRole(BorrowerRole.SUPER_ADMIN)).thenReturn(false);
        when(borrowerRepository.existsByLibraryId("MASTER_ADMIN")).thenReturn(false);
        when(borrowerCredentialsRepository.existsByUsername("MASTER_ADMIN")).thenReturn(false);
        when(passwordEncoder.encode("superadmin")).thenReturn("hashed");
        when(borrowerRepository.save(any(Borrower.class))).thenAnswer(invocation -> {
            Borrower b = invocation.getArgument(0);
            b.setId(1L);
            return b;
        });

        bootstrap.run(new DefaultApplicationArguments());

        ArgumentCaptor<Borrower> borrowerCaptor = ArgumentCaptor.forClass(Borrower.class);
        verify(borrowerRepository).save(borrowerCaptor.capture());
        Borrower saved = borrowerCaptor.getValue();
        assertThat(saved.getLibraryId()).isEqualTo("MASTER_ADMIN");
        assertThat(saved.getName()).isEqualTo("MASTER_ADMIN");
        assertThat(saved.getEmail()).isEqualTo("superadmin@example.com");
        assertThat(saved.getRole()).isEqualTo(BorrowerRole.SUPER_ADMIN);

        ArgumentCaptor<BorrowerCredentials> credentialsCaptor =
                ArgumentCaptor.forClass(BorrowerCredentials.class);
        verify(borrowerCredentialsRepository).save(credentialsCaptor.capture());
        assertThat(credentialsCaptor.getValue().getUsername()).isEqualTo("MASTER_ADMIN");
        assertThat(credentialsCaptor.getValue().getPassword()).isEqualTo("hashed");
    }

    @Test
    void run_skipsWhenSuperAdminAlreadyExists() {
        when(borrowerRepository.existsByRole(BorrowerRole.SUPER_ADMIN)).thenReturn(true);

        bootstrap.run(new DefaultApplicationArguments());

        verify(borrowerRepository, never()).save(any());
        verify(borrowerCredentialsRepository, never()).save(any());
    }

    @Test
    void run_skipsWhenDisabled() {
        properties.setEnabled(false);

        bootstrap.run(new DefaultApplicationArguments());

        verify(borrowerRepository, never()).existsByRole(any());
        verify(borrowerRepository, never()).save(any());
    }

    @Test
    void run_usesSecretManagerOverrides() {
        properties.setLibraryId("SA-LIB");
        properties.setName("Custom Admin");
        properties.setUsername("custom_admin");
        properties.setPassword("from-secret");
        properties.setEmail("admin@corp.example");

        when(borrowerRepository.existsByRole(BorrowerRole.SUPER_ADMIN)).thenReturn(false);
        when(borrowerRepository.existsByLibraryId("SA-LIB")).thenReturn(false);
        when(borrowerCredentialsRepository.existsByUsername("custom_admin")).thenReturn(false);
        when(passwordEncoder.encode("from-secret")).thenReturn("hashed-secret");
        when(borrowerRepository.save(any(Borrower.class))).thenAnswer(invocation -> {
            Borrower b = invocation.getArgument(0);
            b.setId(9L);
            return b;
        });

        bootstrap.run(new DefaultApplicationArguments());

        ArgumentCaptor<Borrower> borrowerCaptor = ArgumentCaptor.forClass(Borrower.class);
        verify(borrowerRepository).save(borrowerCaptor.capture());
        assertThat(borrowerCaptor.getValue().getName()).isEqualTo("Custom Admin");
        assertThat(borrowerCaptor.getValue().getEmail()).isEqualTo("admin@corp.example");
        verify(passwordEncoder).encode("from-secret");
    }
}
