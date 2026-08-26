package com.smartdental.security.oauth2;

import com.smartdental.entity.User;
import java.util.Map;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

/**
 * {@link OAuth2UserPrincipal} extended with the OIDC-specific claims Spring Security expects on
 * the principal for an OIDC login, so callers that only need the plain {@code OAuth2User} view
 * (e.g. {@link OAuth2LoginSuccessHandler}'s cast to {@code OAuth2UserPrincipal}) keep working.
 */
public class OidcUserPrincipal extends OAuth2UserPrincipal implements OidcUser {

    private final OidcUser oidcUser;

    public OidcUserPrincipal(User user, OidcUser oidcUser) {
        super(user, oidcUser.getAttributes());
        this.oidcUser = oidcUser;
    }

    @Override
    public Map<String, Object> getClaims() {
        return oidcUser.getClaims();
    }

    @Override
    public OidcUserInfo getUserInfo() {
        return oidcUser.getUserInfo();
    }

    @Override
    public OidcIdToken getIdToken() {
        return oidcUser.getIdToken();
    }
}
