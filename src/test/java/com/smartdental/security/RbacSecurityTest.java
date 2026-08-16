package com.smartdental.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** Integration tests proving RBAC actually blocks non-admin roles from admin-only endpoints. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RbacSecurityTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;

    private String patientToken;
    private String dentistToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        patientToken = jwtService.generateAccessToken(persistUser("rbac.patient@example.com", RoleName.ROLE_PATIENT));
        dentistToken =
                jwtService.generateAccessToken(persistUser("rbac.dentist@smartdental.example.com", RoleName.ROLE_DENTIST));
        adminToken = jwtService.generateAccessToken(persistUser("rbac.admin@smartdental.example.com", RoleName.ROLE_ADMIN));
    }

    @Test
    void patientCannotListAdminUsers() throws Exception {
        mockMvc
                .perform(get("/api/v1/admin/users").header("Authorization", "Bearer " + patientToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void patientCannotCreateStaffUsers() throws Exception {
        String body =
                """
                {"email":"new.staff@smartdental.example.com","password":"Password123!","firstName":"New",
                 "lastName":"Staff","roles":["ROLE_HYGIENIST"]}
                """;
        mockMvc
                .perform(
                        post("/api/v1/admin/users")
                                .header("Authorization", "Bearer " + patientToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void dentistCannotAccessAdminAuditLogs() throws Exception {
        mockMvc
                .perform(get("/api/v1/admin/audit-logs").header("Authorization", "Bearer " + dentistToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanListUsers() throws Exception {
        mockMvc
                .perform(get("/api/v1/admin/users").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void unauthenticatedRequestIsRejectedNotForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")).andExpect(status().isUnauthorized());
    }

    @Test
    void patientCannotAccessOtherPatientsClinicalNotes() throws Exception {
        mockMvc
                .perform(
                        get("/api/v1/clinical-notes/patient/" + java.util.UUID.randomUUID())
                                .header("Authorization", "Bearer " + patientToken))
                .andExpect(status().isForbidden());
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
