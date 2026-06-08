package com.tecnocampus.footballmanager.exception;

public class TeamFullException extends RuntimeException {
    public TeamFullException(String teamName) {
        super("Team " + teamName + " already has 11 players");
    }
}
