package com.campus.EventInClubs.dto;

import com.campus.EventInClubs.domain.model.Event;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventDto {
    private Long id;
    @NotBlank(message = "Topic title is required")
    private String title;
    private String description;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime registrationDeadline;
    private LocalDateTime ideaSubmissionDeadline;
    private Boolean acceptsIdeas;
    private String location;
    private Integer maxParticipants;
    private Integer currentParticipants;
    private Double registrationFee;
    private Event.EventStatus status;
    private Event.EventType type;
    private Long clubId;
    private String clubName;
    private Long organizerId;
    private String organizerName;
    private String tags;
    private String imageUrl;
    private String pptFileUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Deadline status fields
    private Boolean isExpired;
    private Boolean isViewOnly;
    
    // Active status
    private Boolean isActive;
    
    // Vote information
    private Integer totalVotes;
    
    // Hall information
    private Long hallId;
    private String hallName;
    private Integer hallCapacity;
    
    // Approval workflow information
    private Event.ApprovalStatus approvalStatus;
    private String rejectionReason;
    private Long approvedById;
    private String approvedByName;
    private LocalDateTime approvalDate;
    private LocalDateTime submittedForApprovalDate;
    
    // Proposal information
    private Long proposalId;
    
    // Helper method to check if registration is open
    public boolean isRegistrationOpen() {
        return status == Event.EventStatus.PUBLISHED && 
               (registrationDeadline == null || LocalDateTime.now().isBefore(registrationDeadline)) &&
               (maxParticipants == null || currentParticipants < maxParticipants);
    }
    
    // Helper method to check if idea submission is open
    public boolean isIdeaSubmissionOpen() {
        return acceptsIdeas != null && acceptsIdeas && 
               status == Event.EventStatus.PUBLISHED &&
               (ideaSubmissionDeadline == null || LocalDateTime.now().isBefore(ideaSubmissionDeadline));
    }
    
    // Helper method to check if event is upcoming
    public boolean isUpcoming() {
        return startDate != null && LocalDateTime.now().isBefore(startDate);
    }
    
    // Helper method to check if event is ongoing
    public boolean isOngoing() {
        LocalDateTime now = LocalDateTime.now();
        return startDate != null && now.isAfter(startDate) && 
               (endDate == null || now.isBefore(endDate));
    }
}
