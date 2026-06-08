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
class TeamApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static String teamId;

    // --- 1. Create team ---
    @Test
    @Order(1)
    void createTeam_shouldReturn201() throws Exception {
        MvcResult result = mockMvc.perform(post("/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("name", "FC Tecnocampus", "city", "Mataró"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("FC Tecnocampus"))
                .andExpect(jsonPath("$.city").value("Mataró"))
                .andExpect(jsonPath("$.budget").value(1000))
                .andExpect(jsonPath("$.wins").value(0))
                .andExpect(jsonPath("$.draws").value(0))
                .andExpect(jsonPath("$.losses").value(0))
                .andExpect(jsonPath("$.points").value(0))
                .andExpect(jsonPath("$.goalsFor").value(0))
                .andExpect(jsonPath("$.goalsAgainst").value(0))
                .andExpect(jsonPath("$.goalDifference").value(0))
                .andExpect(jsonPath("$.teamPower").value(0))
                .andExpect(jsonPath("$.players").isArray())
                .andExpect(jsonPath("$.players").isEmpty())
                .andReturn();

        teamId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    // --- 2. Duplicate team name ---
    @Test
    @Order(2)
    void createTeam_duplicateName_shouldReturn409() throws Exception {
        mockMvc.perform(post("/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("name", "FC Tecnocampus", "city", "Barcelona"))))
                .andExpect(status().isConflict());
    }

    // --- 3. Validation errors ---
    @Test
    @Order(3)
    void createTeam_invalidName_shouldReturn400() throws Exception {
        mockMvc.perform(post("/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("name", "FC", "city", "Barcelona"))))
                .andExpect(status().isBadRequest());
    }

    // --- 4. Get team by ID (detail with players) ---
    @Test
    @Order(4)
    void getTeam_shouldReturnDetail() throws Exception {
        mockMvc.perform(get("/teams/" + teamId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("FC Tecnocampus"))
                .andExpect(jsonPath("$.players").isArray());
    }

    // --- 5. Get team not found ---
    @Test
    @Order(5)
    void getTeam_notFound_shouldReturn404() throws Exception {
        mockMvc.perform(get("/teams/00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isNotFound());
    }

    // --- 6. Get all teams ---
    @Test
    @Order(6)
    void getAllTeams_shouldReturnList() throws Exception {
        mockMvc.perform(get("/teams"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    // --- 7. Delete team releases players ---
    @Test
    @Order(7)
    void deleteTeam_shouldReturn204AndReleasePlayers() throws Exception {
        // Create a team and a player, sign the player, then delete the team
        MvcResult teamResult = mockMvc.perform(post("/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("name", "Temp Team", "city", "Temp City"))))
                .andExpect(status().isCreated())
                .andReturn();
        String tempTeamId = objectMapper.readTree(teamResult.getResponse().getContentAsString()).get("id").asText();

        MvcResult playerResult = mockMvc.perform(post("/players")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("name", "Temp Player", "position", "DEF", "skillLevel", 40))))
                .andExpect(status().isCreated())
                .andReturn();
        String tempPlayerId = objectMapper.readTree(playerResult.getResponse().getContentAsString()).get("id").asText();

        // Sign the player
        mockMvc.perform(post("/teams/" + tempTeamId + "/sign/" + tempPlayerId))
                .andExpect(status().isOk());

        // Delete the team
        mockMvc.perform(delete("/teams/" + tempTeamId))
                .andExpect(status().isNoContent());

        // Verify the player is now FREE_AGENT
        mockMvc.perform(get("/players/" + tempPlayerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FREE_AGENT"));
    }

    // --- 8. Validation: create with empty city ---
    @Test
    @Order(8)
    void createTeam_emptyCity_shouldReturn400() throws Exception {
        mockMvc.perform(post("/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("name", "Valid Name", "city", ""))))
                .andExpect(status().isBadRequest());
    }
}
