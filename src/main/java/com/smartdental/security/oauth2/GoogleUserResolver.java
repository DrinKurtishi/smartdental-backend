package com.smartdental.security.oauth2;

import com.smartdental.entity.User;
import com.smartdental.entity.enums.AuthProvider;
import com.smartdental.entity.enums.RoleName;
import com.smartdental.repository.UserRepository;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;

/**
 * Resolves a Google profile (from either the plain OAuth2 or OIDC userinfo flow, which return the
 * same attribute set) into a local {@link User}, provisioning a patient account on first login.
 */
final class GoogleUserResolver {

    private GoogleUserResolver() {}

    static User resolve(UserRepository userRepository, Map<String, Object> attributes) {
        String googleId = (String) attributes.get("sub");
        String email = (String) attributes.get("email");
        String givenName = (String) attributes.get("given_name");
        String familyName = (String) attributes.get("family_name");
        Boolean emailVerified = (Boolean) attributes.get("email_verified");

        if (email == null || googleId == null) {
            throw new OAuth2AuthenticationException("Google account did not return an email/subject id");
        }
        if (emailVerified != null && !emailVerified) {
            throw new OAuth2AuthenticationException("Google email is not verified");
        }

        User user =
                userRepository
                        .findByGoogleId(googleId)
                        .or(() -> userRepository.findByEmailIgnoreCase(email))
                        .orElseGet(() -> provisionNewGoogleUser(userRepository, email, googleId, givenName, familyName));

        if (user.getGoogleId() == null) {
            user.setGoogleId(googleId);
            user.setAuthProvider(AuthProvider.GOOGLE);
        }
        return userRepository.save(user);
    }

    private static User provisionNewGoogleUser(
            UserRepository userRepository, String email, String googleId, String givenName, String familyName) {
        User user = new User();
        user.setEmail(email);
        user.setGoogleId(googleId);
        user.setAuthProvider(AuthProvider.GOOGLE);
        user.setFirstName(givenName != null ? givenName : "Google");
        user.setLastName(familyName != null ? familyName : "User");
        user.setEnabled(true);
        // Hibernate's PersistentSet wraps this field and calls .clear() on it when the entity is
        // saved again later in resolve() — Set.of() is immutable and throws UnsupportedOperationException.
        user.setRoles(new HashSet<>(Set.of(RoleName.ROLE_PATIENT)));
        return userRepository.save(user);
    }
}
