package com.campus.EventInClubs.service;

import com.campus.EventInClubs.domain.model.Club;
import com.campus.EventInClubs.domain.model.Event;
import com.campus.EventInClubs.domain.model.Hall;
import com.campus.EventInClubs.domain.model.Idea;
import com.campus.EventInClubs.domain.model.User;
import com.campus.EventInClubs.dto.EventDto;
import com.campus.EventInClubs.repository.ClubRepository;
import com.campus.EventInClubs.repository.EventRepository;
import com.campus.EventInClubs.repository.EventRegistrationRepository;
import com.campus.EventInClubs.repository.HallRepository;
import com.campus.EventInClubs.repository.IdeaRepository;
import com.campus.EventInClubs.repository.UserRepository;
import com.campus.EventInClubs.repository.VoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class EventService {
    
    private final EventRepository eventRepository;
    private final IdeaRepository ideaRepository;
    private final VoteRepository voteRepository;
    private final NotificationService notificationService;
    private final EventCleanupService eventCleanupService;
    private final EventRegistrationRepository eventRegistrationRepository;
    private final ClubRepository clubRepository;
    private final UserRepository userRepository;
    private final HallRepository hallRepository;
    
    public List<EventDto> getAllEvents() {
        return eventRepository.findAll().stream()
                .filter(event -> event.getIsActive() != null && event.getIsActive()) // Only show active events
                .filter(event -> event.getStatus() != Event.EventStatus.PUBLISHED) // Hide published events from general listing
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    public List<EventDto> getPublishedEventsForAdmin() {
        return eventRepository.findAll().stream()
                .filter(event -> event.getIsActive() == null || event.getIsActive()) // Show events that are active or have null isActive
                .filter(event -> event.getStatus() == Event.EventStatus.PUBLISHED || 
                                 event.getStatus() == Event.EventStatus.APPROVED) // Show both PUBLISHED and APPROVED events
                .filter(event -> event.getApprovalStatus() == Event.ApprovalStatus.APPROVED) // Must be approved by super admin
                .filter(event -> event.getStartDate() != null && event.getEndDate() != null) // Must have dates (properly approved)
                // Location is optional - can be derived from hall or entered directly
                .filter(event -> event.getMaxParticipants() != null) // Must have capacity (properly approved)
                // Keep visible until 3 hours after the end time
                .filter(event -> event.getEndDate().isAfter(java.time.LocalDateTime.now().minusHours(3)))
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    public List<EventDto> getActiveEventsForStudents() {
        // Use optimized query with JOIN FETCH to load all relationships in one query
        List<Event> activeEvents = eventRepository.findActiveEventsWithRelations(LocalDateTime.now());
        log.info("Found {} active events for students", activeEvents.size());
        
        // Use lightweight converter that doesn't trigger additional queries
        return activeEvents.stream()
                .map(this::convertToDtoLightweight)
                .collect(Collectors.toList());
    }
    
    public List<EventDto> getEventsForClubTopics() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneDayAgo = now.minusDays(1); // 24 hours ago
        return eventRepository.findAll().stream()
                .filter(event -> event.getIsActive() == null || event.getIsActive()) // Show events that are active or have null isActive
                // Include events in various stages: DRAFT, PENDING_APPROVAL, APPROVED, PUBLISHED, REJECTED
                .filter(event -> event.getStatus() == Event.EventStatus.DRAFT 
                        || event.getStatus() == Event.EventStatus.PENDING_APPROVAL
                        || event.getStatus() == Event.EventStatus.APPROVED
                        || event.getStatus() == Event.EventStatus.PUBLISHED
                        || event.getStatus() == Event.EventStatus.REJECTED)
                .filter(event -> event.getIdeaSubmissionDeadline() != null) // Must have deadline for idea submissions
                .filter(event -> event.getIdeaSubmissionDeadline().isAfter(oneDayAgo)) // Show events until 1 day after deadline
                // Note: Removed acceptsIdeas filter - club admins should see all events with deadlines for management
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    public List<EventDto> getEventsForClubTopicsDebug() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneDayAgo = now.minusDays(1);
        
        return eventRepository.findAll().stream()
                .filter(event -> event.getStatus() == Event.EventStatus.PUBLISHED)
                .filter(event -> event.getIdeaSubmissionDeadline() != null) // Only events with deadlines for debugging
                .map(event -> {
                    EventDto dto = convertToDto(event);
                    // Add debug info
                    return dto;
                })
                .collect(Collectors.toList());
    }
    
    public List<EventDto> getEventsByClub(Long clubId) {
        return eventRepository.findByClubId(clubId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    public List<EventDto> getUpcomingEvents() {
        return eventRepository.findUpcomingEvents(LocalDateTime.now()).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    public List<EventDto> getOngoingEvents() {
        return eventRepository.findOngoingEvents(LocalDateTime.now()).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    public EventDto getEventById(Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found with id: " + id));
        return convertToDto(event);
    }
    
    public EventDto createEvent(EventDto eventDto, Long organizerId) {
        User organizer = userRepository.findById(organizerId)
                .orElseThrow(() -> new RuntimeException("Organizer not found with id: " + organizerId));
        
        Club club = clubRepository.findById(eventDto.getClubId())
                .orElseThrow(() -> new RuntimeException("Club not found with id: " + eventDto.getClubId()));
        
        // Log the received deadline for debugging
        if (eventDto.getIdeaSubmissionDeadline() != null) {
            log.info("Creating event with idea submission deadline: {}", eventDto.getIdeaSubmissionDeadline());
        }
        
        // Fetch hall if hallId is provided
        Hall hall = null;
        if (eventDto.getHallId() != null) {
            hall = hallRepository.findById(eventDto.getHallId())
                    .orElseThrow(() -> new RuntimeException("Hall not found with id: " + eventDto.getHallId()));
            log.info("Assigning hall: {} to event", hall.getName());
        }
        
        // Determine approval status based on event type
        // Direct events (acceptsIdeas = false) are auto-approved
        // Idea collection events (acceptsIdeas = true) need approval
        boolean isDirectEvent = eventDto.getAcceptsIdeas() != null && !eventDto.getAcceptsIdeas();
        Event.ApprovalStatus approvalStatus = isDirectEvent ? Event.ApprovalStatus.APPROVED : Event.ApprovalStatus.PENDING;
        Event.EventStatus status = isDirectEvent ? Event.EventStatus.APPROVED : 
                                   (eventDto.getStatus() != null ? eventDto.getStatus() : Event.EventStatus.PUBLISHED);
        
        Event event = Event.builder()
                .title(eventDto.getTitle())
                .description(eventDto.getDescription())
                .startDate(eventDto.getStartDate())
                .endDate(eventDto.getEndDate())
                .registrationDeadline(eventDto.getRegistrationDeadline())
                .ideaSubmissionDeadline(eventDto.getIdeaSubmissionDeadline())
                .acceptsIdeas(eventDto.getAcceptsIdeas() != null ? eventDto.getAcceptsIdeas() : true)
                .location(eventDto.getLocation())
                .maxParticipants(eventDto.getMaxParticipants())
                .currentParticipants(0)
                .registrationFee(eventDto.getRegistrationFee() != null ? eventDto.getRegistrationFee() : 0.0)
                .status(status)
                .type(eventDto.getType())
                .club(club)
                .organizer(organizer)
                .hall(hall)
                .tags(eventDto.getTags())
                .imageUrl(eventDto.getImageUrl())
                .isActive(true)
                .approvalStatus(approvalStatus)
                .approvedBy(isDirectEvent ? organizer : null)
                .approvalDate(isDirectEvent ? LocalDateTime.now() : null)
                .build();
        
        Event savedEvent = eventRepository.save(event);
        log.info("Created new event: {} by organizer: {}", savedEvent.getTitle(), organizer.getName());
        log.info("Event details - ID: {}, Status: {}, ApprovalStatus: {}, AcceptsIdeas: {}, StartDate: {}, EndDate: {}, IsActive: {}", 
            savedEvent.getId(), savedEvent.getStatus(), savedEvent.getApprovalStatus(), 
            savedEvent.getAcceptsIdeas(), savedEvent.getStartDate(), savedEvent.getEndDate(), savedEvent.getIsActive());
        
        // Send notification to club members
        notificationService.createNotification(
                club.getAdminUser().getId(),
                "New Event Created",
                "A new event '" + savedEvent.getTitle() + "' has been created in " + club.getName(),
                com.campus.EventInClubs.domain.model.Notification.NotificationType.EVENT_ANNOUNCEMENT,
                savedEvent.getId(),
                "EVENT"
        );
        
        return convertToDto(savedEvent);
    }
    
    public EventDto updateEvent(Long id, EventDto eventDto) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found with id: " + id));
        
        Event.EventStatus oldStatus = event.getStatus();
        
        event.setTitle(eventDto.getTitle());
        event.setDescription(eventDto.getDescription());
        event.setStartDate(eventDto.getStartDate());
        event.setEndDate(eventDto.getEndDate());
        event.setRegistrationDeadline(eventDto.getRegistrationDeadline());
        event.setIdeaSubmissionDeadline(eventDto.getIdeaSubmissionDeadline());
        event.setAcceptsIdeas(eventDto.getAcceptsIdeas());
        event.setLocation(eventDto.getLocation());
        event.setMaxParticipants(eventDto.getMaxParticipants());
        event.setRegistrationFee(eventDto.getRegistrationFee());
        event.setStatus(eventDto.getStatus());
        event.setType(eventDto.getType());
        event.setTags(eventDto.getTags());
        event.setImageUrl(eventDto.getImageUrl());
        
        // Update hall if hallId is provided
        if (eventDto.getHallId() != null) {
            Hall hall = hallRepository.findById(eventDto.getHallId())
                    .orElseThrow(() -> new RuntimeException("Hall not found with id: " + eventDto.getHallId()));
            event.setHall(hall);
            log.info("Updated hall for event to: {}", hall.getName());
        } else {
            event.setHall(null);
        }
        
        Event savedEvent = eventRepository.save(event);
        log.info("Updated event: {}", savedEvent.getTitle());
        
        // Send notification if status changed to published
        if (oldStatus != Event.EventStatus.PUBLISHED && eventDto.getStatus() == Event.EventStatus.PUBLISHED) {
            notificationService.createNotification(
                    event.getClub().getAdminUser().getId(),
                    "Event Published",
                    "Event '" + savedEvent.getTitle() + "' is now open for registration!",
                    com.campus.EventInClubs.domain.model.Notification.NotificationType.EVENT_ANNOUNCEMENT,
                    savedEvent.getId(),
                    "EVENT"
            );
        }
        
        return convertToDto(savedEvent);
    }
    
    public void deleteEvent(Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found with id: " + id));
        
        // Send cancellation notification
        notificationService.createNotification(
                event.getClub().getAdminUser().getId(),
                "Event Cancelled",
                "Event '" + event.getTitle() + "' has been cancelled",
                com.campus.EventInClubs.domain.model.Notification.NotificationType.EVENT_ANNOUNCEMENT,
                event.getId(),
                "EVENT"
        );
        
        eventRepository.delete(event);
        log.info("Deleted event: {}", event.getTitle());
    }
    
    public EventDto publishEvent(Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found with id: " + id));
        
        event.setStatus(Event.EventStatus.PUBLISHED);
        Event savedEvent = eventRepository.save(event);
        
        // Send notification
        notificationService.createNotification(
                event.getClub().getAdminUser().getId(),
                "Event Published",
                "Event '" + savedEvent.getTitle() + "' is now live and accepting registrations!",
                com.campus.EventInClubs.domain.model.Notification.NotificationType.EVENT_ANNOUNCEMENT,
                savedEvent.getId(),
                "EVENT"
        );
        
        log.info("Published event: {}", savedEvent.getTitle());
        return convertToDto(savedEvent);
    }
    
    public List<EventDto> searchEvents(String keyword) {
        return eventRepository.searchEvents(keyword).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    public Long getEventCountByClub(Long clubId) {
        return eventRepository.countByClubId(clubId);
    }
    
    public List<EventDto> getEventsAcceptingIdeas() {
        return eventRepository.findEventsAcceptingIdeas(LocalDateTime.now()).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    public java.util.List<java.util.Map<String, Object>> getIdeasForEvent(Long eventId) {
        // Verify event exists
        eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));
        
        // Fetch ideas from database and convert to map
        return ideaRepository.findByEventIdAndIsActiveTrueOrderByCreatedAtDesc(eventId).stream()
                .map(idea -> {
                    java.util.Map<String, Object> map = new java.util.HashMap<>();
                    map.put("id", idea.getId());
                    map.put("title", idea.getTitle());
                    map.put("description", idea.getDescription());
                    map.put("expectedOutcome", idea.getExpectedOutcome());
                    map.put("submittedBy", idea.getSubmittedBy().getName());
                    map.put("submittedByEmail", idea.getSubmittedBy().getEmail());
                    map.put("submittedById", idea.getSubmittedBy().getId());
                    map.put("submittedAt", idea.getCreatedAt().toString());
                    map.put("eventId", eventId);
                    map.put("status", idea.getStatus().name());
                    return map;
                })
                .collect(java.util.stream.Collectors.toList());
    }

    public java.util.Map<String, Object> submitIdeaForEvent(Long eventId, java.util.Map<String, Object> ideaData, Long userId) {
        // Verify event exists and accepts ideas
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));
        
        if (!event.getAcceptsIdeas()) {
            throw new RuntimeException("This event is not accepting ideas");
        }
        
        // Check if idea submission deadline has passed
        if (event.getIdeaSubmissionDeadline() != null && 
            LocalDateTime.now().isAfter(event.getIdeaSubmissionDeadline())) {
            throw new RuntimeException("Idea submission deadline has passed");
        }
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Check if user has already submitted 2 ideas for this event
        Long existingIdeasCount = ideaRepository.countByEventIdAndSubmittedByIdAndIsActiveTrue(eventId, userId);
        if (existingIdeasCount >= 2) {
            throw new RuntimeException("You can only submit a maximum of 2 ideas per event. You have already submitted " + existingIdeasCount + " ideas for this event.");
        }
        
        // Create and save the idea
        Idea idea = Idea.builder()
                .title((String) ideaData.get("title"))
                .description((String) ideaData.get("description"))
                .expectedOutcome((String) ideaData.get("expectedOutcome"))
                .status(Idea.IdeaStatus.SUBMITTED)
                .problem(null) // Set to null or find appropriate problem if needed
                .event(event)
                .submittedBy(user)
                .isActive(true)
                .build();
        
        Idea savedIdea = ideaRepository.save(idea);
        
        log.info("Idea submitted for event '{}' by user '{}': {}", 
                event.getTitle(), user.getName(), ideaData.get("title"));
        
        // Send notification to event organizer
        if (!event.getOrganizer().getId().equals(userId)) {
            notificationService.notifyNewEventIdea(
                event.getOrganizer().getId(), 
                (String) ideaData.get("title"), 
                eventId
            );
        }
        
        return java.util.Map.of(
            "message", "Idea submitted successfully",
            "eventId", eventId,
            "ideaId", savedIdea.getId(),
            "ideaTitle", savedIdea.getTitle(),
            "submittedBy", user.getName(),
            "submittedAt", LocalDateTime.now().toString()
        );
    }

    public java.util.Map<String, Object> getUserIdeaSubmissionStatus(Long eventId, Long userId) {
        // Verify event exists
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));
        
        // Count existing ideas by user for this event
        Long existingIdeasCount = ideaRepository.countByEventIdAndSubmittedByIdAndIsActiveTrue(eventId, userId);
        Long remainingSubmissions = Math.max(0, 2 - existingIdeasCount);
        
        return java.util.Map.of(
            "eventId", eventId,
            "userId", userId,
            "submittedIdeas", existingIdeasCount,
            "remainingSubmissions", remainingSubmissions,
            "maxIdeasPerEvent", 2,
            "canSubmitMore", remainingSubmissions > 0
        );
    }
    
    // Lightweight converter for list views - doesn't query ideas/votes to avoid N+1
    private EventDto convertToDtoLightweight(Event event) {
        // Get actual registration count
        Long registrationCount = eventRegistrationRepository.countActiveByEventId(event.getId());
        int currentParticipants = registrationCount != null ? registrationCount.intValue() : 0;
        
        return EventDto.builder()
                .id(event.getId())
                .title(event.getTitle())
                .description(event.getDescription())
                .startDate(event.getStartDate())
                .endDate(event.getEndDate())
                .registrationDeadline(event.getRegistrationDeadline())
                .ideaSubmissionDeadline(event.getIdeaSubmissionDeadline())
                .acceptsIdeas(event.getAcceptsIdeas())
                .location(event.getLocation())
                .maxParticipants(event.getMaxParticipants())
                .currentParticipants(currentParticipants)
                .registrationFee(event.getRegistrationFee())
                .status(event.getStatus())
                .type(event.getType())
                .clubId(event.getClub().getId())
                .clubName(event.getClub().getName())
                .organizerId(event.getOrganizer().getId())
                .organizerName(event.getOrganizer().getName())
                .tags(event.getTags())
                .imageUrl(event.getImageUrl())
                .pptFileUrl(event.getPptFileUrl())
                .createdAt(event.getCreatedAt())
                .updatedAt(event.getUpdatedAt())
                .totalVotes(0) // Skip vote calculation for list view
                .isExpired(eventCleanupService.isEventExpired(event))
                .isViewOnly(eventCleanupService.isEventInViewOnlyMode(event))
                .hallId(event.getHall() != null ? event.getHall().getId() : null)
                .hallName(event.getHall() != null ? event.getHall().getName() : null)
                .hallCapacity(event.getHall() != null ? event.getHall().getSeatingCapacity() : null)
                .approvalStatus(event.getApprovalStatus())
                .rejectionReason(event.getRejectionReason())
                .approvedById(event.getApprovedBy() != null ? event.getApprovedBy().getId() : null)
                .approvedByName(event.getApprovedBy() != null ? event.getApprovedBy().getName() : null)
                .approvalDate(event.getApprovalDate())
                .submittedForApprovalDate(event.getSubmittedForApprovalDate())
                .build();
    }
    
    private EventDto convertToDto(Event event) {
        // Calculate total votes from all ideas submitted to this event
        int totalVotes = 0;
        
        // Get all ideas for this event directly from repository to avoid lazy loading issues
        List<Idea> eventIdeas = ideaRepository.findByEventIdAndIsActiveTrueOrderByCreatedAtDesc(event.getId());
        
        if (!eventIdeas.isEmpty()) {
            totalVotes = eventIdeas.stream()
                    .mapToInt(idea -> {
                        // Count both upvotes and downvotes for each idea
                        Long voteCount = voteRepository.countTotalVotesByIdeaId(idea.getId());
                        return voteCount != null ? voteCount.intValue() : 0;
                    })
                    .sum();
        }
        
        log.debug("Event {} has {} ideas with {} total votes", event.getId(), eventIdeas.size(), totalVotes);
        
        // Get actual registration count
        Long registrationCount = eventRegistrationRepository.countActiveByEventId(event.getId());
        int currentParticipants = registrationCount != null ? registrationCount.intValue() : 0;
        
        return EventDto.builder()
                .id(event.getId())
                .title(event.getTitle())
                .description(event.getDescription())
                .startDate(event.getStartDate())
                .endDate(event.getEndDate())
                .registrationDeadline(event.getRegistrationDeadline())
                .ideaSubmissionDeadline(event.getIdeaSubmissionDeadline())
                .acceptsIdeas(event.getAcceptsIdeas())
                .location(event.getLocation())
                .maxParticipants(event.getMaxParticipants())
                .currentParticipants(currentParticipants)
                .registrationFee(event.getRegistrationFee())
                .status(event.getStatus())
                .type(event.getType())
                .clubId(event.getClub().getId())
                .clubName(event.getClub().getName())
                .organizerId(event.getOrganizer().getId())
                .organizerName(event.getOrganizer().getName())
                .tags(event.getTags())
                .imageUrl(event.getImageUrl())
                .pptFileUrl(event.getPptFileUrl())
                .createdAt(event.getCreatedAt())
                .updatedAt(event.getUpdatedAt())
                .totalVotes(totalVotes)
            .isExpired(eventCleanupService.isEventExpired(event))
            .isViewOnly(eventCleanupService.isEventInViewOnlyMode(event))
            .hallId(event.getHall() != null ? event.getHall().getId() : null)
            .hallName(event.getHall() != null ? event.getHall().getName() : null)
            .hallCapacity(event.getHall() != null ? event.getHall().getSeatingCapacity() : null)
            .approvalStatus(event.getApprovalStatus())
            .rejectionReason(event.getRejectionReason())
            .approvedById(event.getApprovedBy() != null ? event.getApprovedBy().getId() : null)
            .approvedByName(event.getApprovedBy() != null ? event.getApprovedBy().getName() : null)
            .approvalDate(event.getApprovalDate())
            .submittedForApprovalDate(event.getSubmittedForApprovalDate())
            .build();
    }
    
    public EventDto updateEventStatus(Long eventId, String status) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found with id: " + eventId));
        try {
            Event.EventStatus eventStatus = Event.EventStatus.valueOf(status.toUpperCase());
            event.setStatus(eventStatus);
            Event savedEvent = eventRepository.save(event);
            
            log.info("Updated event {} status to {}", eventId, status);
            return convertToDto(savedEvent);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid event status: " + status);
        }
    }

    public EventDto activateEvent(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found with id: " + eventId));
        event.setIsActive(true);
        Event savedEvent = eventRepository.save(event);
        log.info("Activated event {} (set isActive=true)", eventId);
        return convertToDto(savedEvent);
    }
    
    @Transactional
    public EventDto approveEventProposal(Long proposalId, String eventName, String eventType, 
                                       String startDateTime, String endDateTime, String location,
                                       Integer maxParticipants, Double registrationFee, 
                                       String description,
                                       org.springframework.web.multipart.MultipartFile poster,
                                       String pptFileUrl,
                                       Long hallId) {
        
        log.info("Approving event proposal - proposalId: {}, name: {}, type: {}", proposalId, eventName, eventType);
        log.info("DateTime strings - start: {}, end: {}", startDateTime, endDateTime);
        log.info("Other params - hall: {}, maxParticipants: {}", hallId, maxParticipants);
        
        // Get the original proposal/event
        Event originalEvent = eventRepository.findById(proposalId)
                .orElseThrow(() -> new RuntimeException("Event proposal not found with id: " + proposalId));
        
        try {
            // Parse date-time strings - handle multiple ISO 8601 formats
            java.time.LocalDateTime startDate;
            java.time.LocalDateTime endDate;
            
            log.info("Parsing startDateTime: '{}'", startDateTime);
            log.info("Parsing endDateTime: '{}'", endDateTime);
            
            try {
                // Try parsing as ISO instant (with timezone like 2025-10-10T05:30:00.000Z)
                startDate = java.time.Instant.parse(startDateTime)
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDateTime();
                endDate = java.time.Instant.parse(endDateTime)
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDateTime();
                log.info("Successfully parsed as Instant with timezone");
            } catch (Exception e1) {
                log.info("Failed to parse as Instant, trying ZonedDateTime: {}", e1.getMessage());
                try {
                    // Try ZonedDateTime
                    startDate = java.time.ZonedDateTime.parse(startDateTime).toLocalDateTime();
                    endDate = java.time.ZonedDateTime.parse(endDateTime).toLocalDateTime();
                    log.info("Successfully parsed as ZonedDateTime");
                } catch (Exception e2) {
                    log.info("Failed to parse as ZonedDateTime, trying LocalDateTime: {}", e2.getMessage());
                    // Fallback to LocalDateTime format (without timezone)
                    startDate = java.time.LocalDateTime.parse(startDateTime);
                    endDate = java.time.LocalDateTime.parse(endDateTime);
                    log.info("Successfully parsed as LocalDateTime");
                }
            }
            
            log.info("Parsed dates - start: {}, end: {}", startDate, endDate);
            
            // Update the event with detailed information
            originalEvent.setTitle(eventName);
            originalEvent.setType(Event.EventType.valueOf(eventType.toUpperCase()));
            originalEvent.setStartDate(startDate);
            originalEvent.setEndDate(endDate);
            originalEvent.setLocation(location);
            originalEvent.setMaxParticipants(maxParticipants);
            originalEvent.setRegistrationFee(registrationFee);
            originalEvent.setDescription(description);
            // Since super admin is disabled, club admin approval is final
            // Auto-approve the event when club admin submits the proposal
            originalEvent.setStatus(Event.EventStatus.APPROVED);
            originalEvent.setApprovalStatus(Event.ApprovalStatus.APPROVED);
            originalEvent.setSubmittedForApprovalDate(java.time.LocalDateTime.now());
            originalEvent.setApprovalDate(java.time.LocalDateTime.now());
            originalEvent.setApprovedBy(originalEvent.getOrganizer()); // Club admin approves their own event
            originalEvent.setUpdatedAt(java.time.LocalDateTime.now());
            
            log.info("Event '{}' auto-approved by club admin (super admin disabled)", eventName);
            
            // Assign hall if provided
            if (hallId != null) {
                Hall hall = hallRepository.findById(hallId)
                        .orElseThrow(() -> new RuntimeException("Hall not found with id: " + hallId));
                
                // Verify hall is available for the event time
                if (maxParticipants != null && hall.getSeatingCapacity() < maxParticipants) {
                    throw new RuntimeException("Selected hall capacity (" + hall.getSeatingCapacity() + 
                                             ") is insufficient for the number of participants (" + maxParticipants + ")");
                }
                
                originalEvent.setHall(hall);
                // Set location from hall if not provided
                if (location == null || location.trim().isEmpty()) {
                    originalEvent.setLocation(hall.getName() + " - " + hall.getLocation());
                }
                log.info("Assigned hall '{}' to event '{}'", hall.getName(), eventName);
            }
            
            // Handle poster upload if provided
            if (poster != null && !poster.isEmpty()) {
                try {
                    String posterUrl = handlePosterUpload(poster);
                    originalEvent.setImageUrl(posterUrl);
                } catch (Exception e) {
                    log.warn("Failed to upload poster, continuing without poster: {}", e.getMessage());
                    // Continue without poster - don't fail the entire approval
                }
            }
            
            // Handle PPT file URL if provided
            if (pptFileUrl != null && !pptFileUrl.trim().isEmpty()) {
                originalEvent.setPptFileUrl(pptFileUrl.trim());
                log.info("Set PPT file URL for event '{}': {}", eventName, pptFileUrl);
            }
            
            Event savedEvent = eventRepository.save(originalEvent);
            
            // Send notification to club admin about the approved event
            notificationService.createNotification(
                originalEvent.getClub().getAdminUser().getId(),
                "Event Approved",
                String.format("Event '%s' has been approved and is now live!", eventName),
                com.campus.EventInClubs.domain.model.Notification.NotificationType.SYSTEM,
                originalEvent.getId(),
                "EVENT"
            );
            
            log.info("Approved and updated event proposal: {} -> {}", proposalId, eventName);
            return convertToDto(savedEvent);
            
        } catch (java.time.format.DateTimeParseException e) {
            log.error("DateTime parse error - start: '{}', end: '{}'", startDateTime, endDateTime, e);
            throw new RuntimeException(String.format(
                "Invalid date-time format. Received start: '%s', end: '%s'. Error: %s",
                startDateTime, endDateTime, e.getMessage()));
        } catch (IllegalArgumentException e) {
            log.error("Invalid event type: {}", eventType, e);
            throw new RuntimeException("Invalid event type: " + eventType);
        } catch (Exception e) {
            log.error("Error approving event proposal: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to approve event proposal: " + e.getMessage());
        }
    }
    
    private String handlePosterUpload(org.springframework.web.multipart.MultipartFile poster) {
        try {
            // Create uploads directory in the project root
            String uploadDir = System.getProperty("user.dir") + "/uploads/posters/";
            java.nio.file.Path uploadPath = java.nio.file.Paths.get(uploadDir);
            if (!java.nio.file.Files.exists(uploadPath)) {
                java.nio.file.Files.createDirectories(uploadPath);
            }
            
            // Generate unique filename
            String originalFilename = poster.getOriginalFilename();
            String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String uniqueFilename = java.util.UUID.randomUUID().toString() + fileExtension;
            
            // Save file
            java.nio.file.Path filePath = uploadPath.resolve(uniqueFilename);
            poster.transferTo(filePath.toFile());
            
            // Return relative URL
            return "/uploads/posters/" + uniqueFilename;
            
        } catch (Exception e) {
            log.error("Error uploading poster: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to upload poster: " + e.getMessage());
        }
    }
    
    @Transactional
    public void deleteApprovedEvents() {
        // Mark all events with PUBLISHED status as inactive instead of deleting
        List<Event> publishedEvents = eventRepository.findAll().stream()
                .filter(event -> event.getStatus() == Event.EventStatus.PUBLISHED)
                .collect(java.util.stream.Collectors.toList());
        
        for (Event event : publishedEvents) {
            log.info("Marking approved event as inactive: {} (ID: {})", event.getTitle(), event.getId());
            event.setIsActive(false);
            event.setStatus(Event.EventStatus.CANCELLED);
            eventRepository.save(event);
        }
        
        log.info("Marked {} approved events as inactive", publishedEvents.size());
    }
    
    @Transactional
    public void deleteEventByTitle(String title) {
        List<Event> events = eventRepository.findAll().stream()
                .filter(event -> event.getTitle() != null && event.getTitle().equalsIgnoreCase(title))
                .collect(java.util.stream.Collectors.toList());
        
        if (events.isEmpty()) {
            throw new RuntimeException("Event not found with title: " + title);
        }
        
        for (Event event : events) {
            log.info("Deleting event: {} (ID: {})", event.getTitle(), event.getId());
            eventRepository.delete(event);
        }
        
        log.info("Deleted {} event(s) with title: {}", events.size(), title);
    }
    
    // New approval workflow methods
    
    @Transactional
    public EventDto submitEventForApproval(Long eventId, Long hallId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found with id: " + eventId));
        
        // Validate that event has all required fields
        if (event.getTitle() == null || event.getTitle().trim().isEmpty()) {
            throw new RuntimeException("Event title is required");
        }
        if (event.getStartDate() == null || event.getEndDate() == null) {
            throw new RuntimeException("Event start and end dates are required");
        }
        if (event.getMaxParticipants() == null) {
            throw new RuntimeException("Maximum participants is required");
        }
        
        // Assign hall if provided
        if (hallId != null) {
            Hall hall = hallRepository.findById(hallId)
                    .orElseThrow(() -> new RuntimeException("Hall not found with id: " + hallId));
            
            // Verify hall capacity
            if (hall.getSeatingCapacity() < event.getMaxParticipants()) {
                throw new RuntimeException("Selected hall capacity is insufficient for the number of participants");
            }
            
            event.setHall(hall);
            // Set location from hall
            event.setLocation(hall.getName() + " - " + hall.getLocation());
        }
        
        // Update status and approval fields
        event.setStatus(Event.EventStatus.PENDING_APPROVAL);
        event.setApprovalStatus(Event.ApprovalStatus.PENDING);
        event.setSubmittedForApprovalDate(LocalDateTime.now());
        event.setRejectionReason(null); // Clear any previous rejection reason
        
        Event savedEvent = eventRepository.save(event);
        
        log.info("Event '{}' submitted for approval by club admin", event.getTitle());
        
        return convertToDto(savedEvent);
    }
    
    @Transactional
    public EventDto approveEvent(Long eventId, Long adminId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found with id: " + eventId));
        
        if (event.getStatus() != Event.EventStatus.PENDING_APPROVAL && event.getStatus() != Event.EventStatus.PUBLISHED) {
            throw new RuntimeException("Event is not pending approval");
        }
        
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin not found"));
        
        // Update approval status and make it visible to students
        // Since super admin is disabled, club admin approval is final
        event.setStatus(Event.EventStatus.APPROVED);
        event.setApprovalStatus(Event.ApprovalStatus.APPROVED);
        event.setApprovedBy(admin);
        event.setApprovalDate(LocalDateTime.now());
        event.setRejectionReason(null);
        
        Event savedEvent = eventRepository.save(event);
        
        // Send notification to club admin
        notificationService.createNotification(
                event.getOrganizer().getId(),
                "Event Approved",
                String.format("Your event '%s' has been approved and is now visible to students!", event.getTitle()),
                com.campus.EventInClubs.domain.model.Notification.NotificationType.SYSTEM,
                event.getId(),
                "EVENT"
        );
        
        log.info("Event '{}' approved by club admin {}", event.getTitle(), admin.getName());
        
        return convertToDto(savedEvent);
    }
    
    @Transactional
    public EventDto rejectEvent(Long eventId, Long adminId, String rejectionReason) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found with id: " + eventId));
        
        if (event.getStatus() != Event.EventStatus.PENDING_APPROVAL && event.getStatus() != Event.EventStatus.PUBLISHED) {
            throw new RuntimeException("Event is not pending approval");
        }
        
        if (rejectionReason == null || rejectionReason.trim().isEmpty()) {
            throw new RuntimeException("Rejection reason is required");
        }
        
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin not found"));
        
        // Update rejection status
        event.setStatus(Event.EventStatus.REJECTED);
        event.setApprovalStatus(Event.ApprovalStatus.REJECTED);
        event.setRejectionReason(rejectionReason);
        event.setApprovedBy(admin);
        event.setApprovalDate(LocalDateTime.now());
        
        Event savedEvent = eventRepository.save(event);
        
        // Send notification to club admin
        notificationService.createNotification(
                event.getOrganizer().getId(),
                "Event Rejected",
                String.format("Your event '%s' has been rejected. Reason: %s", event.getTitle(), rejectionReason),
                com.campus.EventInClubs.domain.model.Notification.NotificationType.SYSTEM,
                event.getId(),
                "EVENT"
        );
        
        log.info("Event '{}' rejected by club admin {} with reason: {}", 
                event.getTitle(), admin.getName(), rejectionReason);
        
        return convertToDto(savedEvent);
    }
    
    public List<EventDto> getPendingApprovalEvents() {
        return eventRepository.findAll().stream()
                .filter(event -> event.getStatus() == Event.EventStatus.PENDING_APPROVAL)
                .filter(event -> event.getIsActive() == null || event.getIsActive())
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    public List<EventDto> getApprovedEventsForStudents() {
        return eventRepository.findAll().stream()
                .filter(event -> event.getStatus() == Event.EventStatus.APPROVED || event.getStatus() == Event.EventStatus.PUBLISHED)
                .filter(event -> event.getIsActive() == null || event.getIsActive())
                .filter(event -> event.getStartDate() != null && event.getStartDate().isAfter(LocalDateTime.now())) // Only future events
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    public List<EventDto> getApprovedEvents() {
        return eventRepository.findAll().stream()
                .filter(event -> event.getStatus() == Event.EventStatus.APPROVED || event.getStatus() == Event.EventStatus.PUBLISHED)
                .filter(event -> event.getApprovalStatus() == Event.ApprovalStatus.APPROVED)
                .filter(event -> event.getIsActive() == null || event.getIsActive())
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    public List<EventDto> getRejectedEvents() {
        return eventRepository.findAll().stream()
                .filter(event -> event.getStatus() == Event.EventStatus.REJECTED)
                .filter(event -> event.getApprovalStatus() == Event.ApprovalStatus.REJECTED)
                .filter(event -> event.getIsActive() == null || event.getIsActive())
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    public List<EventDto> getRejectedEventsForClubAdmin(Long clubId) {
        return eventRepository.findAll().stream()
                .filter(event -> event.getStatus() == Event.EventStatus.REJECTED)
                .filter(event -> event.getClub().getId().equals(clubId))
                .filter(event -> event.getIsActive() == null || event.getIsActive())
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    @Transactional
    public EventDto resubmitRejectedEvent(Long eventId, String eventName, String eventType,
                                         String startDateTime, String endDateTime, String location,
                                         Integer maxParticipants, Double registrationFee,
                                         String description, Long hallId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found with id: " + eventId));
        
        // Verify event is in rejected state
        if (event.getStatus() != Event.EventStatus.REJECTED) {
            throw new RuntimeException("Only rejected events can be resubmitted");
        }
        
        log.info("Club admin resubmitting rejected event {} with updates", eventId);
        
        try {
            // Parse date-time strings
            java.time.LocalDateTime startDate;
            java.time.LocalDateTime endDate;
            
            try {
                startDate = java.time.ZonedDateTime.parse(startDateTime).toLocalDateTime();
                endDate = java.time.ZonedDateTime.parse(endDateTime).toLocalDateTime();
            } catch (Exception e) {
                startDate = java.time.LocalDateTime.parse(startDateTime);
                endDate = java.time.LocalDateTime.parse(endDateTime);
            }
            
            // Update event with new information
            event.setTitle(eventName);
            event.setType(Event.EventType.valueOf(eventType.toUpperCase()));
            event.setStartDate(startDate);
            event.setEndDate(endDate);
            event.setLocation(location);
            event.setMaxParticipants(maxParticipants);
            event.setRegistrationFee(registrationFee);
            event.setDescription(description);
            
            // Assign hall if provided
            if (hallId != null) {
                Hall hall = hallRepository.findById(hallId)
                        .orElseThrow(() -> new RuntimeException("Hall not found with id: " + hallId));
                
                if (maxParticipants != null && hall.getSeatingCapacity() < maxParticipants) {
                    throw new RuntimeException("Selected hall capacity is insufficient");
                }
                
                event.setHall(hall);
                if (location == null || location.trim().isEmpty()) {
                    event.setLocation(hall.getName() + " - " + hall.getLocation());
                }
            }
            
            // Resubmit for approval
            event.setStatus(Event.EventStatus.PENDING_APPROVAL);
            event.setApprovalStatus(Event.ApprovalStatus.PENDING);
            event.setSubmittedForApprovalDate(java.time.LocalDateTime.now());
            event.setRejectionReason(null); // Clear rejection reason
            event.setApprovedBy(null); // Clear previous approval data
            event.setApprovalDate(null);
            event.setUpdatedAt(java.time.LocalDateTime.now());
            
            Event savedEvent = eventRepository.save(event);
            
            log.info("Rejected event '{}' updated and resubmitted for approval", eventName);
            
            return convertToDto(savedEvent);
            
        } catch (Exception e) {
            log.error("Error resubmitting rejected event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to resubmit event: " + e.getMessage());
        }
    }
}
