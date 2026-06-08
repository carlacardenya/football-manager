package com.tecnocampus.footballmanager.api;

import com.tecnocampus.footballmanager.domain.*;
import com.tecnocampus.footballmanager.repository.MatchRepository;
import com.tecnocampus.footballmanager.repository.PlayerRepository;
import com.tecnocampus.footballmanager.repository.TeamRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Pre-implemented "Seed demo data" endpoint.
 *
 * It populates the in-memory repositories with a small but realistic data set
 * (4 teams, 20 players, 20 signings, a round-robin of 6 matches) so the UI
 * has something to show. It uses Java reflection to write directly into the
 * repository lists and the domain object fields — that way the seeder works
 * even when the rest of the API (constructors, validators, simulation,
 * controllers) is still made of TODO stubs.
 *
 * Do not change / use it as an example
 */
@RestController
public class DemoDataController {

    private static final String[][] TEAMS = {
            {"FC Barcelona", "Barcelona"},
            {"Real Madrid CF", "Madrid"},
            {"Atlético de Madrid", "Madrid"},
            {"Sevilla FC", "Sevilla"},
    };

    /** Per-team roster: { name, position, skill }. Five players per team. */
    private static final String[][][] ROSTERS = {
            {
                    {"Marc-André ter Stegen", "GK",  "88"},
                    {"Ronald Araújo",         "DEF", "84"},
                    {"Pedri",                 "MID", "87"},
                    {"Frenkie de Jong",       "MID", "85"},
                    {"Lamine Yamal",          "FWD", "88"},
            },
            {
                    {"Thibaut Courtois",      "GK",  "89"},
                    {"Antonio Rüdiger",       "DEF", "85"},
                    {"Jude Bellingham",       "MID", "90"},
                    {"Federico Valverde",     "MID", "87"},
                    {"Vinícius Júnior",       "FWD", "91"},
            },
            {
                    {"Jan Oblak",             "GK",  "87"},
                    {"José Giménez",          "DEF", "82"},
                    {"Koke",                  "MID", "83"},
                    {"Rodrigo De Paul",       "MID", "81"},
                    {"Antoine Griezmann",     "FWD", "87"},
            },
            {
                    {"Ørjan Nyland",          "GK",  "78"},
                    {"Loïc Badé",             "DEF", "79"},
                    {"Lucas Ocampos",         "MID", "80"},
                    {"Jesús Navas",           "MID", "78"},
                    {"Youssef En-Nesyri",     "FWD", "82"},
            },
    };

    private final TeamRepository teamRepo;
    private final PlayerRepository playerRepo;
    private final MatchRepository matchRepo;

    public DemoDataController(TeamRepository teamRepo, PlayerRepository playerRepo, MatchRepository matchRepo) {
        this.teamRepo = teamRepo;
        this.playerRepo = playerRepo;
        this.matchRepo = matchRepo;
    }

    @PostMapping("/init-data")
    public Map<String, Object> initData() throws Exception {
        clearList(teamRepo, "teams");
        clearList(playerRepo, "players");
        clearList(matchRepo, "matches");

        Team[] teams = new Team[TEAMS.length];
        for (int i = 0; i < TEAMS.length; i++) {
            teams[i] = makeTeam(TEAMS[i][0], TEAMS[i][1]);
            addToList(teamRepo, "teams", teams[i]);
        }

        int playersCreated = 0, signed = 0;
        for (int i = 0; i < teams.length; i++) {
            for (String[] row : ROSTERS[i]) {
                Player p = makePlayer(row[0], row[1], Integer.parseInt(row[2]));
                addToList(playerRepo, "players", p);
                playersCreated++;
                signPlayer(teams[i], p);
                signed++;
            }
        }

        // Round-robin: every team plays every other team once. 4 teams → 6 matches.
        int matches = 0;
        for (int i = 0; i < teams.length; i++) {
            for (int j = i + 1; j < teams.length; j++) {
                Match m = makeMatch(teams[i], teams[j]);
                addToList(matchRepo, "matches", m);
                matches++;
            }
        }

        return Map.of(
                "teams", teams.length,
                "players", playersCreated,
                "signings", signed,
                "matches", matches
        );
    }

    // ------------------------------------------------------------------
    // Domain object construction (reflection-based to bypass student TODOs)
    // ------------------------------------------------------------------

    private Team makeTeam(String name, String city) throws Exception {
        Team t = new Team();
        setField(t, "name", name);
        setField(t, "city", city);
        return t;
    }

    private Player makePlayer(String name, String position, int skill) throws Exception {
        Player p = new Player();
        setField(p, "name", name);
        setField(p, "position", Position.valueOf(position));
        setField(p, "skillLevel", skill);
        return p;
    }

    @SuppressWarnings("unchecked")
    private void signPlayer(Team team, Player player) throws Exception {
        setField(player, "status", PlayerStatus.SIGNED);

        Field playersField = Team.class.getDeclaredField("players");
        playersField.setAccessible(true);
        ((List<Player>) playersField.get(team)).add(player);

        Field budgetField = Team.class.getDeclaredField("budget");
        budgetField.setAccessible(true);
        int budget = (int) budgetField.get(team);
        budgetField.set(team, budget - player.getSkillLevel() * 2);
    }

    private Match makeMatch(Team home, Team away) throws Exception {
        // Pre-computed plausible scoreline (1..3 per side) so the UI shows real numbers.
        int hg = 1 + (int) (Math.random() * 3);
        int ag = 1 + (int) (Math.random() * 3);
        MatchResult result = hg > ag ? MatchResult.HOME_WIN
                          : ag > hg ? MatchResult.AWAY_WIN
                          : MatchResult.DRAW;

        List<String> scorers = pickFakeScorers(home, hg);
        scorers.addAll(pickFakeScorers(away, ag));

        Match m = new Match();
        setField(m, "homeTeam", home);
        setField(m, "awayTeam", away);
        setField(m, "homeGoals", hg);
        setField(m, "awayGoals", ag);
        setField(m, "result", result);
        setField(m, "scorers", scorers);

        // Update both teams' Statistics so the league table makes sense.
        Statistics homeStats = home.getStatistics();
        Statistics awayStats = away.getStatistics();
        homeStats.addGoalsFor(hg);
        homeStats.addGoalsAgainst(ag);
        awayStats.addGoalsFor(ag);
        awayStats.addGoalsAgainst(hg);
        switch (result) {
            case HOME_WIN -> { homeStats.addWin();  awayStats.addLoss(); }
            case AWAY_WIN -> { awayStats.addWin();  homeStats.addLoss(); }
            case DRAW     -> { homeStats.addDraw(); awayStats.addDraw(); }
        }
        return m;
    }

    private List<String> pickFakeScorers(Team team, int goals) {
        List<String> names = new ArrayList<>();
        // Just pick the first FWD/MID we find as the scorer — the demo seeder
        // is not trying to model the FWD-twice/MID-once weighting; that's the
        // student's job in Match.distributeGoals.
        for (int i = 0; i < goals; i++) {
            names.add(team.getPlayers().stream()
                    .filter(p -> p.getPosition() == Position.FWD || p.getPosition() == Position.MID)
                    .findFirst()
                    .map(Player::getName)
                    .orElse("Unknown"));
        }
        return names;
    }

    // ------------------------------------------------------------------
    // Reflection utilities
    // ------------------------------------------------------------------

    private void setField(Object obj, String fieldName, Object value) throws Exception {
        Field f = obj.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(obj, value);
    }

    @SuppressWarnings("unchecked")
    private <T> void addToList(Object repo, String fieldName, T entity) throws Exception {
        Field f = repo.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        ((List<T>) f.get(repo)).add(entity);
    }

    private void clearList(Object repo, String fieldName) throws Exception {
        Field f = repo.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        ((List<?>) f.get(repo)).clear();
    }
}
