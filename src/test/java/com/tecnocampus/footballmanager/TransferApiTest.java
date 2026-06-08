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
class TransferApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static String teamAId;
    private static String teamBId;
    private static String playerFwdId;
    private static String playerDefId;

    @BeforeAll
    static void setUp(@Autowired MockMvc mockMvc, @Autowired ObjectMapper objectMapper) throws Exception {
        // Create two teams
        MvcResult teamA = mockMvc.perform(post("/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("name", "Transfer Team A", "city", "CityA"))))
                .andReturn();
        teamAId = objectMapper.readTree(teamA.getResponse().getContentAsString()).get("id").asText();

        MvcResult teamB = mockMvc.perform(post("/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("name", "Transfer Team B", "city", "CityB"))))
                .andReturn();
        teamBId = objectMapper.readTree(teamB.getResponse().getContentAsString()).get("id").asText();

        // Create players
        MvcResult fwd = mockMvc.perform(post("/players")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("name", "Star Forward", "position", "FWD", "skillLevel", 80))))
                .andReturn();
        playerFwdId = objectMapper.readTree(fwd.getResponse().getContentAsString()).get("id").asText();

        MvcResult def = mockMvc.perform(post("/players")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("name", "Solid Defender", "position", "DEF", "skillLevel", 50))))
                .andReturn();
        playerDefId = objectMapper.readTree(def.getResponse().getContentAsString()).get("id").asText();
    }

    // --- 1. Sign a free agent ---
    @Test
    @Order(1)
    void signPlayer_shouldWork() throws Exception {
        // Cost = 80 * 2 = 160. Budget: 1000 → 840
        mockMvc.perform(post("/teams/" + teamAId + "/sign/" + playerFwdId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.player.status").value("SIGNED"))
                .andExpect(jsonPath("$.cost").value(160))
                .andExpect(jsonPath("$.teamBudgetRemaining").value(840));
    }

    // --- 2. Sign already signed player ---
    @Test
    @Order(2)
    void signPlayer_alreadySigned_shouldReturn400() throws Exception {
        mockMvc.perform(post("/teams/" + teamBId + "/sign/" + playerFwdId))
                .andExpect(status().isBadRequest());
    }

    // --- 3. Sign — insufficient budget ---
    @Test
    @Order(3)
    void signPlayer_insufficientBudget_shouldReturn400() throws Exception {
        // Create an expensive player (skill 99 → cost 198)
        // First fill up a team's budget by signing many expensive players...
        // Simpler: create a team with limited budget scenario
        // Create a player with skill 99 and try to sign to teamB which still has 1000
        // Actually skill 99 * 2 = 198 is affordable. Let's create many to drain budget.

        // Let's create 5 players with skill 99 and sign them all to teamB
        // 5 × 198 = 990. Budget goes from 1000 → 10
        for (int i = 0; i < 5; i++) {
            MvcResult p = mockMvc.perform(post("/players")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    Map.of("name", "Expensive " + i, "position", "MID", "skillLevel", 99))))
                    .andReturn();
            String pid = objectMapper.readTree(p.getResponse().getContentAsString()).get("id").asText();
            mockMvc.perform(post("/teams/" + teamBId + "/sign/" + pid));
        }

        // Now teamB has 1000 - 990 = 10 budget. Try to sign a player with skill 50 (cost 100)
        mockMvc.perform(post("/teams/" + teamBId + "/sign/" + playerDefId))
                .andExpect(status().isBadRequest());
    }

    // --- 4. Sign — team full ---
    @Test
    @Order(4)
    void signPlayer_teamFull_shouldReturn400() throws Exception {
        // Create a new team and fill it to 11 players
        MvcResult fullTeam = mockMvc.perform(post("/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("name", "Full Squad FC", "city", "FullCity"))))
                .andReturn();
        String fullTeamId = objectMapper.readTree(fullTeam.getResponse().getContentAsString()).get("id").asText();

        // Sign 11 cheap players (skill 1, cost 2 each → total 22, well within 1000 budget)
        for (int i = 0; i < 11; i++) {
            MvcResult p = mockMvc.perform(post("/players")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    Map.of("name", "Filler " + i, "position", "DEF", "skillLevel", 1))))
                    .andReturn();
            String pid = objectMapper.readTree(p.getResponse().getContentAsString()).get("id").asText();
            mockMvc.perform(post("/teams/" + fullTeamId + "/sign/" + pid))
                    .andExpect(status().isOk());
        }

        // 12th player should fail
        MvcResult extra = mockMvc.perform(post("/players")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("name", "Extra Player", "position", "GK", "skillLevel", 1))))
                .andReturn();
        String extraId = objectMapper.readTree(extra.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(post("/teams/" + fullTeamId + "/sign/" + extraId))
                .andExpect(status().isBadRequest());
    }

    // --- 5. Release a player ---
    @Test
    @Order(5)
    void releasePlayer_shouldWork() throws Exception {
        // Sign playerDef to teamA first
        mockMvc.perform(post("/teams/" + teamAId + "/sign/" + playerDefId))
                .andExpect(status().isOk());

        // Release: refund = 50 * 1 = 50
        mockMvc.perform(post("/teams/" + teamAId + "/release/" + playerDefId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.player.status").value("FREE_AGENT"))
                .andExpect(jsonPath("$.refund").value(50));
    }

    // --- 6. Release — player not on team ---
    @Test
    @Order(6)
    void releasePlayer_wrongTeam_shouldReturn400() throws Exception {
        // playerDef is now a free agent — try releasing from teamA
        mockMvc.perform(post("/teams/" + teamAId + "/release/" + playerDefId))
                .andExpect(status().isBadRequest());
    }

    // --- 7. Transfer between teams ---
    @Test
    @Order(7)
    void transferPlayer_shouldWork() throws Exception {
        // playerFwd is on teamA. Transfer to teamB.
        // Transfer fee = 80 * 3 = 240
        // Note: teamB budget is low from previous test. Let's create a new team for this.
        MvcResult newTeam = mockMvc.perform(post("/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("name", "Rich FC", "city", "RichCity"))))
                .andReturn();
        String richTeamId = objectMapper.readTree(newTeam.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(post("/teams/" + teamAId + "/transfer/" + playerFwdId + "/to/" + richTeamId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transferFee").value(240));
    }

    // --- 8. Transfer — same team ---
    @Test
    @Order(8)
    void transferPlayer_sameTeam_shouldReturn400() throws Exception {
        mockMvc.perform(post("/teams/" + teamAId + "/transfer/" + playerFwdId + "/to/" + teamAId))
                .andExpect(status().isBadRequest());
    }

    // --- 9. Transfer — player not on team ---
    @Test
    @Order(9)
    void transferPlayer_notOnTeam_shouldReturn400() throws Exception {
        mockMvc.perform(post("/teams/" + teamAId + "/transfer/" + playerDefId + "/to/" + teamBId))
                .andExpect(status().isBadRequest());
    }
}
