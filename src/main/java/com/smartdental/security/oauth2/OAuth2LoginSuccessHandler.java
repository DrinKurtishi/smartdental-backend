package com.smartdental.security.oauth2;

import com.smartdental.config.FrontendProperties;
import com.smartdental.repository.UserRepository;
import com.smartdental.security.jwt.JwtService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

/** Issues a SmartDental JWT after a successful Google login and redirects back to the SPA. */
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final FrontendProperties frontendProperties;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {

        OAuth2UserPrincipal principal = (OAuth2UserPrincipal) authentication.getPrincipal();
        var user =
                userRepository
                        .findById(principal.getId())
                        .orElseThrow(() -> new IllegalStateException("Authenticated user vanished"));

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        String redirectUrl =
                frontendProperties.baseUrl()
                        + "/oauth2/redirect?token="
                        + URLEncoder.encode(accessToken, StandardCharsets.UTF_8)
                        + "&refreshToken="
                        + URLEncoder.encode(refreshToken, StandardCharsets.UTF_8);

        clearAuthenticationAttributes(request);
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}
