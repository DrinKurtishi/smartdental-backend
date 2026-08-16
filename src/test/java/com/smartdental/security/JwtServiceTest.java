package com.smartdental.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.smartdental.config.JwtProperties;
import com.smartdental.entity.User;
import com.smartdental.entity.enums.RoleName;
import com.smartdental.security.jwt.JwtService;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private JwtService jwtService;
    private User user;

    @BeforeEach
    void setUp() {
        JwtProperties properties =
                new JwtProperties(
                        "unit-test-secret-key-must-be-long-enough-for-hs256-signing-0123456789",
                        3_600_000,
                        604_800_000,
                        "smartdental-test");
        jwtService = new JwtService(properties);

        user = new User();
        user.setEmail("dr.rivera@smartdental.example.com");
        user.setFirstName("Elena");
        user.setLastName("Rivera");
        user.setRoles(Set.of(RoleName.ROLE_DENTIST));
        setId(user, UUID.randomUUID());
    }

    @Test
    void generatedAccessTokenIsValidAndCarriesClaims() {
        String token = jwtService.generateAccessToken(user);

        assertThat(jwtService.isTokenValid(token)).isTrue();
        assertThat(jwtService.extractEmail(token)).isEqualTo(user.getEmail());
        assertThat(jwtService.extractUserId(token)).isEqualTo(user.getId());
    }

    @Test
    void tamperedTokenIsRejected() {
        String token = jwtService.generateAccessToken(user);
        String tampered = token.substring(0, token.length() - 2) + "xx";

        assertThat(jwtService.isTokenValid(tampered)).isFalse();
    }

    @Test
    void garbageTokenFailsValidationRatherThanThrowing() {
        assertThat(jwtService.isTokenValid("not-a-jwt")).isFalse();
        assertThatThrownBy(() -> jwtService.extractEmail("not-a-jwt")).isInstanceOf(RuntimeException.class);
    }

    private static void setId(User user, UUID id) {
        try {
            var field = com.smartdental.entity.BaseEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
