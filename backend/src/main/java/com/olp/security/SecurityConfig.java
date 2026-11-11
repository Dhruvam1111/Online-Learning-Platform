package com.olp.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // enables @PreAuthorize on controller methods
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    @Value("${app.cors.allowed-origins:${FRONTEND_URL:}}")
    private String allowedOrigins;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            CustomAccessDeniedHandler customAccessDeniedHandler,
            CustomAuthenticationEntryPoint customAuthenticationEntryPoint) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.customAccessDeniedHandler = customAccessDeniedHandler;
        this.customAuthenticationEntryPoint = customAuthenticationEntryPoint;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF — we're using JWT, not cookies for auth
                .csrf(csrf -> csrf.disable())

                // Enable CORS with the config below
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // Stateless sessions — no server-side session, JWT only
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Exception handling for 401 Unauthorized & 403 Forbidden
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(customAuthenticationEntryPoint)
                        .accessDeniedHandler(customAccessDeniedHandler))

                // URL-level access rules
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints (no login required)
                        .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/register/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/google").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/password-reset/**", "/api/auth/forgot-password", "/api/auth/reset-password", "/api/auth/otp/**").permitAll()
                        .requestMatchers("/api/auth/password-reset/**", "/api/auth/forgot-password", "/api/auth/reset-password", "/api/auth/otp/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auth/verify-email").permitAll()

                        .requestMatchers(HttpMethod.GET, "/api/courses", "/api/courses/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/instructors/{id:[0-9]+}/summary",
                                                         "/api/instructor/{id:[0-9]+}/summary",
                                                         "/api/instructors/{id:[0-9]+}",
                                                         "/api/instructor/{id:[0-9]+}",
                                                         "/api/instructors/{id:[0-9]+}/picture",
                                                         "/api/instructor/{id:[0-9]+}/picture").permitAll()
                        // Admin endpoints require ROLE_admin
                        .requestMatchers("/api/admin/**").hasRole("admin")

                        // Instructor endpoints require instructor or admin role
                        .requestMatchers("/api/instructor/**", "/api/instructors/**").hasAnyRole("instructor", "admin")

                        // Student endpoints require student role
                        .requestMatchers("/api/student/**", "/api/students/**").hasRole("student")

                        // Everything else requires authentication
                        .anyRequest().authenticated())

                // Add our JWT filter BEFORE Spring's default username/password filter
                .addFilterBefore(jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt with default strength (10 rounds)
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        List<String> origins = new ArrayList<>(List.of(
                "http://localhost:*",
                "http://127.0.0.1:*"));

        if (allowedOrigins != null && !allowedOrigins.isBlank()) {
            Arrays.stream(allowedOrigins.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .forEach(origins::add);
        }

        config.setAllowedOriginPatterns(origins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
