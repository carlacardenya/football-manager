package com.tecnocampus.footballmanager.api;

import com.tecnocampus.footballmanager.domain.Player;
import com.tecnocampus.footballmanager.domain.PlayerStatus;
import com.tecnocampus.footballmanager.domain.Position;
import com.tecnocampus.footballmanager.dto.CreatePlayerDTO;
import com.tecnocampus.footballmanager.dto.PlayerDTO;
import com.tecnocampus.footballmanager.dto.UpdatePlayerDTO;
import com.tecnocampus.footballmanager.exception.InvalidDataException;
import com.tecnocampus.footballmanager.exception.PlayerNotFoundException;
import com.tecnocampus.footballmanager.repository.PlayerRepository;
import com.tecnocampus.footballmanager.repository.TeamRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/players")
public class PlayerRestController {

    // The repositories are injected by Spring through the constructor
    private final PlayerRepository playerRepository;
    private final TeamRepository teamRepository;

    public PlayerRestController(PlayerRepository playerRepository, TeamRepository teamRepository) {
        this.playerRepository = playerRepository;
        this.teamRepository = teamRepository;
    }

    // --- POST /players ---
    // TODO: Register a new player as a free agent
    // - Build a new Player by passing the CreatePlayerDTO to its constructor
    //   (the domain validates name, skillLevel, and parses position)
    // - Save it through playerRepository.save(player)
    // - Return PlayerDTO with status 201 CREATED
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PlayerDTO createPlayer (@RequestBody CreatePlayerDTO dto) {
        Player player = new Player(dto);
        Player savedPlayer = playerRepository.save(player);
        return savedPlayer.toDTO();
    }

    // --- GET /players ---
    // TODO: List all players with optional filtering
    // - @RequestParam position (optional): filter by Position
    // - @RequestParam status (optional): filter by PlayerStatus
    // - Use playerRepository.findAll() and stream-filter
    // - Return List<PlayerDTO>
    @GetMapping
    public List<PlayerDTO> getPlayers (@RequestParam(required = false) Position position, @RequestParam (required = false) PlayerStatus playerStatus){
        return playerRepository.findAll().stream()
                .filter(p-> position == null || p.getPosition().equals(position))
                .filter(p-> playerStatus == null || p.getStatus().equals(playerStatus))
                .map(Player::toDTO)
                .toList();
    }

    // --- GET /players/top-scorers ---
    // TODO: Return top 10 players by goals scored
    // - Tiebreaker 1: fewer games played
    // - Tiebreaker 2: higher skillLevel
    @GetMapping("/top-scorers")
    public List<PlayerDTO> getTopScorers() {
        return playerRepository.findAll().stream()
                .sorted(Comparator
                        .comparingInt(Player::getGoals).reversed()
                        .thenComparingInt(Player::getGamesPlayed)
                        .thenComparing(Comparator.comparingInt(Player::getSkillLevel).reversed()))
                .limit(10)
                .map(Player::toDTO)
                .toList();
    }


    // --- GET /players/{playerId} ---
    // TODO: Get a specific player by ID
    // - Return PlayerDTO, or 404 if not found
        @GetMapping("/{id}")
        public PlayerDTO getPlayerById(@PathVariable String id){
            Player player = playerRepository.findById(id);
            return player.toDTO();
        }

    // --- PUT /players/{playerId} ---
    // TODO: Update player name and skillLevel (position cannot change)
    // - Pass the UpdatePlayerDTO to player.update(dto) — the domain validates name and skillLevel
    // - Return updated PlayerDTO
    @PutMapping("/{playerId}")
    public PlayerDTO updatePlayer (@PathVariable String playerId, @RequestBody UpdatePlayerDTO dto){
    Player player = playerRepository.findById(playerId);
    player.update(dto);
    return player.toDTO();
    }

    // --- DELETE /players/{playerId} ---
    // TODO: Delete a player
    // - Remove the player from any team that has them on the roster
    //   (loop teamRepository.findAll() and call team.removePlayer(player))
    // - Then playerRepository.delete(player)
    // - Return 204 NO CONTENT
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePlayer(@PathVariable String id){
        Player player = playerRepository.findById(id);
        teamRepository.findAll().forEach(team -> team.removePlayer(player));
        playerRepository.delete(player);
    }

    // --- POST /players/{playerId}/recover ---
    // TODO: Recover an injured player
    // - Call player.recover() — the domain validates that the player is INJURED
    // - Return updated PlayerDTO
    @PostMapping("/{id}/recover")
    public PlayerDTO recoverPlayer(@PathVariable String id) {
        Player player = playerRepository.findById(id);
        player.recover();
        return player.toDTO();
    }

    // --- Exception Handlers ---
    // TODO: Add @ExceptionHandler methods for:
    // - PlayerNotFoundException → 404, return error JSON
    // - InvalidDataException → 400, return error JSON
    // Error format: { "error": "ExceptionName", "message": "...", "status": 404/400 }
    @ExceptionHandler(PlayerNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, Object> handlePlayerNotFound(PlayerNotFoundException ex) {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("error", "PlayerNotFoundException");
        error.put("message", ex.getMessage());
        error.put("status", HttpStatus.NOT_FOUND.value());
        return error;
    }

    @ExceptionHandler(InvalidDataException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleInvalidData(InvalidDataException ex) {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("error", "InvalidDataException");
        error.put("message", ex.getMessage());
        error.put("status", HttpStatus.BAD_REQUEST.value());
        return error;
    }
}
