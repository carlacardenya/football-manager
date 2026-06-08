package com.tecnocampus.footballmanager.api;

import com.tecnocampus.footballmanager.domain.Match;
import com.tecnocampus.footballmanager.domain.Team;
import com.tecnocampus.footballmanager.dto.MatchDTO;
import com.tecnocampus.footballmanager.dto.PlayMatchDTO;
import com.tecnocampus.footballmanager.exception.InvalidDataException;
import com.tecnocampus.footballmanager.exception.MatchNotFoundException;
import com.tecnocampus.footballmanager.exception.TeamNotFoundException;
import com.tecnocampus.footballmanager.repository.MatchRepository;
import com.tecnocampus.footballmanager.repository.TeamRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class MatchRestController {

    // The repositories are injected by Spring through the constructor
    private final MatchRepository matchRepository;
    private final TeamRepository teamRepository;

    public MatchRestController(MatchRepository matchRepository, TeamRepository teamRepository) {
        this.matchRepository = matchRepository;
        this.teamRepository = teamRepository;
    }

    // --- POST /matches ---
    // TODO: Play a match between two teams
    // - Look up both teams (throw TeamNotFoundException if missing)
    // - Build the Match: new Match(home, away) — the constructor runs the full
    //   simulation (validations, goals, stat updates, injuries) inside the domain
    // - Save the match with matchRepository.save(match)
    // - Return MatchDTO with status 201 CREATED
    @PostMapping("/matches")
    @ResponseStatus(HttpStatus.CREATED)
    public MatchDTO playMatch(@RequestBody PlayMatchDTO dto) {
        Team homeTeam = teamRepository.findById(dto.homeTeamId());
        Team awayTeam = teamRepository.findById(dto.awayTeamId());

        if (dto.homeTeamId().equals(dto.awayTeamId())) {
            throw new InvalidDataException("A team cannot play against itself");
        }

        Match match = new Match(homeTeam, awayTeam);
        Match savedMatch = matchRepository.save(match);

        return savedMatch.toDTO();
    }

        // --- GET /matches ---
        // TODO: List all matches, ordered by most recent first
        // - Return List<MatchDTO>
        @GetMapping("/matches")
        public List<MatchDTO> listMatches() {
            return matchRepository.findAll().stream()
                    .sorted(Comparator.comparing(Match::getPlayedAt).reversed())
                    .map(Match::toDTO)
                    .collect(Collectors.toList());
        }

        // --- GET /matches/{matchId} ---
        // TODO: Get a specific match by ID
        // - Return MatchDTO, or 404 if not found
        @GetMapping("/matches/{matchId}")
        public MatchDTO getMatch(@PathVariable String matchId) {
            Match match = matchRepository.findById(matchId);
            return match.toDTO();
        }

        // --- GET /teams/{teamId}/matches ---
        // TODO: Get all matches for a specific team
        // - Verify the team exists first (throw TeamNotFoundException)
        // - Filter matches where team is home OR away
        // - Return List<MatchDTO> sorted by most recent first
        @GetMapping("/teams/{teamId}/matches")
        public List<MatchDTO> getTeamMatches(@PathVariable String teamId) {
            teamRepository.findById(teamId);

            return matchRepository.findAll().stream()
                    .filter(m -> m.getHomeTeam().getId().equals(teamId) || m.getAwayTeam().getId().equals(teamId))
                    .sorted(Comparator.comparing(Match::getPlayedAt).reversed())
                    .map(Match::toDTO)
                    .toList();
        }

        // --- Exception Handlers ---
        // TODO: Add @ExceptionHandler methods for:
        // - MatchNotFoundException → 404
        // - TeamNotFoundException → 404
        // - InvalidDataException → 400
        @ExceptionHandler({MatchNotFoundException.class, TeamNotFoundException.class})
        @ResponseStatus(HttpStatus.NOT_FOUND)
        public Map<String, Object> handleNotFound (Exception ex){
            return createErrorResponse(ex.getClass().getSimpleName(), ex.getMessage(), HttpStatus.NOT_FOUND);
        }

        @ExceptionHandler(InvalidDataException.class)
        @ResponseStatus(HttpStatus.BAD_REQUEST)
        public Map<String, Object> handleBadRequest (InvalidDataException ex){
            return createErrorResponse("InvalidDataException", ex.getMessage(), HttpStatus.BAD_REQUEST);
        }

        private Map<String, Object> createErrorResponse (String errorName, String message, HttpStatus status){
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("error", errorName);
            error.put("message", message);
            error.put("status", status.value());
            return error;
        }
}
