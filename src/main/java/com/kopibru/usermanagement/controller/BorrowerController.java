package com.kopibru.usermanagement.controller;

import com.kopibru.usermanagement.dto.BorrowerRegistrationRequest;
import com.kopibru.usermanagement.model.Borrower;
import com.kopibru.usermanagement.service.BorrowerService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/borrowers")
public class BorrowerController {

    private static final Logger log = LoggerFactory.getLogger(BorrowerController.class);

    private final BorrowerService borrowerService;

    public BorrowerController(BorrowerService borrowerService) {
        this.borrowerService = borrowerService;
    }

    @PostMapping
    public ResponseEntity<Borrower> registerBorrower(
            @Valid @RequestBody BorrowerRegistrationRequest request) {
        log.info("POST /api/borrowers libraryId={} username={}", request.getLibraryId(), request.getUsername());
        Borrower registered = borrowerService.register(request);
        log.info("Borrower registered id={} libraryId={}", registered.getId(), registered.getLibraryId());
        return ResponseEntity.status(HttpStatus.CREATED).body(registered);
    }

    @GetMapping
    public ResponseEntity<List<Borrower>> getAllBorrowers() {
        log.info("GET /api/borrowers");
        List<Borrower> borrowers = borrowerService.getAllBorrowers();
        log.info("Returning {} borrowers", borrowers.size());
        return ResponseEntity.ok(borrowers);
    }

    @GetMapping("/{libraryId}")
    public ResponseEntity<Borrower> getBorrower(@PathVariable String libraryId) {
        log.info("GET /api/borrowers/{}", libraryId);
        Borrower borrower = borrowerService.getByLibraryId(libraryId);
        log.info("Found borrower id={} libraryId={}", borrower.getId(), borrower.getLibraryId());
        return ResponseEntity.ok(borrower);
    }
}
