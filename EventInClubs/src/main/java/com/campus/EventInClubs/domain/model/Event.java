package com.campus.EventInClubs.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Event {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "Event title is required")
    @Column(nullable = false)
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "start_date", nullable = true)
    private LocalDateTime startDate;
    
    @Column(name = "end_date", nullable = true)
    private LocalDateTime endDate;
    
    @Column(name = "registration_deadline")
    private LocalDateTime registrationDeadline;
    
    @Column(name = "idea_submission_deadline")
    private LocalDateTime ideaSubmissionDeadline;
    
    @Column(name = "accepts_ideas")
    @Builder.Default
    private Boolean acceptsIdeas = true;
    
    @Column(nullable = true)
    private String location;
    
    @Column(name = "max_participants")
    private Integer maxParticipants;
    
    @Column(name = "current_participants")
    @Builder.Default
    private Integer currentParticipants = 0;
    
    @Column(name = "registration_fee")
    @Builder.Default
    private Double registrationFee = 0.0;
    
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private EventStatus status = EventStatus.DRAFT;
    
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private EventType type = EventType.WORKSHOP;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id", nullable = false)
    private Club club;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizer_id", nullable = false)
    private User organizer;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hall_id")
    private Hall hall;
    
    @JsonIgnore
    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<EventRegistration> registrations;
    
    @JsonIgnore
    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Idea> ideas;
    
    @Column(name = "tags")
    private String tags; // Comma-separated tags
    
    @Column(name = "image_url")
    private String imageUrl;
    
    @Column(name = "ppt_file_url")
    private String pptFileUrl;
    
    @Column(name = "is_active", nullable = true)
    @Builder.Default
    private Boolean isActive = true;
    
    // Team event fields
    @Column(name = "is_team_event")
    @Builder.Default
    private Boolean isTeamEvent = false;
    
    @Column(name = "min_team_members")
    private Integer minTeamMembers;
    
    @Column(name = "max_team_members")
    private Integer maxTeamMembers;
    
    // Approval workflow fields
    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status")
    @Builder.Default
    private ApprovalStatus approvalStatus = ApprovalStatus.PENDING;
    
    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;
    
    @Column(name = "approval_date")
    private LocalDateTime approvalDate;
    
    @Column(name = "submitted_for_approval_date")
    private LocalDateTime submittedForApprovalDate;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    public enum EventStatus {
        DRAFT,              // Event is being planned by Club Admin
        PENDING_APPROVAL,   // Submitted by Club Admin, waiting for Super Admin approval
        APPROVED,           // Approved by Super Admin, visible to students
        REJECTED,           // Rejected by Super Admin, back to Club Admin for revision
        PUBLISHED,          // Legacy status, same as APPROVED
        REGISTRATION_CLOSED, // Registration deadline passed
        ONGOING,            // Event is currently happening
        COMPLETED,          // Event finished successfully
        CANCELLED           // Event was cancelled
    }
    
    public enum ApprovalStatus {
        PENDING,
        APPROVED,
        REJECTED
    }
    
    public enum EventType {
        WORKSHOP,
        SEMINAR,
        COMPETITION,
        HACKATHON,
        CONFERENCE,
        NETWORKING,
        SOCIAL,
        SPORTS,
        CULTURAL,
        TECHNICAL,
        OTHER
    }
}
