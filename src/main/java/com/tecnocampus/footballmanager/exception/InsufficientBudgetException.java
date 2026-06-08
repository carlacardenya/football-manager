package com.tecnocampus.footballmanager.exception;

public class InsufficientBudgetException extends RuntimeException {
    public InsufficientBudgetException(String teamName, int required, int available) {
        super("Team " + teamName + " doesn't have enough budget. Required: " + required + ", Available: " + available);
    }
}
