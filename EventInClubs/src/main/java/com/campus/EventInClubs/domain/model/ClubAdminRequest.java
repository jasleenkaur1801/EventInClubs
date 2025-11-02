package com.campus.EventInClubs.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "club_admin_requests")
public class ClubAdminRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, length = 100)
    private String name;

    @NotBlank
    @Email
    @Column(unique = true, nullable = false, length = 160)
    private String email;

    @NotBlank
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "club_id")
    private Long clubId; // Assigned by super admin during approval

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    @Builder.Default
    private ClubAdminRequestStatus status = ClubAdminRequestStatus.PENDING;

    @Column(name = "requested_at", nullable = false)
    @Builder.Default
    private Instant requestedAt = Instant.now();

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "approved_by")
    private Long approvedBy; // ID of the super admin who approved

    @Column(name = "rejection_reason")
    private String rejectionReason;

    public enum ClubAdminRequestStatus {
        PENDING,
        APPROVED,
        REJECTED
    }
}
