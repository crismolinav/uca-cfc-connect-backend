package sv.edu.udb.ucacfcconnect.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Apagamos la protección CSRF temporalmente para permitir POST, PUT, DELETE
                .csrf(csrf -> csrf.disable())
                // Permitimos que cualquier petición pase sin necesidad de iniciar sesión
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

        return http.build();
    }
}