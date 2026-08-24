package com.smartdental.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartdental.entity.User;
import com.smartdental.entity.enums.AuthProvider;
import com.smartdental.entity.enums.RoleName;
import com.smartdental.repository.UserRepository;
import com.smartdental.security.jwt.JwtService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Full HTTP round-trip for booking → confirming → cancelling an appointment, with SES disabled
 * (as it is in the "test" profile). This is the exact condition that previously triggered a
 * LazyInitializationException when the controller mapped the response after the transaction closed.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AppointmentLifecycleTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;

    private String patientToken;
    private String dentistToken;

    @BeforeEach
    void setUp() {
        User patient = persistUser("lifecycle.patient@example.com", RoleName.ROLE_PATIENT);
        User dentist = persistUser("lifecycle.dentist@smartdental.example.com", RoleName.ROLE_DENTIST);
        patientToken = jwtService.generateAccessToken(patient);
        dentistToken = jwtService.generateAccessToken(dentist);
    }

    @Test
    void patientCanBookAndDentistCanConfirmThenCancel() throws Exception {
        Instant start = Instant.now().plus(2, ChronoUnit.DAYS);
        Instant end = start.plus(30, ChronoUnit.MINUTES);
        User dentist =
                userRepository.findByEmailIgnoreCase("lifecycle.dentist@smartdental.example.com").orElseThrow();

        String createBody =
                objectMapper.writeValueAsString(
                        Map.of(
                                "dentistId", dentist.getId().toString(),
                                "startTime", start.toString(),
                                "endTime", end.toString(),
                                "reason", "Lifecycle test checkup"));

        String createResponse =
                mockMvc
                        .perform(
                                post("/api/v1/appointments")
                                        .header("Authorization", "Bearer " + patientToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(createBody))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.status").value("PENDING"))
                        .andExpect(jsonPath("$.dentistName").isNotEmpty())
                        .andExpect(jsonPath("$.patientName").isNotEmpty())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        String appointmentId = objectMapper.readTree(createResponse).get("id").asText();

        mockMvc
                .perform(
                        patch("/api/v1/appointments/" + appointmentId + "/status")
                                .header("Authorization", "Bearer " + dentistToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"status\":\"CONFIRMED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.dentistName").isNotEmpty())
                .andExpect(jsonPath("$.patientName").isNotEmpty());

        mockMvc
                .perform(delete("/api/v1/appointments/" + appointmentId).header("Authorization", "Bearer " + patientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
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
