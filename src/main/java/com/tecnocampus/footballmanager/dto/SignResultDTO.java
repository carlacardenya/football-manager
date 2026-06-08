package com.tecnocampus.footballmanager.dto;

public record SignResultDTO(
        PlayerDTO player,
        int cost,
        int teamBudgetRemaining
) {}
