package com.tecnocampus.footballmanager.exception;


public class PlayerNotFoundException extends RuntimeException {
    public PlayerNotFoundException(String id) {
        super("Player not found with id: " + id);
    }
}
