package com.tecnocampus.footballmanager.dto;

import java.time.LocalDateTime;
import java.util.List;

public record MatchDTO(
        String id,
        String homeTeam,
        String awayTeam,
        int homeGoals,
        int awayGoals,
        String result,
        List<String> scorers,
        LocalDateTime playedAt
) {}
