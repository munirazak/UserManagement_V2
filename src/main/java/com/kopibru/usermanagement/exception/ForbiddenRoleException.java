package com.kopibru.usermanagement.exception;

public class ForbiddenRoleException extends RuntimeException {

    public ForbiddenRoleException(String message) {
        super(message);
    }
}
