package com.ledgerline.config;

import java.util.Arrays;
import java.util.List;

import com.ledgerline.entity.User;
import com.ledgerline.enums.AccountStatus;
import com.ledgerline.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.AbstractOAuth2TokenAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.core.convert.converter.Converter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain apiSecurityFilterChain(
            HttpSecurity http,
            Converter<Jwt, ? extends AbstractOAuth2TokenAuthenticationToken<Jwt>> jwtAuthenticationConverter)
            throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/auth/register", "/api/auth/login",
                                "/api/v1/auth/register", "/api/v1/auth/login").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/accounts/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/accounts/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/accounts/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/accounts/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/audit/**").denyAll()
                        .requestMatchers(HttpMethod.PUT, "/api/audit/**").denyAll()
                        .requestMatchers(HttpMethod.PATCH, "/api/audit/**").denyAll()
                        .requestMatchers(HttpMethod.DELETE, "/api/audit/**").denyAll()
                        .requestMatchers(HttpMethod.POST, "/api/ledger/**").denyAll()
                        .requestMatchers(HttpMethod.PUT, "/api/ledger/**").denyAll()
                        .requestMatchers(HttpMethod.PATCH, "/api/ledger/**").denyAll()
                        .requestMatchers(HttpMethod.DELETE, "/api/ledger/**").denyAll()
                        .requestMatchers("/api/audit/**").hasAnyRole("ADMIN", "AUDITOR")
                        .requestMatchers("/api/auditor/**").hasRole("AUDITOR")
                        .requestMatchers(HttpMethod.GET, "/api/transactions/**", "/api/ledger/**")
                                .hasAnyRole("ADMIN", "ACCOUNT_HOLDER", "AUDITOR")
                        .requestMatchers(HttpMethod.POST, "/api/transfers/**", "/api/transactions/**")
                                .hasRole("ACCOUNT_HOLDER")
                        .requestMatchers(HttpMethod.PUT, "/api/transfers/**", "/api/transactions/**")
                                .hasRole("ACCOUNT_HOLDER")
                        .requestMatchers(HttpMethod.PATCH, "/api/transfers/**", "/api/transactions/**")
                                .hasRole("ACCOUNT_HOLDER")
                        .requestMatchers(HttpMethod.DELETE, "/api/transfers/**", "/api/transactions/**")
                                .hasRole("ACCOUNT_HOLDER")
                        .requestMatchers("/api/accounts/**").hasAnyRole("ADMIN", "ACCOUNT_HOLDER", "AUDITOR")
                        .requestMatchers("/api/auth/me", "/api/v1/auth/me").authenticated()
                        .anyRequest().denyAll())
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint(authenticationEntryPoint())
                        .accessDeniedHandler(accessDeniedHandler()))
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
                .build();
    }

    @Bean
    Converter<Jwt, ? extends AbstractOAuth2TokenAuthenticationToken<Jwt>> jwtAuthenticationConverter(
            UserRepository userRepository) {
        return jwt -> {
            long userId;
            try {
                userId = Long.parseLong(jwt.getSubject());
            } catch (NumberFormatException exception) {
                throw new BadCredentialsException("Invalid bearer token.");
            }
            User user = userRepository.findById(userId)
                    .filter(candidate -> candidate.getAccountStatus() == AccountStatus.ACTIVE)
                    .orElseThrow(() -> new BadCredentialsException("Invalid bearer token."));
            return new JwtAuthenticationToken(
                    jwt,
                    List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())),
                    jwt.getSubject());
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${ledgerline.cors.allowed-origins}") String configuredOrigins) {
        List<String> allowedOrigins = Arrays.stream(configuredOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList();

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Idempotency-Key"));
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        source.registerCorsConfiguration("/actuator/health", configuration);
        return source;
    }

    private static AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, exception) -> writeProblem(
                response, HttpStatus.UNAUTHORIZED,
                "Authentication required", "Authentication is required or the token is invalid.");
    }

    private static AccessDeniedHandler accessDeniedHandler() {
        return (request, response, exception) -> writeProblem(
                response, HttpStatus.FORBIDDEN,
                "Access denied", "You are not permitted to perform this action.");
    }

    private static void writeProblem(
            jakarta.servlet.http.HttpServletResponse response,
            HttpStatus status,
            String title,
            String detail) throws java.io.IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        if (status == HttpStatus.UNAUTHORIZED) {
            response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        }
        response.getWriter().write(
                "{\"type\":\"about:blank\",\"title\":\"" + title + "\",\"status\":"
                        + status.value() + ",\"detail\":\"" + detail + "\"}");
    }
}
