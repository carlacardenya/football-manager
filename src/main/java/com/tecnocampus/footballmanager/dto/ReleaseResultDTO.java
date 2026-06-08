package com.tecnocampus.footballmanager.dto;

public record ReleaseResultDTO(
        PlayerDTO player,
        int refund,
        int teamBudgetRemaining
) {}
