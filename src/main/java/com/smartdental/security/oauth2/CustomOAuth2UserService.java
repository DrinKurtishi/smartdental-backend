package com.smartdental.security.oauth2;

import com.smartdental.entity.User;
import com.smartdental.entity.enums.AuthProvider;
import com.smartdental.entity.enums.RoleName;
import com.smartdental.repository.UserRepository;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Resolves the Google profile into a local {@link User}, creating a patient account on first login. */
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String googleId = oAuth2User.getAttribute("sub");
        String email = oAuth2User.getAttribute("email");
        String givenName = oAuth2User.getAttribute("given_name");
        String familyName = oAuth2User.getAttribute("family_name");
        Boolean emailVerified = oAuth2User.getAttribute("email_verified");

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
                        .orElseGet(() -> provisionNewGoogleUser(email, googleId, givenName, familyName));

        if (user.getGoogleId() == null) {
            user.setGoogleId(googleId);
            user.setAuthProvider(AuthProvider.GOOGLE);
        }
        userRepository.save(user);

        return new OAuth2UserPrincipal(user, oAuth2User.getAttributes());
    }

    private User provisionNewGoogleUser(String email, String googleId, String givenName, String familyName) {
        User user = new User();
        user.setEmail(email);
        user.setGoogleId(googleId);
        user.setAuthProvider(AuthProvider.GOOGLE);
        user.setFirstName(givenName != null ? givenName : "Google");
        user.setLastName(familyName != null ? familyName : "User");
        user.setEnabled(true);
        user.setRoles(Set.of(RoleName.ROLE_PATIENT));
        return userRepository.save(user);
    }
}
