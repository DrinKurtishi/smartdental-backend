package com.smartdental.security.oauth2;

import com.smartdental.entity.User;
import com.smartdental.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resolves the Google profile into a local {@link User}, creating a patient account on first
 * login. Google's client registration requests the "openid" scope, so Spring Security treats it
 * as an OIDC provider and calls this service (not {@link CustomOAuth2UserService}) — it must be
 * wired via {@code userInfoEndpoint().oidcUserService(...)}, not {@code .userService(...)}.
 */
@Service
@RequiredArgsConstructor
public class CustomOidcUserService extends OidcUserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);
        User user = GoogleUserResolver.resolve(userRepository, oidcUser.getAttributes());
        return new OidcUserPrincipal(user, oidcUser);
    }
}
