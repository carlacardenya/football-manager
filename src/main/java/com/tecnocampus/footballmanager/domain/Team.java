package com.tecnocampus.footballmanager.domain;

import com.tecnocampus.footballmanager.dto.CreateTeamDTO;
import com.tecnocampus.footballmanager.dto.TeamDTO;
import com.tecnocampus.footballmanager.exception.InsufficientBudgetException;
import com.tecnocampus.footballmanager.exception.InvalidDataException;
import com.tecnocampus.footballmanager.exception.TeamFullException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Team {

    private static final int MAX_PLAYERS = 11;
    private static final int INITIAL_BUDGET = 1000;
    private static final Random RANDOM = new Random();

    private final String id = UUID.randomUUID().toString();
    private String name;
    private String city;
    private int budget = INITIAL_BUDGET;
    private final Statistics statistics = new Statistics();
    private final List<Player> players = new ArrayList<>();
    private final LocalDateTime createdAt = LocalDateTime.now();

    public Team() {
        // Empty constructor (e.g. for frameworks that build the object via reflection
        // and set fields afterwards — JPA, Jackson, etc.).
    }

    public Team(CreateTeamDTO dto) {
        validateName(dto.name());
        validateCity(dto.city());
        this.name = dto.name();
        this.city = dto.city();
    }

    // --- Getters ---
    public String getId() { return id; }
    public String getName() { return name; }
    public String getCity() { return city; }
    public int getBudget() { return budget; }
    public Statistics getStatistics() { return statistics; }
    public List<Player> getPlayers() { return players; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public int getTeamPower() {
        return players.stream()
                .filter(p -> p.getStatus() == PlayerStatus.SIGNED)
                .mapToInt(Player::getSkillLevel)
                .sum();
    }

    public List<Player> getAvailablePlayers() {
        return players.stream()
                .filter(p -> p.getStatus() == PlayerStatus.SIGNED)
                .toList();
    }

    // --- Budget operations ---

    // TODO: Spend an amount from the team's budget.
    //  - Validate the team has enough budget, otherwise throw InsufficientBudgetException
    //  - Decrease budget by amount
    public void spend(int amount) {
        if (this.budget < amount) {
            throw new InsufficientBudgetException(this.name, amount, this.budget);
        }
        this.budget -= amount;
    }

    // TODO: Increase the team's budget by amount.
    public void refund(int amount) {
        this.budget += amount;
    }

    // --- Roster operations ---

    // TODO: Add a player to the roster.
    //  - Validate the team has fewer than MAX_PLAYERS, otherwise throw TeamFullException
    //  - Add the player to the list
    public void addPlayer(Player player) {
        if (this.players.size() >= MAX_PLAYERS) {
            throw new TeamFullException("The team must have fewer players than " + MAX_PLAYERS);
        }
        this.players.add(player);
    }

    public void removePlayer(Player player) { this.players.remove(player); }

    // --- Scorer selection (pre-implemented for you) ---

    /** Picks `goals` scorers (with repetition) — FWD twice as likely as MID. */
    public List<Player> pickScorers(int goals) {
        List<Player> pool = buildScoringPool();
        List<Player> scorers = new ArrayList<>();
        for (int i = 0; i < goals; i++) {
            scorers.add(pool.get(RANDOM.nextInt(pool.size())));
        }
        return scorers;
    }

    private List<Player> buildScoringPool() {
        List<Player> pool = new ArrayList<>();
        for (Player p : getAvailablePlayers()) {
            for (int i = 0; i < scoringWeight(p); i++) pool.add(p);
        }
        if (pool.isEmpty()) pool.addAll(getAvailablePlayers());
        return pool;
    }

    private int scoringWeight(Player p) {
        return switch (p.getPosition()) {
            case FWD -> 2;
            case MID -> 1;
            default -> 0;
        };
    }

    // --- DTO conversion ---
    public TeamDTO toDTO() {
        return new TeamDTO(
                id,
                name,
                city,
                budget,
                statistics.getWins(),
                statistics.getDraws(),
                statistics.getLosses(),
                statistics.getPoints(),
                statistics.getGoalsFor(),
                statistics.getGoalsAgainst(),
                statistics.getGoalDifference(),
                getTeamPower(),
                players.stream().map(Player::toDTO).toList()
        );
    }

    // --- Validation helpers ---

    // TODO: Validate that the team name is at least 3 characters.
    //  Throw InvalidDataException with a descriptive message if invalid.
    private void validateName(String name) {
        if (name == null || name.trim().length() < 3) {
            throw new InvalidDataException("The team name must have at least 3 characters");
        }
    }

    // TODO: Validate that the city is not null/empty.
    //  Throw InvalidDataException with a descriptive message if invalid.
    private void validateCity(String city) {
        if (city == null || city.trim().isEmpty()) {
            throw new InvalidDataException("The city cannot be null or empty");
        }
    }
}
