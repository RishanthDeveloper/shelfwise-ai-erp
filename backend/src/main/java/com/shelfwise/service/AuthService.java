package com.shelfwise.service;

import com.shelfwise.dto.LoginRequestDTO;
import com.shelfwise.dto.LoginResponseDTO;
import com.shelfwise.dto.RegisterRequestDTO;
import com.shelfwise.model.User;
import com.shelfwise.model.User.Role;
import com.shelfwise.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;

    public LoginResponseDTO login(LoginRequestDTO request) {
        String identifier = request.getIdentifier().trim();
        User user = userRepository.findByEmailOrUsername(identifier, identifier)
                .orElse(null);

        if (user == null) {
            log.warn("Login failed: User not found for identifier '{}'", identifier);
            return LoginResponseDTO.builder()
                    .success(false)
                    .message("Invalid email/username or password")
                    .build();
        }

        // Plain text password comparison for demo environment
        if (!user.getPassword().equals(request.getPassword())) {
            log.warn("Login failed: Invalid password for user '{}'", user.getUsername());
            return LoginResponseDTO.builder()
                    .success(false)
                    .message("Invalid email/username or password")
                    .build();
        }

        String token = "shelfwise_jwt_" + UUID.randomUUID().toString().replace("-", "");

        log.info("User logged in successfully: {} ({})", user.getUsername(), user.getRole());

        return LoginResponseDTO.builder()
                .success(true)
                .token(token)
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .avatarUrl(user.getAvatarUrl())
                .message("Login successful")
                .build();
    }

    @Transactional
    public LoginResponseDTO register(RegisterRequestDTO request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            return LoginResponseDTO.builder()
                    .success(false)
                    .message("Email is already registered")
                    .build();
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            return LoginResponseDTO.builder()
                    .success(false)
                    .message("Username is already taken")
                    .build();
        }

        Role assignedRole = request.getRole() != null ? request.getRole() : Role.STORE_MANAGER;

        User user = User.builder()
                .username(request.getUsername().trim())
                .email(request.getEmail().trim().toLowerCase())
                .password(request.getPassword())
                .fullName(request.getFullName().trim())
                .role(assignedRole)
                .avatarUrl("https://api.dicebear.com/7.x/avataaars/svg?seed=" + request.getUsername())
                .build();

        User saved = userRepository.save(user);
        String token = "shelfwise_jwt_" + UUID.randomUUID().toString().replace("-", "");

        log.info("Registered new user: {} ({})", saved.getUsername(), saved.getRole());

        return LoginResponseDTO.builder()
                .success(true)
                .token(token)
                .userId(saved.getId())
                .username(saved.getUsername())
                .email(saved.getEmail())
                .fullName(saved.getFullName())
                .role(saved.getRole())
                .avatarUrl(saved.getAvatarUrl())
                .message("Registration successful")
                .build();
    }

    public User getUserByEmailOrUsername(String identifier) {
        return userRepository.findByEmailOrUsername(identifier, identifier)
                .orElseThrow(() -> new RuntimeException("User not found: " + identifier));
    }
}
