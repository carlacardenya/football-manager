package com.tecnocampus.footballmanager.exception;


public class TeamNotFoundException extends RuntimeException {
    public TeamNotFoundException(String id) {
        super("Team not found with id: " + id);
    }
}
