package com.smartdental.dto.auth;

public record AuthResponse(String accessToken, String refreshToken, String tokenType, UserSummaryResponse user) {

    public static AuthResponse of(String accessToken, String refreshToken, UserSummaryResponse user) {
        return new AuthResponse(accessToken, refreshToken, "Bearer", user);
    }
}
