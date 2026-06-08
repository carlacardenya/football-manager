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
class PlayerApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static String playerId;

    // --- 1. Create player ---
    @Test
    @Order(1)
    void createPlayer_shouldReturn201() throws Exception {
        MvcResult result = mockMvc.perform(post("/players")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("name", "Lamine Yamal", "position", "FWD", "skillLevel", 88))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Lamine Yamal"))
                .andExpect(jsonPath("$.position").value("FWD"))
                .andExpect(jsonPath("$.skillLevel").value(88))
                .andExpect(jsonPath("$.status").value("FREE_AGENT"))
                .andExpect(jsonPath("$.goals").value(0))
                .andExpect(jsonPath("$.gamesPlayed").value(0))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        playerId = objectMapper.readTree(body).get("id").asText();
    }

    // --- 2. Create player — validation errors ---
    @Test
    @Order(2)
    void createPlayer_invalidName_shouldReturn400() throws Exception {
        mockMvc.perform(post("/players")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("name", "A", "position", "FWD", "skillLevel", 50))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(3)
    void createPlayer_invalidSkillLevel_shouldReturn400() throws Exception {
        mockMvc.perform(post("/players")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("name", "Test Player", "position", "MID", "skillLevel", 100))))
                .andExpect(status().isBadRequest());
    }

    // --- 3. Get player by ID ---
    @Test
    @Order(4)
    void getPlayer_shouldReturn200() throws Exception {
        mockMvc.perform(get("/players/" + playerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Lamine Yamal"));
    }

    @Test
    @Order(5)
    void getPlayer_notFound_shouldReturn404() throws Exception {
        mockMvc.perform(get("/players/00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isNotFound());
    }

    // --- 4. Get all players ---
    @Test
    @Order(6)
    void getAllPlayers_shouldReturnList() throws Exception {
        mockMvc.perform(get("/players"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    // --- 5. Filter players by position ---
    @Test
    @Order(7)
    void getPlayers_filterByPosition_shouldWork() throws Exception {
        mockMvc.perform(get("/players?position=FWD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].position", everyItem(is("FWD"))));
    }

    // --- 6. Filter players by status ---
    @Test
    @Order(8)
    void getPlayers_filterByStatus_shouldWork() throws Exception {
        mockMvc.perform(get("/players?status=FREE_AGENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].status", everyItem(is("FREE_AGENT"))));
    }

    // --- 7. Update player ---
    @Test
    @Order(9)
    void updatePlayer_shouldReturn200() throws Exception {
        mockMvc.perform(put("/players/" + playerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("name", "Lamine Yamal Jr.", "skillLevel", 92))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Lamine Yamal Jr."))
                .andExpect(jsonPath("$.skillLevel").value(92))
                .andExpect(jsonPath("$.position").value("FWD")); // position unchanged
    }

    // --- 8. Delete player ---
    @Test
    @Order(10)
    void deletePlayer_shouldReturn204() throws Exception {
        // Create a player to delete
        MvcResult result = mockMvc.perform(post("/players")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("name", "To Delete", "position", "GK", "skillLevel", 30))))
                .andExpect(status().isCreated())
                .andReturn();

        String id = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(delete("/players/" + id))
                .andExpect(status().isNoContent());

        // Verify deleted
        mockMvc.perform(get("/players/" + id))
                .andExpect(status().isNotFound());
    }

    // --- 9. Validation: create with invalid position ---
    @Test
    @Order(11)
    void createPlayer_invalidPosition_shouldReturn400() throws Exception {
        mockMvc.perform(post("/players")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("name", "Some Name", "position", "INVALID", "skillLevel", 50))))
                .andExpect(status().isBadRequest());
    }

    // --- 10. Validation: create with skillLevel below minimum ---
    @Test
    @Order(12)
    void createPlayer_skillLevelTooLow_shouldReturn400() throws Exception {
        mockMvc.perform(post("/players")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("name", "Some Name", "position", "MID", "skillLevel", 0))))
                .andExpect(status().isBadRequest());
    }

    // --- 11. Validation: update with invalid name ---
    @Test
    @Order(13)
    void updatePlayer_invalidName_shouldReturn400() throws Exception {
        mockMvc.perform(put("/players/" + playerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("name", "X", "skillLevel", 70))))
                .andExpect(status().isBadRequest());
    }

    // --- 12. Validation: update with invalid skillLevel (too low) ---
    @Test
    @Order(14)
    void updatePlayer_skillLevelTooLow_shouldReturn400() throws Exception {
        mockMvc.perform(put("/players/" + playerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("name", "Valid Name", "skillLevel", 0))))
                .andExpect(status().isBadRequest());
    }

    // --- 13. Validation: update with invalid skillLevel (too high) ---
    @Test
    @Order(15)
    void updatePlayer_skillLevelTooHigh_shouldReturn400() throws Exception {
        mockMvc.perform(put("/players/" + playerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("name", "Valid Name", "skillLevel", 100))))
                .andExpect(status().isBadRequest());
    }
}
