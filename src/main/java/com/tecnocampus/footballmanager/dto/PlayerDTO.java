package com.tecnocampus.footballmanager.dto;

public record PlayerDTO(
        String id,
        String name,
        String position,
        int skillLevel,
        String status,
        int goals,
        int gamesPlayed
) {}
