package com.smartdental.security.oauth2;

import com.smartdental.entity.User;
import com.smartdental.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles the plain (non-OIDC) OAuth2 login path. Google's registration requests the "openid"
 * scope, so real Google logins are actually routed through {@link CustomOidcUserService} instead
 * — this stays wired only in case a future provider without OIDC support is added.
 */
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        User user = GoogleUserResolver.resolve(userRepository, oAuth2User.getAttributes());
        return new OAuth2UserPrincipal(user, oAuth2User.getAttributes());
    }
}
