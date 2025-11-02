package com.campus.EventInClubs.service;

import com.campus.EventInClubs.domain.model.Role;
import com.campus.EventInClubs.domain.model.User;
import com.campus.EventInClubs.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // Register a new user
    public User register(String name, String email, String rawPassword, Role role) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already in use");
        }

        // Validate email domain for STUDENT role
        if (role == Role.STUDENT && !email.toLowerCase().endsWith("@chitkara.edu.in")) {
            throw new IllegalArgumentException("Students must register with official Chitkara email ID (@chitkara.edu.in)");
        }

        User u = User.builder()
                .name(name)
                .email(email)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .role(role)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        return userRepository.save(u);
    }

    // Find user by email
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    // Get all users
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // Get users by role
    public List<User> getUsersByRole(Role role) {
        return userRepository.findByRole(role);
    }

    // Get user counts by role
    public Map<String, Long> getUserCounts() {
        Map<String, Long> counts = new HashMap<>();
        counts.put("total", userRepository.count());
        counts.put("students", userRepository.countByRole(Role.STUDENT));
        counts.put("clubAdmins", userRepository.countByRole(Role.CLUB_ADMIN));
        counts.put("superAdmins", userRepository.countByRole(Role.SUPER_ADMIN));
        return counts;
    }

    // Get user analytics
    public Map<String, Object> getUserAnalytics() {
        Map<String, Object> analytics = new HashMap<>();
        
        // Basic counts
        long totalUsers = userRepository.count();
        long students = userRepository.countByRole(Role.STUDENT);
        long clubAdmins = userRepository.countByRole(Role.CLUB_ADMIN);
        long superAdmins = userRepository.countByRole(Role.SUPER_ADMIN);
        
        analytics.put("totalUsers", totalUsers);
        analytics.put("students", students);
        analytics.put("clubAdmins", clubAdmins);
        analytics.put("superAdmins", superAdmins);
        
        // Percentages
        if (totalUsers > 0) {
            analytics.put("studentPercentage", Math.round((students * 100.0) / totalUsers));
            analytics.put("clubAdminPercentage", Math.round((clubAdmins * 100.0) / totalUsers));
            analytics.put("superAdminPercentage", Math.round((superAdmins * 100.0) / totalUsers));
        }
        
        return analytics;
    }
}
