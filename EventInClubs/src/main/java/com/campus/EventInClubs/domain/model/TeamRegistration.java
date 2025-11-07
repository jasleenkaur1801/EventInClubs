package com.campus.EventInClubs.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "team_registrations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamRegistration {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;
    
    @Column(name = "team_name", nullable = false)
    private String teamName;
    
    @Column(name = "team_size", nullable = false)
    private Integer teamSize;
    
    @Column(name = "member_roll_numbers", nullable = false, columnDefinition = "TEXT")
    private String memberRollNumbers; // Comma-separated roll numbers
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registered_by", nullable = false)
    private User registeredBy;
    
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "status")
    private RegistrationStatus status = RegistrationStatus.REGISTERED;
    
    @Column(name = "registration_notes")
    private String registrationNotes;
    
    @Column(name = "payment_status")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;
    
    @CreationTimestamp
    @Column(name = "registered_at", nullable = false, updatable = false)
    private LocalDateTime registeredAt;
    
    public enum RegistrationStatus {
        REGISTERED,
        WAITLISTED,
        CANCELLED,
        ATTENDED,
        NO_SHOW
    }
    
    public enum PaymentStatus {
        PENDING,
        PAID,
        REFUNDED,
        NOT_REQUIRED
    }
}
