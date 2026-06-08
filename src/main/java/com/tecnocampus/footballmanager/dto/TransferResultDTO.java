package com.tecnocampus.footballmanager.dto;

public record TransferResultDTO(
        PlayerDTO player,
        int transferFee,
        int fromTeamBudget,
        int toTeamBudget
) {}
