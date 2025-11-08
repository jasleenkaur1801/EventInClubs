package com.campus.EventInClubs.service;

import com.campus.EventInClubs.domain.model.Notification;
import com.campus.EventInClubs.domain.model.User;
import com.campus.EventInClubs.dto.NotificationDto;
import com.campus.EventInClubs.repository.NotificationRepository;
import com.campus.EventInClubs.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {
    
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    
    public List<NotificationDto> getUserNotifications(Long userId) {
        List<Notification> notifications = notificationRepository.findByUserIdAndIsActiveTrueOrderByCreatedAtDesc(userId);
        return notifications.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    public List<NotificationDto> getUnreadNotifications(Long userId) {
        List<Notification> notifications = notificationRepository.findByUserIdAndIsReadFalseAndIsActiveTrueOrderByCreatedAtDesc(userId);
        return notifications.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    public long getUnreadCount(Long userId) {
        return notificationRepository.countUnreadByUserId(userId);
    }
    
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public NotificationDto createNotification(Long userId, String title, String message, 
                                            Notification.NotificationType type, 
                                            Long relatedEntityId, String relatedEntityType) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            Notification notification = Notification.builder()
                    .title(title)
                    .message(message)
                    .type(type)
                    .user(user)
                    .relatedEntityId(relatedEntityId)
                    .relatedEntityType(relatedEntityType)
                    .isRead(false)
                    .isActive(true)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
            
            Notification saved = notificationRepository.save(notification);
            log.info("Created notification for user {}: {}", userId, title);
            
            return convertToDto(saved);
        } catch (Exception e) {
            log.error("Failed to create notification for user {}: {}", userId, title, e);
            throw e;
        }
    }
    
    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        
        if (!notification.getUser().getId().equals(userId)) {
            throw new RuntimeException("Cannot mark other user's notifications as read");
        }
        
        notification.setIsRead(true);
        notification.setReadAt(Instant.now());
        notification.setUpdatedAt(Instant.now());
        
        notificationRepository.save(notification);
        log.info("Marked notification {} as read for user {}", notificationId, userId);
    }
    
    @Transactional
    public void markAllAsRead(Long userId) {
        List<Notification> unreadNotifications = notificationRepository.findByUserIdAndIsReadFalseAndIsActiveTrueOrderByCreatedAtDesc(userId);
        
        for (Notification notification : unreadNotifications) {
            notification.setIsRead(true);
            notification.setReadAt(Instant.now());
            notification.setUpdatedAt(Instant.now());
        }
        
        notificationRepository.saveAll(unreadNotifications);
        log.info("Marked {} notifications as read for user {}", unreadNotifications.size(), userId);
    }
    
    @Transactional
    public void deleteNotification(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        
        if (!notification.getUser().getId().equals(userId)) {
            throw new RuntimeException("Cannot delete other user's notifications");
        }
        
        notification.setIsActive(false);
        notification.setUpdatedAt(Instant.now());
        
        notificationRepository.save(notification);
        log.info("Deleted notification {} for user {}", notificationId, userId);
    }
    
    // Helper methods for creating specific notification types
    public void notifyNewIdea(Long problemOwnerId, String ideaTitle, Long ideaId) {
        createNotification(
            problemOwnerId,
            "New Idea Submitted",
            "A new idea '" + ideaTitle + "' has been submitted to your problem.",
            Notification.NotificationType.NEW_IDEA,
            ideaId,
            "IDEA"
        );
    }
    
    public void notifyIdeaVoted(Long ideaOwnerId, String voterName, String voteType, Long ideaId) {
        createNotification(
            ideaOwnerId,
            "Your Idea Received a Vote",
            voterName + " " + (voteType.equals("UP") ? "upvoted" : "downvoted") + " your idea.",
            Notification.NotificationType.IDEA_VOTED,
            ideaId,
            "IDEA"
        );
    }
    
    public void notifyIdeaCommented(Long ideaOwnerId, String commenterName, Long ideaId) {
        createNotification(
            ideaOwnerId,
            "New Comment on Your Idea",
            commenterName + " commented on your idea.",
            Notification.NotificationType.IDEA_COMMENTED,
            ideaId,
            "IDEA"
        );
    }
    
    public void notifyIdeaStatusChanged(Long userId, String newStatus, Long ideaId) {
        createNotification(
            userId,
            "Idea Status Updated",
            "Your idea status has been changed to " + newStatus + ".",
            Notification.NotificationType.IDEA_STATUS_CHANGED,
            ideaId,
            "IDEA"
        );
    }
    
    public void notifyNewEventIdea(Long userId, String ideaTitle, Long eventId) {
        createNotification(
            userId,
            "New Event Idea Submitted",
            "A new idea '" + ideaTitle + "' has been submitted for your event.",
            Notification.NotificationType.NEW_IDEA,
            eventId,
            "EVENT"
        );
    }
    
    private NotificationDto convertToDto(Notification notification) {
        return NotificationDto.builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .type(notification.getType().name())
                .userId(notification.getUser().getId())
                .relatedEntityId(notification.getRelatedEntityId())
                .relatedEntityType(notification.getRelatedEntityType())
                .isRead(notification.getIsRead())
                .readAt(notification.getReadAt())
                .createdAt(notification.getCreatedAt())
                .updatedAt(notification.getUpdatedAt())
                .build();
    }
}
