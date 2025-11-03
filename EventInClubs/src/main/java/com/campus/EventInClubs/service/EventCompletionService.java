package com.campus.EventInClubs.service;

import com.campus.EventInClubs.domain.model.Event;
import com.campus.EventInClubs.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventCompletionService {
    
    private final EventRepository eventRepository;
    private final EventService eventService;
    
    /**
     * Runs every hour to check and mark completed events
     * Automatically increments club eventCount through EventService.updateEventStatus()
     */
    @Scheduled(fixedRate = 3600000) // Run every hour (3600000 milliseconds)
    @Transactional
    public void checkAndCompleteEvents() {
        log.info("Running event completion check...");
        
        LocalDateTime now = LocalDateTime.now();
        
        // Find events that should be marked as completed
        List<Event> eventsToComplete = eventRepository.findAll().stream()
                .filter(event -> event.getEndDate() != null)
                .filter(event -> event.getEndDate().isBefore(now))
                .filter(event -> event.getStatus() != Event.EventStatus.COMPLETED)
                .filter(event -> event.getStatus() != Event.EventStatus.CANCELLED)
                .filter(event -> event.getIsActive() != null && event.getIsActive())
                .toList();
        
        if (!eventsToComplete.isEmpty()) {
            log.info("Found {} events to mark as completed", eventsToComplete.size());
            
            for (Event event : eventsToComplete) {
                try {
                    // Use EventService.updateEventStatus to ensure eventCount is incremented
                    eventService.updateEventStatus(event.getId(), "COMPLETED");
                    log.info("Marked event '{}' (ID: {}) as COMPLETED", event.getTitle(), event.getId());
                } catch (Exception e) {
                    log.error("Error marking event {} as completed: {}", event.getId(), e.getMessage());
                }
            }
        } else {
            log.info("No events need to be marked as completed at this time");
        }
    }
}
