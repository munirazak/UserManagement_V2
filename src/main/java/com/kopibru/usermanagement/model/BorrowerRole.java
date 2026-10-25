package com.kopibru.usermanagement.model;

/**
 * Access role stored on {@link Borrower}.
 * Default for new registrations is {@link #BORROWER}.
 */
public enum BorrowerRole {
    SUPER_ADMIN,
    ADMIN,
    LIBRARIAN,
    BORROWER
}
