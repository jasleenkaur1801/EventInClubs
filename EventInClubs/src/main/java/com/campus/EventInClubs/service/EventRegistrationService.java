package com.campus.EventInClubs.service;

import com.campus.EventInClubs.domain.model.Event;
import com.campus.EventInClubs.domain.model.EventRegistration;
import com.campus.EventInClubs.domain.model.User;
import com.campus.EventInClubs.dto.EventRegistrationDto;
import com.campus.EventInClubs.repository.EventRegistrationRepository;
import com.campus.EventInClubs.repository.EventRepository;
import com.campus.EventInClubs.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class EventRegistrationService {
    
    private final EventRegistrationRepository registrationRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    
    public EventRegistrationDto registerForEvent(Long eventId, Long userId, String notes, String rollNumber) {
        // Check if event exists and is open for registration
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));
        
        if (event.getStatus() != Event.EventStatus.PUBLISHED && 
            event.getStatus() != Event.EventStatus.APPROVED) {
            throw new RuntimeException("Event is not open for registration");
        }
        
        // Check if user exists
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Check if user is already registered (any status except CANCELLED)
        if (registrationRepository.existsByEventIdAndUserId(eventId, userId)) {
            EventRegistration existingRegistration = registrationRepository.findByEventIdAndUserId(eventId, userId).orElse(null);
            if (existingRegistration != null && existingRegistration.getStatus() != EventRegistration.RegistrationStatus.CANCELLED) {
                throw new RuntimeException("User is already registered for this event");
            }
        }
        
        // Check capacity (count REGISTERED + ATTENDED + NO_SHOW towards capacity)
        Long currentRegistrations = registrationRepository.countActiveByEventId(eventId);
        if (event.getMaxParticipants() != null && currentRegistrations >= event.getMaxParticipants()) {
            // Register as waitlisted
            return createRegistration(event, user, EventRegistration.RegistrationStatus.WAITLISTED, notes, rollNumber);
        }
        
        // Register normally
        EventRegistrationDto registration = createRegistration(event, user, EventRegistration.RegistrationStatus.REGISTERED, notes, rollNumber);
        
        // Send notification to user
        notificationService.createNotification(
            userId,
            "Event Registration Confirmed",
            String.format("You have successfully registered for '%s'", event.getTitle()),
            com.campus.EventInClubs.domain.model.Notification.NotificationType.SYSTEM,
            eventId,
            "EVENT"
        );
        
        // Send notification to event organizer
        notificationService.createNotification(
            event.getClub().getAdminUser().getId(),
            "New Event Registration",
            String.format("New registration for '%s' by %s", event.getTitle(), user.getName()),
            com.campus.EventInClubs.domain.model.Notification.NotificationType.SYSTEM,
            eventId,
            "EVENT"
        );
        
        log.info("User {} registered for event {}", userId, eventId);
        return registration;
    }
    
    private EventRegistrationDto createRegistration(Event event, User user, EventRegistration.RegistrationStatus status, String notes, String rollNumber) {
        EventRegistration registration = EventRegistration.builder()
                .event(event)
                .user(user)
                .status(status)
                .registrationNotes(notes)
                .rollNumber(rollNumber)
                .paymentStatus(event.getRegistrationFee() == null || event.getRegistrationFee() == 0 
                    ? EventRegistration.PaymentStatus.NOT_REQUIRED 
                    : EventRegistration.PaymentStatus.PENDING)
                .build();
        
        EventRegistration saved = registrationRepository.save(registration);
        return convertToDto(saved);
    }
    
    public List<EventRegistrationDto> getEventRegistrations(Long eventId) {
        List<EventRegistration> registrations = registrationRepository.findByEventIdOrderByRegisteredAtDesc(eventId);
        return registrations.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    public List<EventRegistrationDto> getUserRegistrations(Long userId) {
        List<EventRegistration> registrations = registrationRepository.findByUserIdOrderByRegisteredAtDesc(userId);
        return registrations.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    public Long getRegistrationCount(Long eventId) {
        return registrationRepository.countActiveByEventId(eventId);
    }
    
    public EventRegistrationDto updateRegistrationStatus(Long registrationId, EventRegistration.RegistrationStatus newStatus) {
        EventRegistration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new RuntimeException("Registration not found"));
        
        registration.setStatus(newStatus);
        EventRegistration updated = registrationRepository.save(registration);
        
        // Send notification to user about status change
        notificationService.createNotification(
            registration.getUser().getId(),
            "Registration Status Updated",
            String.format("Your registration status for '%s' has been updated to %s", 
                registration.getEvent().getTitle(), newStatus.toString()),
            com.campus.EventInClubs.domain.model.Notification.NotificationType.SYSTEM,
            registration.getEvent().getId(),
            "EVENT"
        );
        
        log.info("Registration {} status updated to {}", registrationId, newStatus);
        return convertToDto(updated);
    }
    
    public void cancelRegistration(Long eventId, Long userId) {
        EventRegistration registration = registrationRepository.findByEventIdAndUserId(eventId, userId)
                .orElseThrow(() -> new RuntimeException("Registration not found"));
        
        registration.setStatus(EventRegistration.RegistrationStatus.CANCELLED);
        registrationRepository.save(registration);
        
        // Send notification
        notificationService.createNotification(
            userId,
            "Registration Cancelled",
            String.format("Your registration for '%s' has been cancelled", registration.getEvent().getTitle()),
            com.campus.EventInClubs.domain.model.Notification.NotificationType.SYSTEM,
            eventId,
            "EVENT"
        );
        
        log.info("User {} cancelled registration for event {}", userId, eventId);
    }

    public EventRegistrationDto setRollNumber(Long eventId, Long userId, String rollNumber) {
        EventRegistration registration = registrationRepository.findByEventIdAndUserId(eventId, userId)
                .orElseThrow(() -> new RuntimeException("Registration not found"));
        registration.setRollNumber(rollNumber);
        EventRegistration saved = registrationRepository.save(registration);
        return convertToDto(saved);
    }
    
    private EventRegistrationDto convertToDto(EventRegistration registration) {
        return EventRegistrationDto.builder()
                .id(registration.getId())
                .eventId(registration.getEvent().getId())
                .eventTitle(registration.getEvent().getTitle())
                .eventDescription(registration.getEvent().getDescription())
                .eventStartDate(registration.getEvent().getStartDate())
                .eventEndDate(registration.getEvent().getEndDate())
                .eventLocation(registration.getEvent().getLocation())
                .registrationFee(registration.getEvent().getRegistrationFee())
                .userId(registration.getUser().getId())
                .userName(registration.getUser().getName())
                .userEmail(registration.getUser().getEmail())
                .status(registration.getStatus())
                .registrationNotes(registration.getRegistrationNotes())
                .rollNumber(registration.getRollNumber())
                .paymentStatus(registration.getPaymentStatus())
                .registeredAt(registration.getRegisteredAt())
                .build();
    }
}
