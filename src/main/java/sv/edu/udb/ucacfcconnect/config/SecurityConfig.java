package sv.edu.udb.ucacfcconnect.config;

import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import sv.edu.udb.ucacfcconnect.dto.auth.ErrorResponse;
import sv.edu.udb.ucacfcconnect.security.OAuth2AuthenticationFailureHandler;
import sv.edu.udb.ucacfcconnect.security.OAuth2AuthenticationSuccessHandler;
import sv.edu.udb.ucacfcconnect.service.GoogleOidcUserService;
import sv.edu.udb.ucacfcconnect.security.AuthCookieService;
import sv.edu.udb.ucacfcconnect.security.ActiveAccountFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            ObjectMapper objectMapper,
            ObjectProvider<ClientRegistrationRepository> clientRegistrations,
            GoogleOidcUserService googleOidcUserService,
            OAuth2AuthenticationSuccessHandler successHandler,
            OAuth2AuthenticationFailureHandler failureHandler,
            BearerTokenResolver bearerTokenResolver,
            ActiveAccountFilter activeAccountFilter
    ) throws Exception {
        http
                .csrf(Customizer.withDefaults())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/auth/registro",
                                "/api/v1/auth/login",
                                "/api/v1/auth/logout").permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/auth/google",
                                "/api/v1/auth/csrf",
                                "/api/v1/cursos/**").permitAll()
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET,
                                "/",
                                "/index.html",
                                "/login.html",
                                "/registro.html",
                                "/auth-callback.html",
                                "/perfil.html",
                                "/cliente.html",
                                "/aprendizaje.html",
                                "/configuracion.html",
                                "/admin.html",
                                "/auth/**",
                                "/cuenta/**",
                                "/cliente/**",
                                "/admin/**",
                                "/css/**",
                                "/js/**",
                                "/assets/**",
                                "/favicon.ico").permitAll()
                        .requestMatchers(
                                "/oauth2/**",
                                "/login/oauth2/**",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/error").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .bearerTokenResolver(bearerTokenResolver)
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, exception) ->
                                escribirError(objectMapper, response, request.getRequestURI(),
                                        HttpStatus.UNAUTHORIZED, "Debe iniciar sesión para acceder a este recurso"))
                        .accessDeniedHandler((request, response, exception) ->
                                escribirError(objectMapper, response, request.getRequestURI(),
                                        HttpStatus.FORBIDDEN, "No tiene permisos para realizar esta acción")))
                .addFilterAfter(activeAccountFilter, BearerTokenAuthenticationFilter.class);

        if (clientRegistrations.getIfAvailable() != null) {
            http.oauth2Login(oauth2 -> oauth2
                    .userInfoEndpoint(userInfo -> userInfo.oidcUserService(googleOidcUserService))
                    .successHandler(successHandler)
                    .failureHandler(failureHandler));
        }

        return http.build();
    }

    @Bean
    public BearerTokenResolver bearerTokenResolver(AuthCookieService authCookies) {
        DefaultBearerTokenResolver authorizationHeader = new DefaultBearerTokenResolver();
        return request -> {
            String cookieToken = authCookies.resolver(request);
            return cookieToken != null ? cookieToken : authorizationHeader.resolve(request);
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("role");
        authorities.setAuthorityPrefix("");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins:http://localhost:3000}") String allowedOrigins
    ) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .toList());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-CSRF-TOKEN"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private void escribirError(
            ObjectMapper objectMapper,
            jakarta.servlet.http.HttpServletResponse response,
            String ruta,
            HttpStatus status,
            String mensaje
    ) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), new ErrorResponse(
                Instant.now(), status.value(), status.getReasonPhrase(), mensaje, ruta));
    }
}
