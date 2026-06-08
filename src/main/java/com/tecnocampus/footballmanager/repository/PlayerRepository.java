package com.tecnocampus.footballmanager.repository;

import com.tecnocampus.footballmanager.domain.Player;
import com.tecnocampus.footballmanager.exception.PlayerNotFoundException;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class PlayerRepository {

    // TODO: Create an ArrayList to store players in memory
    private final List<Player> players = new ArrayList<>();

    // TODO: Return all players
    public List<Player> findAll() {
        return this.players;
    }

    // TODO: Find a player by id, throw PlayerNotFoundException if not found
    public Player findById(String id) {
        return this.players.stream()
                .filter(p -> p.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new PlayerNotFoundException("The player isn't possible to found"));
    }

    // TODO: Add a player to the in-memory list and return it
    public Player save(Player player) {
        this.players.add(player);
        return player;
    }

    // TODO: Remove a player from the in-memory list
    public void delete(Player player) {
        this.players.remove(player);
    }
}
