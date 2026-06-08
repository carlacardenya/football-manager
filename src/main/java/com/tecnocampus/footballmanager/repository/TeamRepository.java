package com.tecnocampus.footballmanager.repository;

import com.tecnocampus.footballmanager.domain.Team;
import com.tecnocampus.footballmanager.exception.TeamNotFoundException;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Repository
public class TeamRepository {

    // TODO: Create an ArrayList to store teams in memory
    private final List<Team> teams = new ArrayList<>();

    // TODO: Return all teams
    public List<Team> findAll() {
        return this.teams;
    }

    // TODO: Find a team by id, throw TeamNotFoundException if not found
    public Team findById(String id) {
        return this.teams.stream()
                .filter(t -> t.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new TeamNotFoundException("Team not found"));
    }

    // TODO: Add a team to the in-memory list and return it
    public Team save(Team team) {
        this.teams.add(team);
        return team;
    }

    // TODO: Remove a team from the in-memory list
    public void delete(Team team) {
        this.teams.remove(team);
    }
}
