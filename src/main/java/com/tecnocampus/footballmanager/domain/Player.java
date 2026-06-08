package com.tecnocampus.footballmanager.domain;

import com.tecnocampus.footballmanager.dto.CreatePlayerDTO;
import com.tecnocampus.footballmanager.dto.PlayerDTO;
import com.tecnocampus.footballmanager.dto.UpdatePlayerDTO;
import com.tecnocampus.footballmanager.exception.InvalidDataException;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

import static java.util.Arrays.stream;

public class Player {

    private final String id = UUID.randomUUID().toString();
    private String name;
    private Position position;
    private int skillLevel;
    private PlayerStatus status = PlayerStatus.FREE_AGENT;
    private int goals = 0;
    private int gamesPlayed = 0;
    private final LocalDateTime createdAt = LocalDateTime.now();

    public Player() {
        // Empty constructor (e.g. for frameworks that build the object via reflection
        // and set fields afterwards — JPA, Jackson, etc.).
    }

    public Player(CreatePlayerDTO dto) {
        validateName(dto.name());
        validateSkillLevel(dto.skillLevel());
        this.name = dto.name();
        this.position = parsePosition(dto.position());
        this.skillLevel = dto.skillLevel();
    }

    // --- Getters ---
    public String getId() { return id; }
    public String getName() { return name; }
    public Position getPosition() { return position; }
    public int getSkillLevel() { return skillLevel; }
    public PlayerStatus getStatus() { return status; }
    public int getGoals() { return goals; }
    public int getGamesPlayed() { return gamesPlayed; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    // --- Domain operations ---

    public void update(UpdatePlayerDTO dto) {
        validateName(dto.name());
        validateSkillLevel(dto.skillLevel());
        this.name = dto.name();
        this.skillLevel = dto.skillLevel();
    }

    // TODO: Mark the player's status as SIGNED.
    public void markAsSigned() {
        this.status = PlayerStatus.SIGNED;
    }

    // TODO: Mark the player's status as FREE_AGENT.
    public void markAsFreeAgent() {
        this.status = PlayerStatus.FREE_AGENT;
    }

    // TODO: Mark the player's status as INJURED.
    public void markAsInjured() {
        this.status = PlayerStatus.INJURED;
    }

    // TODO: Recover from an injury.
    //  - Validate current status is INJURED, otherwise throw InvalidDataException
    //  - Change status to SIGNED
    public void recover() {
        if (this.status != PlayerStatus.INJURED){
            throw new InvalidDataException("The current status isn't injured");
        }
        this.status = PlayerStatus.SIGNED;
    }

    public void addGoal() { this.goals++; }
    public void addGamePlayed() { this.gamesPlayed++; }

    // --- DTO conversion ---
    public PlayerDTO toDTO() {
        return new PlayerDTO(
                id,
                name,
                position.name(),
                skillLevel,
                status.name(),
                goals,
                gamesPlayed
        );
    }

    // --- Validation helpers ---

    // TODO: Validate that name is at least 2 characters.
    //  Throw InvalidDataException with a descriptive message if invalid.
    private void validateName(String name) {
        if (name == null || name.trim().length() < 2) {
            throw new InvalidDataException("Player must have at least 2 characters.");
        }
    }

    // TODO: Validate that skillLevel is between 1 and 99 (inclusive).
    //  Throw InvalidDataException with a descriptive message if invalid.
    private void validateSkillLevel(int skillLevel) {
        if (skillLevel < 1 || skillLevel > 99) {
            throw new InvalidDataException("SkillLevel must be between 1 and 99");
        }
    }

    // TODO: Parse the position string into a Position enum.
    //  - Throw InvalidDataException if position is null or not a valid enum value.
    private Position parsePosition(String position) {
        return stream(Position.values())
                .filter (pos -> position != null && pos.name().equalsIgnoreCase(position.trim()))
                .findFirst()
                .orElseThrow(() -> new InvalidDataException("Position is null or not a valid enum value"));
    }
}
