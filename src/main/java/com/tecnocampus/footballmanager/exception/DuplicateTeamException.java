package com.tecnocampus.footballmanager.exception;

public class DuplicateTeamException extends RuntimeException {
    public DuplicateTeamException(String name) {
        super("Team with name '" + name + "' already exists");
    }
}
