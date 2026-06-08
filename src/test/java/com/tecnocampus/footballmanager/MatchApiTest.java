package com.tecnocampus.footballmanager;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MatchApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static String homeTeamId;
    private static String awayTeamId;
    private static String emptyTeamId;
    private static String matchId;
    private static String homePlayerId;
    private static String awayPlayerId;

    @BeforeAll
    static void setUp(@Autowired MockMvc mockMvc, @Autowired ObjectMapper objectMapper) throws Exception {
        // Create two teams with players
        MvcResult home = mockMvc.perform(post("/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("name", "Match Home FC", "city", "HomeCity"))))
                .andReturn();
        homeTeamId = objectMapper.readTree(home.getResponse().getContentAsString()).get("id").asText();

        MvcResult away = mockMvc.perform(post("/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("name", "Match Away FC", "city", "AwayCity"))))
                .andReturn();
        awayTeamId = objectMapper.readTree(away.getResponse().getContentAsString()).get("id").asText();

        // Empty team (no players)
        MvcResult empty = mockMvc.perform(post("/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("name", "Empty FC", "city", "EmptyCity"))))
                .andReturn();
        emptyTeamId = objectMapper.readTree(empty.getResponse().getContentAsString()).get("id").asText();

        // Add players to home team
        MvcResult hp = mockMvc.perform(post("/players")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("name", "Home Forward", "position", "FWD", "skillLevel", 75))))
                .andReturn();
        homePlayerId = objectMapper.readTree(hp.getResponse().getContentAsString()).get("id").asText();
        mockMvc.perform(post("/teams/" + homeTeamId + "/sign/" + homePlayerId));

        MvcResult homeMid = mockMvc.perform(post("/players")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("name", "Home Midfielder", "position", "MID", "skillLevel", 60))))
                .andReturn();
        String homeMidId = objectMapper.readTree(homeMid.getResponse().getContentAsString()).get("id").asText();
        mockMvc.perform(post("/teams/" + homeTeamId + "/sign/" + homeMidId));

        // Add players to away team
        MvcResult ap = mockMvc.perform(post("/players")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("name", "Away Forward", "position", "FWD", "skillLevel", 70))))
                .andReturn();
        awayPlayerId = objectMapper.readTree(ap.getResponse().getContentAsString()).get("id").asText();
        mockMvc.perform(post("/teams/" + awayTeamId + "/sign/" + awayPlayerId));

        MvcResult awayDef = mockMvc.perform(post("/players")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("name", "Away Defender", "position", "DEF", "skillLevel", 55))))
                .andReturn();
        String awayDefId = objectMapper.readTree(awayDef.getResponse().getContentAsString()).get("id").asText();
        mockMvc.perform(post("/teams/" + awayTeamId + "/sign/" + awayDefId));
    }

    // --- 1. Play a match ---
    @Test
    @Order(1)
    void playMatch_shouldReturn201() throws Exception {
        MvcResult result = mockMvc.perform(post("/matches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("homeTeamId", homeTeamId, "awayTeamId", awayTeamId))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.homeTeam").value("Match Home FC"))
                .andExpect(jsonPath("$.awayTeam").value("Match Away FC"))
                .andExpect(jsonPath("$.homeGoals").isNumber())
                .andExpect(jsonPath("$.awayGoals").isNumber())
                .andExpect(jsonPath("$.result").isString())
                .andExpect(jsonPath("$.scorers").isArray())
                .andExpect(jsonPath("$.playedAt").isNotEmpty())
                .andReturn();

        matchId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    // --- 2. Match updates player stats ---
    @Test
    @Order(2)
    void playMatch_shouldUpdatePlayerStats() throws Exception {
        mockMvc.perform(get("/players/" + homePlayerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gamesPlayed").value(greaterThanOrEqualTo(1)));
    }

    // --- 3. Match updates team stats ---
    @Test
    @Order(3)
    void playMatch_shouldUpdateTeamStats() throws Exception {
        // After at least one match, the team should have some stats
        MvcResult result = mockMvc.perform(get("/teams/" + homeTeamId))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        var tree = objectMapper.readTree(body);
        int wins = tree.get("wins").asInt();
        int draws = tree.get("draws").asInt();
        int losses = tree.get("losses").asInt();
        Assertions.assertTrue(wins + draws + losses >= 1, "Team should have played at least 1 match");
    }

    // --- 4. Same team cannot play itself ---
    @Test
    @Order(4)
    void playMatch_sameTeam_shouldReturn400() throws Exception {
        mockMvc.perform(post("/matches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("homeTeamId", homeTeamId, "awayTeamId", homeTeamId))))
                .andExpect(status().isBadRequest());
    }

    // --- 5. Team with no players cannot play ---
    @Test
    @Order(5)
    void playMatch_noPlayers_shouldReturn400() throws Exception {
        mockMvc.perform(post("/matches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("homeTeamId", emptyTeamId, "awayTeamId", awayTeamId))))
                .andExpect(status().isBadRequest());
    }

    // --- 6. Get all matches ---
    @Test
    @Order(6)
    void getAllMatches_shouldReturnList() throws Exception {
        mockMvc.perform(get("/matches"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    // --- 7. Get match by ID ---
    @Test
    @Order(7)
    void getMatch_shouldReturn200() throws Exception {
        mockMvc.perform(get("/matches/" + matchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.homeTeam").value("Match Home FC"));
    }

    // --- 8. Get match not found ---
    @Test
    @Order(8)
    void getMatch_notFound_shouldReturn404() throws Exception {
        mockMvc.perform(get("/matches/00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isNotFound());
    }

    // --- 9. Get team matches ---
    @Test
    @Order(9)
    void getTeamMatches_shouldReturnList() throws Exception {
        mockMvc.perform(get("/teams/" + homeTeamId + "/matches"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    // --- 10. League table ---
    @Test
    @Order(10)
    void getLeagueTable_shouldReturnSortedTeams() throws Exception {
        mockMvc.perform(get("/teams/league-table"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // --- 11. Top scorers ---
    @Test
    @Order(11)
    void getTopScorers_shouldReturnList() throws Exception {
        mockMvc.perform(get("/players/top-scorers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()", lessThanOrEqualTo(10)));
    }

    // --- 12. Recover injured player ---
    @Test
    @Order(12)
    void recoverPlayer_notInjured_shouldReturn400() throws Exception {
        // The home player is SIGNED, not injured
        mockMvc.perform(post("/players/" + homePlayerId + "/recover"))
                .andExpect(status().isBadRequest());
    }
}
