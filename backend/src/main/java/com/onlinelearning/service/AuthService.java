package com.onlinelearning.service;

import com.onlinelearning.dto.AuthResponse;
import com.onlinelearning.dto.GoogleAuthRequest;
import com.onlinelearning.dto.LoginRequest;
import com.onlinelearning.dto.RegisterRequest;
import com.onlinelearning.dto.UserProfileResponse;
import com.onlinelearning.exception.BadRequestException;
import com.onlinelearning.exception.ResourceNotFoundException;
import com.onlinelearning.model.User;
import com.onlinelearning.repository.UserRepository;
import com.onlinelearning.security.GoogleAuthService;
import com.onlinelearning.security.JwtTokenProvider;
import com.onlinelearning.security.UserPrincipal;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final GoogleAuthService googleAuthService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtTokenProvider tokenProvider,
                       GoogleAuthService googleAuthService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
        this.googleAuthService = googleAuthService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("An account with email '" + email + "' already exists");
        }

        String role = request.getRole().trim().toLowerCase();
        if (!role.equals("student") && !role.equals("instructor")) {
            throw new BadRequestException("Invalid role: '" + role + "'. Must be either 'student' or 'instructor'");
        }

        String encodedPassword = passwordEncoder.encode(request.getPassword());
        User newUser = new User(request.getName().trim(), email, encodedPassword, role);
        User savedUser = userRepository.save(newUser);

        String jwt = tokenProvider.generateToken(
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getName(),
                savedUser.getRole()
        );

        return new AuthResponse(
                jwt,
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getEmail(),
                savedUser.getRole()
        );
    }

    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        String jwt = tokenProvider.generateToken(
                userPrincipal.getId(),
                userPrincipal.getUsername(),
                userPrincipal.getName(),
                userPrincipal.getRole()
        );

        return new AuthResponse(
                jwt,
                userPrincipal.getId(),
                userPrincipal.getName(),
                userPrincipal.getUsername(),
                userPrincipal.getRole()
        );
    }

    @Transactional
    public AuthResponse loginWithGoogle(GoogleAuthRequest request) {
        User user = googleAuthService.processGoogleUser(request.getIdToken(), request.getRole());

        String jwt = tokenProvider.generateToken(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole()
        );

        return new AuthResponse(
                jwt,
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole()
        );
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentUser(UserPrincipal userPrincipal) {
        User user = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userPrincipal.getId()));

        return new UserProfileResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getCreatedAt()
        );
    }
}
