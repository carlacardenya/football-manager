package com.tecnocampus.footballmanager.dto;

public record CreatePlayerDTO(
        String name,
        String position,
        int skillLevel
) {}
