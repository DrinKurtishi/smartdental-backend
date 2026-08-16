package com.smartdental.controller;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartdental.entity.User;
import com.smartdental.entity.enums.AuthProvider;
import com.smartdental.entity.enums.RoleName;
import com.smartdental.repository.UserRepository;
import com.smartdental.security.jwt.JwtService;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Verifies the /api/v1/translate endpoint: role enforcement, request validation, and the
 * rule-based fallback summary that is used whenever no AI provider is configured or reachable.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "smartdental.ai.translation.enabled=false")
@Transactional
class TranslateControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;

    private String dentistToken;
    private String patientToken;

    @BeforeEach
    void setUp() {
        User dentist = persistUser("dentist@smartdental.example.com", RoleName.ROLE_DENTIST);
        User patient = persistUser("patient@example.com", RoleName.ROLE_PATIENT);
        dentistToken = jwtService.generateAccessToken(dentist);
        patientToken = jwtService.generateAccessToken(patient);
    }

    @Test
    void fallbackSummaryExtractsPlainEnglishTermsWhenAiIsDisabled() throws Exception {
        String body = objectMapper.writeValueAsString(
                java.util.Map.of("shorthand", "Tooth #14 DO composite, deep caries"));

        mockMvc
                .perform(
                        post("/api/v1/translate")
                                .header("Authorization", "Bearer " + dentistToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aiGenerated").value(false))
                .andExpect(jsonPath("$.plainEnglishSummary").value(containsString("tooth-colored filling")))
                .andExpect(jsonPath("$.plainEnglishSummary").value(containsString("cavity")))
                .andExpect(jsonPath("$.plainEnglishSummary").value(containsString("tooth #14")));
    }

    @Test
    void blankShorthandIsRejected() throws Exception {
        String body = objectMapper.writeValueAsString(java.util.Map.of("shorthand", "   "));

        mockMvc
                .perform(
                        post("/api/v1/translate")
                                .header("Authorization", "Bearer " + dentistToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void patientRoleCannotCallTranslate() throws Exception {
        String body = objectMapper.writeValueAsString(java.util.Map.of("shorthand", "Tooth #30 MOD amalgam failing"));

        mockMvc
                .perform(
                        post("/api/v1/translate")
                                .header("Authorization", "Bearer " + patientToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        String body = objectMapper.writeValueAsString(java.util.Map.of("shorthand", "Tooth #30 MOD amalgam failing"));

        mockMvc
                .perform(post("/api/v1/translate").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
    }

    private User persistUser(String email, RoleName role) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode("Password123!"));
        user.setFirstName("Test");
        user.setLastName("User");
        user.setAuthProvider(AuthProvider.LOCAL);
        user.setEnabled(true);
        user.setRoles(Set.of(role));
        return userRepository.save(user);
    }
}
