package com.tecnocampus.footballmanager.repository;

import com.tecnocampus.footballmanager.domain.Match;
import com.tecnocampus.footballmanager.exception.MatchNotFoundException;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class MatchRepository {

    // TODO: Create an ArrayList to store matches in memory
    private final List<Match> matches = new ArrayList<>();

    // TODO: Return all matches
    public List<Match> findAll() {
        return matches;
    }

    // TODO: Find a match by id, throw MatchNotFoundException if not found
    public Match findById(String id) {
        return this.matches.stream()
                .filter(m-> m.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new MatchNotFoundException("The match wasn't possible to found"));
    }

    // TODO: Add a match to the in-memory list and return it
    public Match save(Match match) {
        this.matches.add(match);
        return match;
    }

    // TODO: Remove a match from the in-memory list
    public void delete(Match match) {
        this.matches.remove(match);
    }
}
