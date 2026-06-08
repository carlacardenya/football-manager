package com.tecnocampus.footballmanager.domain;

import com.tecnocampus.footballmanager.dto.MatchDTO;
import com.tecnocampus.footballmanager.exception.InvalidDataException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Stream;

public class Match {

    private static final Random RANDOM = new Random();
    private static final int GOALS_PER_SIDE_MAX = 5;      // each side scores 1..5 goals

    private String id = UUID.randomUUID().toString();
    private Team homeTeam;
    private Team awayTeam;
    private int homeGoals;
    private int awayGoals;
    private MatchResult result;
    private List<String> scorers;
    private LocalDateTime playedAt = LocalDateTime.now();

    public Match() {
        // Empty constructor (e.g. for frameworks that build the object via reflection
        // and set fields afterwards — JPA, Jackson, etc.).
    }

    public Match(Team homeTeam, Team awayTeam) {
        this.homeTeam = homeTeam;
        this.awayTeam = awayTeam;
        validateDifferentTeams();
        validateAvailablePlayers();
        play();
    }

    /** Orchestrates the simulation. Each step is its own method below. */
    private void play() {
        generateGoals();
        determineResult();
        updateTeamStats();
        updatePlayerStats();
        distributeGoals();
        applyInjuries();
    }

    // TODO: Validate that home and away are not the same team.
    //  Throw InvalidDataException if they have the same id.
    private void validateDifferentTeams() {
        if (this.homeTeam.getId().equals(this.awayTeam.getId())){
            throw new InvalidDataException("They can't have the same id");
        }
    }

    // TODO: Validate that both teams have at least one available player.
    //  Throw InvalidDataException naming which side has none.
    private void validateAvailablePlayers() {
        if(this.homeTeam.getAvailablePlayers().isEmpty()){
            throw new InvalidDataException("Home team must have at least one available player");
        }

        if(this.awayTeam.getAvailablePlayers().isEmpty()){
            throw new InvalidDataException("Away team must have at least one available player");
        }
    }

    // TODO: Generate this.homeGoals and this.awayGoals.
    //  Each side rolls a goal count between 1 and GOALS_PER_SIDE_MAX (inclusive).
    //  Hint: RANDOM.nextInt(N) returns 0..N-1, so add 1 to land in 1..N.
    private void generateGoals() {
        this.homeGoals = RANDOM.nextInt(GOALS_PER_SIDE_MAX)+1;
        this.awayGoals = RANDOM.nextInt(GOALS_PER_SIDE_MAX)+1;
    }

    // TODO: Set this.result based on homeGoals vs awayGoals (HOME_WIN, AWAY_WIN, DRAW).
    private void determineResult() {
        if (this.homeGoals > this.awayGoals){
            this.result = MatchResult.HOME_WIN;
        }
        else if(this.awayGoals > this.homeGoals){
            this.result = MatchResult.AWAY_WIN;
        }
        else {
            this.result = MatchResult.DRAW;
        }
    }

    // TODO: Update team stats:
    //  - addGoalsFor / addGoalsAgainst on each team
    //  - addWin / addDraw / addLoss based on this.result
    private void updateTeamStats() {
        Statistics homeStats = this.homeTeam.getStatistics();
        Statistics awayStats = this.awayTeam.getStatistics();

        homeStats.addGoalsFor(this.homeGoals);
        homeStats.addGoalsAgainst(this.awayGoals);
        awayStats.addGoalsFor(this.awayGoals);
        awayStats.addGoalsAgainst(this.homeGoals);

        if (this.result == MatchResult.HOME_WIN) {
            homeStats.addWin();
            awayStats.addLoss();
        } else if (this.result == MatchResult.AWAY_WIN) {
            awayStats.addWin();
            homeStats.addLoss();
        } else {
            homeStats.addDraw();
            awayStats.addDraw();
        }
    }

    // TODO: Increment gamesPlayed for each available player on both teams.
    //  Use player.addGamePlayed().
    private void updatePlayerStats() {
        Stream.of(this.homeTeam.getAvailablePlayers(), this.awayTeam.getAvailablePlayers())
                .flatMap(List::stream)
                .forEach(Player::addGamePlayed);
    }

    // TODO: Initialize this.scorers and record the goals scored by each side.
    //  - Use homeTeam.pickScorers(homeGoals) and awayTeam.pickScorers(awayGoals)
    //    to get the players who scored on each side. (Team.pickScorers already
    //    handles the FWD/MID weighting for you.)
    //  - For each scorer returned: scorer.addGoal(); this.scorers.add(scorer.getName())
    private void distributeGoals() {
        this.scorers = new ArrayList<>();

        List<Player> homeScorers = this.homeTeam.pickScorers(this.homeGoals);
        List<Player> awayScorers = this.awayTeam.pickScorers(this.awayGoals);

        homeScorers.forEach(Player::addGoal);
        homeScorers.stream()
                .map(Player::getName)
                .forEach(this.scorers::add);

        awayScorers.forEach(Player::addGoal);
        awayScorers.stream()
                .map(Player::getName)
                .forEach(this.scorers::add);
    }

    // TODO: 10% injury chance per side.
    //  - For each side, with 10% probability (RANDOM.nextInt(10) == 0),
    //    pick a random available player and call player.markAsInjured().
    private void applyInjuries() {
        if (RANDOM.nextInt(10) == 0) {
            List<Player> homeAvailable = this.homeTeam.getAvailablePlayers();
            if (!homeAvailable.isEmpty()) {
                homeAvailable.get(RANDOM.nextInt(homeAvailable.size())).markAsInjured();
            }
        }

        if (RANDOM.nextInt(10) == 0) {
            List<Player> awayAvailable = this.awayTeam.getAvailablePlayers();
            if (!awayAvailable.isEmpty()) {
                awayAvailable.get(RANDOM.nextInt(awayAvailable.size())).markAsInjured();
            }
        }
    }

    public String getId() { return id; }
    public Team getHomeTeam() { return homeTeam; }
    public Team getAwayTeam() { return awayTeam; }
    public int getHomeGoals() { return homeGoals; }
    public int getAwayGoals() { return awayGoals; }
    public MatchResult getResult() { return result; }
    public List<String> getScorers() { return scorers; }
    public LocalDateTime getPlayedAt() { return playedAt; }

    // --- DTO conversion ---
    public MatchDTO toDTO() {
        return new MatchDTO(
                id,
                homeTeam.getName(),
                awayTeam.getName(),
                homeGoals,
                awayGoals,
                result.name(),
                scorers,
                playedAt
        );
    }
}
