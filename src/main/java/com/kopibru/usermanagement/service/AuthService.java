package com.kopibru.usermanagement.service;

import com.kopibru.usermanagement.dto.LoginRequest;
import com.kopibru.usermanagement.dto.LoginResponse;
import com.kopibru.usermanagement.exception.UnauthorizedException;
import com.kopibru.usermanagement.model.BorrowerCredentials;
import com.kopibru.usermanagement.repository.BorrowerCredentialsRepository;
import com.kopibru.usermanagement.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final BorrowerCredentialsRepository borrowerCredentialsRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            BorrowerCredentialsRepository borrowerCredentialsRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.borrowerCredentialsRepository = borrowerCredentialsRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        log.info("Authenticating username={}", request.getUsername());
        BorrowerCredentials credentials = borrowerCredentialsRepository
                .findByUsernameWithBorrower(request.getUsername())
                .orElseThrow(() -> new UnauthorizedException("Invalid username or password"));

        if (!passwordEncoder.matches(request.getPassword(), credentials.getPassword())) {
            log.info("Authentication failed for username={}", request.getUsername());
            throw new UnauthorizedException("Invalid username or password");
        }

        String libraryId = credentials.getBorrower().getLibraryId();
        String token = jwtService.generateToken(libraryId, credentials.getUsername());
        log.info("Authentication succeeded username={} libraryId={}", credentials.getUsername(), libraryId);
        return new LoginResponse(token, libraryId, credentials.getUsername());
    }
}
