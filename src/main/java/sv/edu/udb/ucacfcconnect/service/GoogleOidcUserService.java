package sv.edu.udb.ucacfcconnect.service;

import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

@Service
public class GoogleOidcUserService implements OAuth2UserService<OidcUserRequest, OidcUser> {
    private final OidcUserService delegate = new OidcUserService();
    private final AuthService authService;

    public GoogleOidcUserService(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = delegate.loadUser(userRequest);

        if ("google".equals(userRequest.getClientRegistration().getRegistrationId())) {
            authService.procesarUsuarioGoogle(
                    oidcUser.getSubject(),
                    oidcUser.getEmail(),
                    oidcUser.getFullName(),
                    Boolean.TRUE.equals(oidcUser.getEmailVerified())
            );
        }

        return oidcUser;
    }
}
