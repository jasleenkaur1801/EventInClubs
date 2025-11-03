package com.campus.EventInClubs.service;

import com.campus.EventInClubs.domain.model.Event;
import com.campus.EventInClubs.domain.model.EventRegistration;
import com.campus.EventInClubs.repository.EventRegistrationRepository;
import com.campus.EventInClubs.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventReminderService {
    
    private final EventRepository eventRepository;
    private final EventRegistrationRepository registrationRepository;
    private final EmailService emailService;
    
    /**
     * Send reminder emails for events happening today
     * Runs every day at 8:00 AM
     */
    @Scheduled(cron = "0 0 8 * * *") // Run at 8:00 AM every day
    public void sendDailyEventReminders() {
        log.info("Starting daily event reminder job...");
        
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(23, 59, 59);
        
        // Find all events happening today
        List<Event> todaysEvents = eventRepository.findAll().stream()
                .filter(event -> event.getStartDate() != null)
                .filter(event -> {
                    LocalDateTime startDate = event.getStartDate();
                    return !startDate.isBefore(startOfDay) && !startDate.isAfter(endOfDay);
                })
                .filter(event -> event.getStatus() == Event.EventStatus.PUBLISHED || 
                                 event.getStatus() == Event.EventStatus.APPROVED)
                .toList();
        
        log.info("Found {} events happening today", todaysEvents.size());
        
        // For each event, send reminders to all registered students
        for (Event event : todaysEvents) {
            sendRemindersForEvent(event);
        }
        
        log.info("Daily event reminder job completed");
    }
    
    /**
     * Send reminders to all registered students for a specific event
     */
    private void sendRemindersForEvent(Event event) {
        try {
            // Get all active registrations for this event
            List<EventRegistration> registrations = registrationRepository.findByEventIdOrderByRegisteredAtDesc(event.getId())
                    .stream()
                    .filter(reg -> reg.getStatus() == EventRegistration.RegistrationStatus.REGISTERED ||
                                   reg.getStatus() == EventRegistration.RegistrationStatus.ATTENDED)
                    .toList();
            
            String clubAdminEmail = event.getClub().getAdminUser().getEmail();
            
            log.info("Sending {} reminder emails for event: {}", registrations.size(), event.getTitle());
            
            for (EventRegistration registration : registrations) {
                try {
                    emailService.sendEventReminder(registration, clubAdminEmail);
                } catch (Exception e) {
                    log.error("Failed to send reminder to {}", registration.getUser().getEmail(), e);
                    // Continue with other registrations even if one fails
                }
            }
            
            log.info("Completed sending reminders for event: {}", event.getTitle());
            
        } catch (Exception e) {
            log.error("Error sending reminders for event: {}", event.getTitle(), e);
        }
    }
    
    /**
     * Manual trigger to send reminder for a specific event (can be called from API)
     */
    public void sendReminderForEvent(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));
        
        log.info("Manually triggering reminder for event: {}", event.getTitle());
        sendRemindersForEvent(event);
    }
}
