package com.kaloyan.taskboard.api.exception;

public class UsernameAlreadyExistsException extends RuntimeException {

    public UsernameAlreadyExistsException(String username) {
        super("Username already taken: " + username);
    }
}
