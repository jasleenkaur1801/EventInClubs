package com.campus.EventInClubs.service;

import com.campus.EventInClubs.domain.model.ClubAdminRequest;
import com.campus.EventInClubs.domain.model.Role;
import com.campus.EventInClubs.domain.model.User;
import com.campus.EventInClubs.repository.ClubAdminRequestRepository;
import com.campus.EventInClubs.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ClubAdminRequestService {

    private final ClubAdminRequestRepository clubAdminRequestRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // Create a new club admin request
    @Transactional
    public void createClubAdminRequest(String name, String email, String rawPassword) {
        // Check if email is already in use by an existing user
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email is already registered");
        }

        // Check if there's already a pending request for this email
        if (clubAdminRequestRepository.existsByEmail(email)) {
            ClubAdminRequest existing = clubAdminRequestRepository.findByEmail(email)
                    .orElseThrow();
            if (existing.getStatus() == ClubAdminRequest.ClubAdminRequestStatus.PENDING) {
                throw new IllegalArgumentException("A club admin request with this email is already pending approval");
            }
        }

        ClubAdminRequest request = ClubAdminRequest.builder()
                .name(name)
                .email(email)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .status(ClubAdminRequest.ClubAdminRequestStatus.PENDING)
                .requestedAt(Instant.now())
                .build();

        clubAdminRequestRepository.save(request);
    }

    // Get all club admin requests
    public List<ClubAdminRequest> getAllRequests() {
        return clubAdminRequestRepository.findAllByOrderByRequestedAtDesc();
    }

    // Get pending club admin requests
    public List<ClubAdminRequest> getPendingRequests() {
        return clubAdminRequestRepository.findByStatus(ClubAdminRequest.ClubAdminRequestStatus.PENDING);
    }

    // Approve a club admin request
    @Transactional
    public void approveRequest(Long requestId, Long superAdminId, Long clubId) {
        ClubAdminRequest request = clubAdminRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found"));

        if (request.getStatus() != ClubAdminRequest.ClubAdminRequestStatus.PENDING) {
            throw new IllegalArgumentException("Request is not pending");
        }

        // Create the club admin user (clubId is optional)
        User clubAdmin = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .passwordHash(request.getPasswordHash())
                .role(Role.CLUB_ADMIN)
                .clubId(clubId) // Can be null - club admin can manage any club
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        userRepository.save(clubAdmin);

        // Update request status
        request.setClubId(clubId);
        request.setStatus(ClubAdminRequest.ClubAdminRequestStatus.APPROVED);
        request.setApprovedAt(Instant.now());
        request.setApprovedBy(superAdminId);
        clubAdminRequestRepository.save(request);
    }

    // Reject a club admin request
    @Transactional
    public void rejectRequest(Long requestId, Long superAdminId, String reason) {
        ClubAdminRequest request = clubAdminRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found"));

        if (request.getStatus() != ClubAdminRequest.ClubAdminRequestStatus.PENDING) {
            throw new IllegalArgumentException("Request is not pending");
        }

        request.setStatus(ClubAdminRequest.ClubAdminRequestStatus.REJECTED);
        request.setApprovedBy(superAdminId);
        request.setRejectionReason(reason);
        clubAdminRequestRepository.save(request);
    }

    // Get request by ID
    public ClubAdminRequest getRequestById(Long requestId) {
        return clubAdminRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found"));
    }
}
