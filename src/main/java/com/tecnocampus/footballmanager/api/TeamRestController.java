package com.tecnocampus.footballmanager.api;

import com.tecnocampus.footballmanager.domain.Player;
import com.tecnocampus.footballmanager.domain.PlayerStatus;
import com.tecnocampus.footballmanager.domain.Team;
import com.tecnocampus.footballmanager.dto.*;
import com.tecnocampus.footballmanager.exception.*;
import com.tecnocampus.footballmanager.repository.PlayerRepository;
import com.tecnocampus.footballmanager.repository.TeamRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/teams")
public class TeamRestController {

    // The repositories are injected by Spring through the constructor
    private final TeamRepository teamRepository;
    private final PlayerRepository playerRepository;

    public TeamRestController(TeamRepository teamRepository, PlayerRepository playerRepository) {
        this.teamRepository = teamRepository;
        this.playerRepository = playerRepository;
    }

    // --- POST /teams ---
    // TODO: Create a new team
    // - Check name is unique (case insensitive), throw DuplicateTeamException
    // - Build a new Team by passing the CreateTeamDTO to its constructor
    //   (the domain validates name and city)
    // - Save with teamRepository.save(team)
    // - Return TeamDTO with status 201 CREATED
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TeamDTO createTeam(@RequestBody CreateTeamDTO dto) {
        boolean exists = teamRepository.findAll().stream()
                .anyMatch(t -> t.getName().equalsIgnoreCase(dto.name()));

        if (exists) {
            throw new DuplicateTeamException("A team with this name already exists.");
        }

        Team team = new Team(dto);
        Team savedTeam = teamRepository.save(team);
        return savedTeam.toDTO();
    }

    // --- GET /teams ---
    // TODO: List all teams
    // - Return List<TeamDTO>
    @GetMapping
    public List<TeamDTO> listTeams() {
        return teamRepository.findAll().stream()
                .map(Team::toDTO)
                .toList();
    }

    // --- GET /teams/league-table ---
    // TODO: Return teams sorted by league standings
    // - Sort by: points (desc), then goalDifference (desc), then goalsFor (desc)
    @GetMapping("/league-table")
    public List<TeamDTO> getLeagueTable() {
        return teamRepository.findAll().stream()
                .sorted(Comparator
                        .comparingInt((Team t) -> t.getStatistics().getPoints()).reversed()
                        .thenComparingInt(t -> t.getStatistics().getGoalDifference()).reversed()
                        .thenComparingInt(t -> t.getStatistics().getGoalsFor()).reversed())
                .map(Team::toDTO)
                .toList();
    }

    // --- GET /teams/{teamId} ---
    // TODO: Get a specific team
    // - Use teamRepository.findById(teamId) (throws TeamNotFoundException if missing)
    // - Convert to a DTO with team.toDTO() — TeamDTO already includes the player list
    @GetMapping("/{teamId}")
    public TeamDTO getTeam(@PathVariable String teamId) {
        Team team = teamRepository.findById(teamId); // Lanza TeamNotFoundException internamente si no existe
        return team.toDTO();
    }

    // --- DELETE /teams/{teamId} ---
    // TODO: Delete a team
    // - For each player in the team, call player.markAsFreeAgent()
    // - Use teamRepository.delete(team)
    // - Return 204 NO CONTENT
    @DeleteMapping("/{teamId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTeam(@PathVariable String teamId) {
        Team team = teamRepository.findById(teamId);
        team.getPlayers().forEach(Player::markAsFreeAgent);
        teamRepository.delete(team);
    }

    // --- POST /teams/{teamId}/sign/{playerId} ---
    // TODO: Sign a free agent to a team
    // - Player must be FREE_AGENT (else throw InvalidDataException)
    // - Cost = skillLevel × 2
    // - Use team.spend(cost)            — domain validates budget (InsufficientBudgetException)
    // - Use team.addPlayer(player)      — domain validates capacity (TeamFullException)
    // - Use player.markAsSigned()
    // - Return SignResultDTO
    @PostMapping("/{teamId}/sign/{playerId}")
    public SignResultDTO signPlayer(@PathVariable String teamId, @PathVariable String playerId) {
        Team team = teamRepository.findById(teamId);
        Player player = playerRepository.findById(playerId);

        if (player.getStatus() != PlayerStatus.FREE_AGENT) {
            throw new InvalidDataException("Player is not a free agent.");
        }

        int cost = player.getSkillLevel() * 2;

        team.spend(cost);
        team.addPlayer(player);
        player.markAsSigned();

        return new SignResultDTO(player.toDTO(), cost, team.getBudget());
    }

    // --- POST /teams/{teamId}/release/{playerId} ---
    // TODO: Release a player from a team
    // - Player must belong to this team (else throw InvalidDataException)
    // - Refund = skillLevel × 1
    // - Use team.removePlayer(player), team.refund(refund), player.markAsFreeAgent()
    // - Return ReleaseResultDTO
    @PostMapping("/{teamId}/release/{playerId}")
    public ReleaseResultDTO releasePlayer(@PathVariable String teamId, @PathVariable String playerId) {
        Team team = teamRepository.findById(teamId);
        Player player = playerRepository.findById(playerId);

        if (!team.getPlayers().contains(player)) {
            throw new InvalidDataException("Player doesn't belong to this team");
        }

        int refund = player.getSkillLevel() * 1;
        team.removePlayer(player);
        team.refund(refund);
        player.markAsFreeAgent();

        return new ReleaseResultDTO(player.toDTO(), refund,team.getBudget());
    }

    // --- POST /teams/{fromTeamId}/transfer/{playerId}/to/{toTeamId} ---
    // TODO: Transfer a player between teams
    // - Cannot transfer to same team
    // - Player must belong to fromTeam
    // - Player must NOT be INJURED
    // - Transfer fee = skillLevel × 3
    // - Use toTeam.spend(fee), toTeam.addPlayer(player) — these validate budget & capacity
    // - Use fromTeam.removePlayer(player), fromTeam.refund(fee)
    // - Player stays SIGNED — no status change needed
    // - Return TransferResultDTO
    @PostMapping("/{fromTeamId}/transfer/{playerId}/to/{toTeamId}")
    public TransferResultDTO transferPlayer(
            @PathVariable String fromTeamId,
            @PathVariable String playerId,
            @PathVariable String toTeamId) {

        if (fromTeamId.equals(toTeamId)) {
            throw new InvalidDataException("Cannot transfer a player to the same team");
        }

        Team fromTeam = teamRepository.findById(fromTeamId);
        Team toTeam = teamRepository.findById(toTeamId);
        Player player = playerRepository.findById(playerId);

        if (!fromTeam.getPlayers().contains(player)) {
            throw new InvalidDataException("Player doesn't belong to the origin team");
        }
        if (player.getStatus() == PlayerStatus.INJURED) {
            throw new InvalidDataException("Injured players cannot be transferred");
        }

        int fee = player.getSkillLevel() * 3;

        toTeam.spend(fee);
        toTeam.addPlayer(player);
        fromTeam.removePlayer(player);
        fromTeam.refund(fee);

        return new TransferResultDTO(player.toDTO(), fee, fromTeam.getBudget(), toTeam.getBudget());
    }

    // --- Exception Handlers ---
    // TODO: Add @ExceptionHandler methods for:
    // - TeamNotFoundException → 404
    // - PlayerNotFoundException → 404
    // - DuplicateTeamException → 409
    // - InvalidDataException → 400
    // - InsufficientBudgetException → 400
    // - TeamFullException → 400
    @ExceptionHandler({TeamNotFoundException.class, PlayerNotFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, Object> handleNotFound(Exception ex) {
        return createErrorResponse(ex.getClass().getSimpleName(), ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(DuplicateTeamException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, Object> handleDuplicate(DuplicateTeamException ex) {
        return createErrorResponse("DuplicateTeamException", ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler({InvalidDataException.class, InsufficientBudgetException.class, TeamFullException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleBadRequest(Exception ex) {
        return createErrorResponse(ex.getClass().getSimpleName(), ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    private Map<String, Object> createErrorResponse(String errorName, String message, HttpStatus status) {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("error", errorName);
        error.put("message", message);
        error.put("status", status.value());
        return error;
    }
}
