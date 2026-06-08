package com.tecnocampus.footballmanager.domain;

public class Statistics {

    private int wins = 0;
    private int draws = 0;
    private int losses = 0;
    private int goalsFor = 0;
    private int goalsAgainst = 0;

    // --- Getters ---
    public int getWins() { return wins; }
    public int getDraws() { return draws; }
    public int getLosses() { return losses; }
    public int getGoalsFor() { return goalsFor; }
    public int getGoalsAgainst() { return goalsAgainst; }

    // --- Computed ---
    public int getPoints() { return wins * 3 + draws; }
    public int getGoalDifference() { return goalsFor - goalsAgainst; }

    // --- Mutators ---
    public void addWin() { this.wins++; }
    public void addDraw() { this.draws++; }
    public void addLoss() { this.losses++; }
    public void addGoalsFor(int goals) { this.goalsFor += goals; }
    public void addGoalsAgainst(int goals) { this.goalsAgainst += goals; }
}
