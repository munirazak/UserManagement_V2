package com.kopibru.usermanagement.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "borrowers")
public class Borrower {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotBlank(message = "libraryId is required")
    @Column(name = "library_id", nullable = false, unique = true, length = 100)
    private String libraryId;

    @NotBlank(message = "name is required")
    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @NotBlank(message = "email is required")
    @Email(message = "email must be valid")
    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 50)
    private BorrowerRole role = BorrowerRole.BORROWER;

    public Borrower() {
    }

    public Borrower(String libraryId, String name, String email) {
        this(libraryId, name, email, BorrowerRole.BORROWER);
    }

    public Borrower(String libraryId, String name, String email, BorrowerRole role) {
        this.libraryId = libraryId;
        this.name = name;
        this.email = email;
        this.role = role != null ? role : BorrowerRole.BORROWER;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public BorrowerRole getRole() {
        return role;
    }

    public void setRole(BorrowerRole role) {
        this.role = role != null ? role : BorrowerRole.BORROWER;
    }
}
