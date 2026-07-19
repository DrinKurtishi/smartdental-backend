package com.smartdental.security.oauth2;

import com.smartdental.entity.User;
import com.smartdental.security.UserPrincipal;
import java.util.Map;
import lombok.Getter;
import org.springframework.security.oauth2.core.user.OAuth2User;

@Getter
public class OAuth2UserPrincipal extends UserPrincipal implements OAuth2User {

    private final Map<String, Object> attributes;

    public OAuth2UserPrincipal(User user, Map<String, Object> attributes) {
        super(user);
        this.attributes = attributes;
    }

    @Override
    public String getName() {
        return getId().toString();
    }
}
