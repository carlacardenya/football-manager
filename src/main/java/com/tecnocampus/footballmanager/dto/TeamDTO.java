package com.tecnocampus.footballmanager.dto;

import java.util.List;

public record TeamDTO(
        String id,
        String name,
        String city,
        int budget,
        int wins,
        int draws,
        int losses,
        int points,
        int goalsFor,
        int goalsAgainst,
        int goalDifference,
        int teamPower,
        List<PlayerDTO> players
) {}
