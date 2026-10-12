# PERSON 2 — Auth Backend Developer

## What You Own

You are the **sole owner** of the authentication and authorization layer. This includes user registration, login, logout, JWT token management, and the security middleware that every other backend developer (Person 3 and Person 4) will import and use.

**Nobody else writes auth code.** If Person 3 or Person 4 needs to protect a route, they use the middleware you provide.

---

## Tech Stack

Based on the deployment doc (`05-deployment.md`), the backend uses:
- **Java 17** with **Spring Boot 3.x**
- **Spring Security** for auth filters
- **JJWT** library for JWT creation and validation
- **BCrypt** (built into Spring Security) for password hashing
- **Spring Data JPA** for database access
- **PostgreSQL** (provided by Pranamya's Docker Compose)

---

## Your Deliverables

1. `User` entity (JPA mapping to the `users` table)
2. `AuthController` with register, login, logout, and me endpoints
3. `AuthService` with business logic
4. `UserRepository` (Spring Data JPA)
5. `JwtUtil` utility class
6. `JwtAuthenticationFilter` (the middleware everyone else uses)
7. `SecurityConfig` (Spring Security configuration)
8. Request/Response DTOs
9. Global exception handler

---

## Project Structure

All your code goes under `src/main/java/com/olp/` (olp = Online Learning Platform). Create exactly these files:

```
backend/
├── pom.xml
├── src/
│   └── main/
│       ├── java/com/olp/
│       │   ├── OnlineLearningApplication.java        ← Main class (exists already or create)
│       │   ├── entity/
│       │   │   └── User.java                         ← JPA entity
│       │   ├── repository/
│       │   │   └── UserRepository.java               ← Data access
│       │   ├── dto/
│       │   │   ├── RegisterRequest.java              ← Registration input
│       │   │   ├── LoginRequest.java                 ← Login input
│       │   │   ├── AuthResponse.java                 ← Login/register success output
│       │   │   ├── UserResponse.java                 ← User info (no password)
│       │   │   └── ErrorResponse.java                ← Standardized error output
│       │   ├── service/
│       │   │   └── AuthService.java                  ← Business logic
│       │   ├── controller/
│       │   │   └── AuthController.java               ← REST endpoints
│       │   ├── security/
│       │   │   ├── JwtUtil.java                      ← Token create/validate/parse
│       │   │   ├── JwtAuthenticationFilter.java      ← Intercepts every request
│       │   │   └── SecurityConfig.java               ← Spring Security setup
│       │   └── exception/
│       │       ├── DuplicateEmailException.java      ← Custom exception
│       │       ├── InvalidCredentialsException.java  ← Custom exception
│       │       └── GlobalExceptionHandler.java       ← Catches and formats errors
│       └── resources/
│           └── application.properties                ← DB + JWT config
```

---

## File 1: `pom.xml` Dependencies

Add these dependencies to the project's `pom.xml`. If the project doesn't exist yet, go to https://start.spring.io and generate a project with these settings:
- **Group:** com.olp
- **Artifact:** backend
- **Java:** 17
- **Spring Boot:** 3.2.x or 3.3.x
- **Dependencies:** Spring Web, Spring Security, Spring Data JPA, PostgreSQL Driver, Lombok, Validation

Then manually add the JJWT dependency:

```xml
<dependencies>
    <!-- Spring Boot Starters -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- PostgreSQL -->
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
        <scope>runtime</scope>
    </dependency>

    <!-- JWT (JJWT library) -->
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-api</artifactId>
        <version>0.12.5</version>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-impl</artifactId>
        <version>0.12.5</version>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-jackson</artifactId>
        <version>0.12.5</version>
        <scope>runtime</scope>
    </dependency>

    <!-- Lombok (optional but reduces boilerplate) -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

> **IMPORTANT:** The JJWT version must be `0.12.5` (or newer 0.12.x). Older versions (0.9.x, 0.11.x) have a completely different API and the code below will NOT compile with them.

---

## File 2: `application.properties`

```properties
# ===== Database =====
# These match Pranamya's docker-compose.yml defaults
spring.datasource.url=${DATABASE_URL:jdbc:postgresql://localhost:5432/online_learning}
spring.datasource.username=${DATABASE_USERNAME:postgres}
spring.datasource.password=${DATABASE_PASSWORD:postgres}

# validate = check that entities match existing tables (don't auto-create/alter)
# Pranamya's SQL scripts create the tables. We just validate they match.
spring.jpa.hibernate.ddl-auto=validate

# Show SQL in logs during development (disable in production)
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

# ===== JWT =====
app.jwt.secret=${JWT_SECRET:this-is-a-dev-only-secret-key-that-must-be-at-least-256-bits-long-for-HS256}
app.jwt.expiration-ms=${JWT_EXPIRATION_MS:86400000}

# ===== Server =====
server.port=${PORT:8080}
```

### Why `ddl-auto=validate`?
Pranamya owns the schema. His SQL scripts create the tables. Setting this to `validate` means Hibernate checks that your `@Entity` classes match the real tables, but never creates or modifies tables itself. If there's a mismatch, the app fails to start with a clear error — which is exactly what you want.

---

## File 3: `User.java` (Entity)

```java
package com.olp.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(nullable = false, length = 20)
    private String role;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // ===== Constructors =====

    public User() {}

    public User(String name, String email, String passwordHash, String role) {
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    // ===== Getters and Setters =====

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
```

### Critical Mapping Notes
| Java Field | DB Column | Why |
|---|---|---|
| `passwordHash` | `password_hash` | The `@Column(name = "password_hash")` annotation maps the camelCase Java field to the snake_case DB column. Without this, Hibernate looks for a column called `passwordhash` and fails. |
| `createdAt` | `created_at` | Same camelCase-to-snake_case mapping. Spring Boot's default naming strategy handles this automatically, but the explicit `@Column(name = ...)` makes it unambiguous. |
| `@Table(name = "users")` | `users` table | Required because the default would be `user`, which is a reserved word in PostgreSQL. |
| `GenerationType.IDENTITY` | `SERIAL` | Tells Hibernate to let PostgreSQL auto-generate the ID using its SERIAL sequence. Do NOT use `GenerationType.AUTO` — it will try to create a `hibernate_sequence` table. |

---

## File 4: `UserRepository.java`

```java
package com.olp.repository;

import com.olp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    // Spring Data JPA generates the query automatically from the method name
    Optional<User> findByEmail(String email);

    // Used to check for duplicate email during registration
    boolean existsByEmail(String email);
}
```

That's it. No implementation class needed. Spring Data JPA generates the SQL at runtime.

---

## File 5: DTOs (Data Transfer Objects)

### `RegisterRequest.java`
```java
package com.olp.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RegisterRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must be 100 characters or fewer")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    private String password;

    @NotBlank(message = "Role is required")
    @Pattern(regexp = "^(student|instructor)$", message = "Role must be 'student' or 'instructor'")
    private String role;

    // ===== Getters and Setters =====

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
```

### `LoginRequest.java`
```java
package com.olp.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class LoginRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;

    // ===== Getters and Setters =====

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
```

### `AuthResponse.java`
```java
package com.olp.dto;

public class AuthResponse {

    private String message;
    private String token;
    private UserResponse user;

    public AuthResponse(String message, String token, UserResponse user) {
        this.message = message;
        this.token = token;
        this.user = user;
    }

    // ===== Getters =====

    public String getMessage() { return message; }
    public String getToken() { return token; }
    public UserResponse getUser() { return user; }
}
```

### `UserResponse.java`
```java
package com.olp.dto;

public class UserResponse {

    private Integer id;
    private String name;
    private String email;
    private String role;

    public UserResponse(Integer id, String name, String email, String role) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.role = role;
    }

    // ===== Getters =====

    public Integer getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
}
```

### `ErrorResponse.java`
```java
package com.olp.dto;

import java.time.LocalDateTime;

public class ErrorResponse {

    private int status;
    private String error;
    private LocalDateTime timestamp;

    public ErrorResponse(int status, String error) {
        this.status = status;
        this.error = error;
        this.timestamp = LocalDateTime.now();
    }

    // ===== Getters =====

    public int getStatus() { return status; }
    public String getError() { return error; }
    public LocalDateTime getTimestamp() { return timestamp; }
}
```

---

## File 6: `JwtUtil.java`

This is the core JWT utility. It creates tokens, validates them, and extracts user info from them.

```java
package com.olp.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtUtil(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs) {
        // The secret string must be at least 256 bits (32 bytes) for HS256
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    /**
     * Create a JWT token containing the user's ID and role.
     * Person 3 and Person 4: this is what gets sent to the client on login.
     */
    public String generateToken(Integer userId, String role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    /**
     * Validate the token signature and check if it's expired.
     * Returns true only if the token is valid and not expired.
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            // Token is invalid, expired, or tampered with
            return false;
        }
    }

    /**
     * Extract the user ID from a valid token.
     * Call validateToken() first — this method assumes the token is valid.
     */
    public Integer getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return Integer.parseInt(claims.getSubject());
    }

    /**
     * Extract the role from a valid token.
     */
    public String getRoleFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.get("role", String.class);
    }
}
```

### How the JWT Payload Looks
When decoded (e.g., on https://jwt.io), a token from this app looks like:
```json
{
  "sub": "3",
  "role": "student",
  "iat": 1695600000,
  "exp": 1695686400
}
```
- `sub` = user ID (as a string, parsed back to Integer when reading)
- `role` = "student" or "instructor"
- `iat` = issued at (Unix timestamp)
- `exp` = expires at (Unix timestamp, 24 hours after iat by default)

---

## File 7: `JwtAuthenticationFilter.java`

**This is the middleware that Person 3 and Person 4 depend on.** It runs before every request, checks for a valid JWT in the `Authorization` header, and sets the authenticated user in Spring Security's context.

```java
package com.olp.security;

import com.olp.entity.User;
import com.olp.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, UserRepository userRepository) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // 1. Extract the token from the Authorization header
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // No token present — let the request continue (it will be
            // blocked later by SecurityConfig if the endpoint requires auth)
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7); // Remove "Bearer " prefix

        // 2. Validate the token
        if (!jwtUtil.validateToken(token)) {
            // Token is invalid or expired
            filterChain.doFilter(request, response);
            return;
        }

        // 3. Extract user info from the token
        Integer userId = jwtUtil.getUserIdFromToken(token);
        String role = jwtUtil.getRoleFromToken(token);

        // 4. Load the full user from the database
        //    (ensures the user still exists and hasn't been deleted since the token was issued)
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            filterChain.doFilter(request, response);
            return;
        }

        // 5. Set the authentication in Spring Security's context
        //    The "ROLE_" prefix is a Spring Security convention.
        //    Person 3 uses @PreAuthorize("hasRole('instructor')") — Spring adds the prefix automatically.
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        user,                                                    // principal (the User object)
                        null,                                                    // credentials (not needed, we have a token)
                        List.of(new SimpleGrantedAuthority("ROLE_" + role))      // authorities
                );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 6. Continue the filter chain
        filterChain.doFilter(request, response);
    }
}
```

### How Person 3 and Person 4 Access the Current User

After this filter runs, the authenticated user is available in any controller method:

```java
// In any controller method:
@GetMapping("/some-endpoint")
public ResponseEntity<?> someMethod(Authentication authentication) {
    User currentUser = (User) authentication.getPrincipal();
    Integer userId = currentUser.getId();
    String role = currentUser.getRole();
    // ... use it
}
```

This is the contract. Person 3 and Person 4 depend on `authentication.getPrincipal()` returning a `User` entity object. **Do not change this to return a different type without telling them.**

---

## File 8: `SecurityConfig.java`

```java
package com.olp.security;

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

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // enables @PreAuthorize on controller methods
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF — we're using JWT, not cookies for auth
                .csrf(csrf -> csrf.disable())

                // Enable CORS with the config below
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // Stateless sessions — no server-side session, JWT only
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // URL-level access rules
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints (no login required)
                        .requestMatchers(HttpMethod.POST, "/api/auth/register").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()

                        // Everything else requires authentication
                        .anyRequest().authenticated()
                )

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
        config.setAllowedOrigins(List.of(
                "http://localhost:3000",       // React dev server
                "http://localhost:5173"        // Vite dev server
                // Person 4 will add the deployed frontend URL here later
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
```

### What This Config Does

| Setting | Purpose |
|---|---|
| `csrf.disable()` | We use JWT in the Authorization header, not cookies. CSRF protection is irrelevant and would block legitimate API calls. |
| `SessionCreationPolicy.STATELESS` | No server-side sessions. Every request must include a valid JWT. |
| `permitAll()` on register/login | These two endpoints are the only ones accessible without a token. |
| `anyRequest().authenticated()` | Everything else requires a valid JWT. Person 3 and Person 4's endpoints are automatically protected. |
| `@EnableMethodSecurity` | Allows Person 3 and Person 4 to use `@PreAuthorize("hasRole('instructor')")` on their controller methods for role-based access. |

---

## File 9: `AuthService.java`

```java
package com.olp.service;

import com.olp.dto.*;
import com.olp.entity.User;
import com.olp.exception.DuplicateEmailException;
import com.olp.exception.InvalidCredentialsException;
import com.olp.repository.UserRepository;
import com.olp.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    /**
     * Register a new user.
     * Throws DuplicateEmailException if the email already exists.
     */
    public AuthResponse register(RegisterRequest request) {
        // 1. Check for duplicate email
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException("An account with this email already exists");
        }

        // 2. Hash the password
        String hashedPassword = passwordEncoder.encode(request.getPassword());

        // 3. Create and save the user
        User user = new User(
                request.getName(),
                request.getEmail().toLowerCase().trim(),
                hashedPassword,
                request.getRole().toLowerCase().trim()
        );
        user = userRepository.save(user);

        // 4. Generate a JWT token
        String token = jwtUtil.generateToken(user.getId(), user.getRole());

        // 5. Build and return the response
        UserResponse userResponse = new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole()
        );
        return new AuthResponse("Registration successful", token, userResponse);
    }

    /**
     * Authenticate a user.
     * Throws InvalidCredentialsException on failure.
     * IMPORTANT: Never reveal whether the email or password was wrong.
     */
    public AuthResponse login(LoginRequest request) {
        // 1. Look up user by email
        User user = userRepository.findByEmail(request.getEmail().toLowerCase().trim())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        // 2. Compare passwords
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        // 3. Generate a JWT token
        String token = jwtUtil.generateToken(user.getId(), user.getRole());

        // 4. Build and return the response
        UserResponse userResponse = new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole()
        );
        return new AuthResponse("Login successful", token, userResponse);
    }

    /**
     * Get the current user's info from a User entity.
     * Called by the /me endpoint.
     */
    public UserResponse getCurrentUser(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole()
        );
    }
}
```

---

## File 10: `AuthController.java`

```java
package com.olp.controller;

import com.olp.dto.*;
import com.olp.entity.User;
import com.olp.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * POST /api/auth/register
     * Public — no token required.
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * POST /api/auth/login
     * Public — no token required.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/auth/logout
     * Authenticated — requires a valid token.
     *
     * Since we use stateless JWT (no server-side session), there's nothing
     * to invalidate on the server. The client simply discards its token.
     * This endpoint exists so the frontend has a consistent API to call.
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        // Stateless JWT: nothing to do server-side.
        // The client should delete its stored token.
        return ResponseEntity.ok().body(java.util.Map.of("message", "Logged out successfully"));
    }

    /**
     * GET /api/auth/me
     * Authenticated — requires a valid token.
     * Returns the current user's info.
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        UserResponse response = authService.getCurrentUser(currentUser);
        return ResponseEntity.ok(response);
    }
}
```

---

## File 11: Custom Exceptions + Global Handler

### `DuplicateEmailException.java`
```java
package com.olp.exception;

public class DuplicateEmailException extends RuntimeException {
    public DuplicateEmailException(String message) {
        super(message);
    }
}
```

### `InvalidCredentialsException.java`
```java
package com.olp.exception;

public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException(String message) {
        super(message);
    }
}
```

### `GlobalExceptionHandler.java`
```java
package com.olp.exception;

import com.olp.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateEmail(DuplicateEmailException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)  // 409
                .body(new ErrorResponse(409, ex.getMessage()));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(InvalidCredentialsException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)  // 401
                .body(new ErrorResponse(401, ex.getMessage()));
    }

    /**
     * Handles validation errors from @Valid on request DTOs.
     * Returns the first validation error message.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("Validation failed");

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)  // 400
                .body(new ErrorResponse(400, message));
    }
}
```

---

## API Contract Reference

This is the exact JSON shape the frontend (Person 5) will code against. Do not change these without telling Person 5.

### POST `/api/auth/register`

**Request:**
```
POST /api/auth/register
Content-Type: application/json

{
  "name": "Jane Doe",
  "email": "jane@example.com",
  "password": "securepass123",
  "role": "student"
}
```

**Success (201):**
```json
{
  "message": "Registration successful",
  "token": "eyJhbGciOiJIUzI1NiIs...",
  "user": {
    "id": 6,
    "name": "Jane Doe",
    "email": "jane@example.com",
    "role": "student"
  }
}
```

**Duplicate Email (409):**
```json
{
  "status": 409,
  "error": "An account with this email already exists",
  "timestamp": "2026-09-30T23:00:00"
}
```

**Validation Error (400):**
```json
{
  "status": 400,
  "error": "password: Password must be between 8 and 100 characters",
  "timestamp": "2026-09-30T23:00:00"
}
```

---

### POST `/api/auth/login`

**Request:**
```
POST /api/auth/login
Content-Type: application/json

{
  "email": "alice@student.com",
  "password": "password123"
}
```

**Success (200):**
```json
{
  "message": "Login successful",
  "token": "eyJhbGciOiJIUzI1NiIs...",
  "user": {
    "id": 3,
    "name": "Alice Johnson",
    "email": "alice@student.com",
    "role": "student"
  }
}
```

**Wrong Credentials (401):**
```json
{
  "status": 401,
  "error": "Invalid email or password",
  "timestamp": "2026-09-30T23:00:00"
}
```

---

### GET `/api/auth/me`

**Request:**
```
GET /api/auth/me
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
```

**Success (200):**
```json
{
  "id": 3,
  "name": "Alice Johnson",
  "email": "alice@student.com",
  "role": "student"
}
```

**No Token / Invalid Token (401):**
Spring Security returns its default 401 response.

---

### POST `/api/auth/logout`

**Request:**
```
POST /api/auth/logout
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
```

**Success (200):**
```json
{
  "message": "Logged out successfully"
}
```

---

## Testing with Postman

Test each endpoint in this exact order:

### Test 1: Register a student
```
POST http://localhost:8080/api/auth/register
Body (JSON):
{
  "name": "Test Student",
  "email": "test@student.com",
  "password": "testpass123",
  "role": "student"
}
Expected: 201, response contains token and user with role "student"
```

### Test 2: Register duplicate email
```
POST http://localhost:8080/api/auth/register
Body (JSON):
{
  "name": "Test Student Again",
  "email": "test@student.com",
  "password": "testpass123",
  "role": "student"
}
Expected: 409, error message about duplicate email
```

### Test 3: Register with short password
```
POST http://localhost:8080/api/auth/register
Body (JSON):
{
  "name": "Bad User",
  "email": "bad@user.com",
  "password": "short",
  "role": "student"
}
Expected: 400, validation error about password length
```

### Test 4: Register with invalid role
```
POST http://localhost:8080/api/auth/register
Body (JSON):
{
  "name": "Bad User",
  "email": "bad2@user.com",
  "password": "longpassword",
  "role": "admin"
}
Expected: 400, validation error about role
```

### Test 5: Login with correct credentials
```
POST http://localhost:8080/api/auth/login
Body (JSON):
{
  "email": "test@student.com",
  "password": "testpass123"
}
Expected: 200, response contains token
Save the token from the response for Tests 6-8.
```

### Test 6: Login with wrong password
```
POST http://localhost:8080/api/auth/login
Body (JSON):
{
  "email": "test@student.com",
  "password": "wrongpassword"
}
Expected: 401, "Invalid email or password"
```

### Test 7: Get current user with valid token
```
GET http://localhost:8080/api/auth/me
Headers:
  Authorization: Bearer <paste token from Test 5>
Expected: 200, returns user info
```

### Test 8: Get current user without token
```
GET http://localhost:8080/api/auth/me
(No Authorization header)
Expected: 401
```

### Test 9: Login with seed data
```
POST http://localhost:8080/api/auth/login
Body (JSON):
{
  "email": "alice@student.com",
  "password": "password123"
}
Expected: 200, returns Alice's info with role "student"
```

### Test 10: Login as seed instructor
```
POST http://localhost:8080/api/auth/login
Body (JSON):
{
  "email": "sarah@instructor.com",
  "password": "password123"
}
Expected: 200, returns Sarah's info with role "instructor"
```

---

## Integration Checklist (What Person 3 and Person 4 Need from You)

When you're done, confirm these to the team:

| Item | Details |
|---|---|
| **How to get the current user in a controller** | `User currentUser = (User) authentication.getPrincipal();` where `Authentication authentication` is a method parameter |
| **How to restrict to instructors only** | Add `@PreAuthorize("hasRole('instructor')")` to the controller method |
| **How to restrict to students only** | Add `@PreAuthorize("hasRole('student')")` to the controller method |
| **How the frontend sends the token** | `Authorization: Bearer <token>` header on every request |
| **What happens if no token is sent** | Spring Security returns 401 automatically |
| **What happens if an expired token is sent** | Spring Security returns 401 automatically |
| **Where is the User entity?** | `com.olp.entity.User` — Person 3 and 4 import this, they do NOT create their own |
| **Where is UserRepository?** | `com.olp.repository.UserRepository` — shared, they can import and use `findById()` if needed |

---

## Verification Checklist

- [ ] Application starts without errors (`mvn spring-boot:run` or run from IDE)
- [ ] Hibernate validates entities against Pranamya's DB schema without errors
- [ ] All 10 Postman tests pass
- [ ] Seed data users (alice, sarah, etc.) can log in with "password123"
- [ ] Password is NEVER returned in any API response (check every endpoint)
- [ ] JWT secret is read from environment variable, not hardcoded in code
- [ ] Registration with duplicate email returns 409, not 500
- [ ] Login failure returns generic "Invalid email or password" — never "email not found" or "wrong password" separately
- [ ] CORS allows requests from `http://localhost:3000` and `http://localhost:5173`
- [ ] Person 3 can import `User`, `UserRepository`, and use `@PreAuthorize("hasRole('instructor')")` successfully
- [ ] Person 4 can import `User`, `UserRepository`, and use `@PreAuthorize("hasRole('student')")` successfully

---

## Common Pitfalls

| Pitfall | How to Avoid |
|---|---|
| `GenerationType.AUTO` instead of `IDENTITY` | AUTO creates a `hibernate_sequence` table. Pranamya's schema uses `SERIAL`, which needs `IDENTITY`. |
| Missing `@Table(name = "users")` | Without it, Hibernate looks for a table called `user` (singular), which is a PostgreSQL reserved word. |
| JJWT version mismatch | The code above uses JJWT 0.12.x API. If you use 0.9.x or 0.11.x, methods like `verifyWith()` and `parseSignedClaims()` don't exist. Use exactly `0.12.5`. |
| JWT secret too short | HS256 requires at least 256 bits (32 bytes). If your secret string is shorter, JJWT throws `WeakKeyException` at startup. |
| Returning `passwordHash` in JSON | The `User` entity has a `passwordHash` field. If you accidentally return a `User` entity directly (instead of `UserResponse`), the hash leaks. Always use `UserResponse`. |
| Spring Security default login page | Without `SecurityConfig`, Spring Boot shows an auto-generated login form at `/login`. The config above disables this entirely. |
| Not normalizing email | `Alice@Student.COM` and `alice@student.com` should be treated as the same email. The service lowercases and trims email before saving and before lookup. |

---

## Git Branch

Work on branch: `feature/auth-backend`

```bash
git checkout -b feature/auth-backend
# ... do all your work ...
git add .
git commit -m "feat: add auth module (register, login, JWT, middleware)"
git push origin feature/auth-backend
# Then open a Pull Request to main
```
