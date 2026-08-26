package com.smartdental.service;

import com.smartdental.dto.auth.AuthResponse;
import com.smartdental.dto.auth.LoginRequest;
import com.smartdental.dto.auth.RegisterRequest;
import com.smartdental.dto.auth.UserSummaryResponse;
import com.smartdental.entity.User;
import com.smartdental.entity.enums.AuthProvider;
import com.smartdental.entity.enums.RoleName;
import com.smartdental.exception.ConflictException;
import com.smartdental.repository.UserRepository;
import com.smartdental.security.UserPrincipal;
import com.smartdental.security.jwt.JwtService;
import java.util.HashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ConflictException("An account with this email already exists");
        }

        User user = new User();
        user.setEmail(request.email().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setPhone(request.phone());
        user.setAuthProvider(AuthProvider.LOCAL);
        user.setRoles(new HashSet<>(Set.of(RoleName.ROLE_PATIENT)));
        user.setEnabled(true);

        User saved = userRepository.save(user);
        return issueTokens(saved);
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email().toLowerCase(), request.password()));

        User user =
                userRepository
                        .findByEmailIgnoreCase(request.email())
                        .orElseThrow(() -> new IllegalStateException("User vanished after authentication"));
        return issueTokens(user);
    }

    public UserSummaryResponse currentUser(UserPrincipal principal) {
        User user =
                userRepository
                        .findById(principal.getId())
                        .orElseThrow(() -> new IllegalStateException("Authenticated user no longer exists"));
        return UserSummaryResponse.from(user);
    }

    private AuthResponse issueTokens(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        return AuthResponse.of(accessToken, refreshToken, UserSummaryResponse.from(user));
    }
}
