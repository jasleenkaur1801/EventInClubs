package com.campus.EventInClubs.service;

import com.campus.EventInClubs.domain.model.Event;
import com.campus.EventInClubs.domain.model.TeamRegistration;
import com.campus.EventInClubs.domain.model.User;
import com.campus.EventInClubs.dto.TeamRegistrationDto;
import com.campus.EventInClubs.repository.EventRepository;
import com.campus.EventInClubs.repository.TeamRegistrationRepository;
import com.campus.EventInClubs.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TeamRegistrationService {
    
    private final TeamRegistrationRepository teamRegistrationRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    
    public TeamRegistrationDto registerTeam(Long eventId, Long userId, String teamName, 
                                           List<String> memberRollNumbers, List<String> memberNames, 
                                           List<String> memberEmails, String notes) {
        // Validate event exists and is a team event
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));
        
        if (!event.getIsTeamEvent()) {
            throw new RuntimeException("This event is not a team event");
        }
        
        // Check if user has already registered a team for this event
        if (teamRegistrationRepository.existsByEventIdAndRegisteredById(eventId, userId)) {
            throw new RuntimeException("You have already registered a team for this event");
        }
        
        // Validate team size
        int teamSize = memberRollNumbers.size();
        if (event.getMinTeamMembers() != null && teamSize < event.getMinTeamMembers()) {
            throw new RuntimeException("Team size must be at least " + event.getMinTeamMembers() + " members");
        }
        if (event.getMaxTeamMembers() != null && teamSize > event.getMaxTeamMembers()) {
            throw new RuntimeException("Team size cannot exceed " + event.getMaxTeamMembers() + " members");
        }
        
        // Validate that no roll number is already registered in another team for this event
        for (String rollNumber : memberRollNumbers) {
            List<TeamRegistration> existingRegistrations = 
                teamRegistrationRepository.findByEventIdAndRollNumberContaining(eventId, rollNumber);
            
            if (!existingRegistrations.isEmpty()) {
                throw new RuntimeException("Roll number " + rollNumber + " is already registered in another team");
            }
        }
        
        // Get user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Create team registration
        String rollNumbersStr = String.join(",", memberRollNumbers);
        String namesStr = (memberNames != null && !memberNames.isEmpty()) ? String.join(",", memberNames) : null;
        String emailsStr = (memberEmails != null && !memberEmails.isEmpty()) ? String.join(",", memberEmails) : null;
        
        TeamRegistration teamRegistration = TeamRegistration.builder()
                .event(event)
                .teamName(teamName)
                .teamSize(teamSize)
                .memberRollNumbers(rollNumbersStr)
                .memberNames(namesStr)
                .memberEmails(emailsStr)
                .registeredBy(user)
                .status(TeamRegistration.RegistrationStatus.REGISTERED)
                .registrationNotes(notes)
                .paymentStatus(event.getRegistrationFee() == 0 ? 
                    TeamRegistration.PaymentStatus.NOT_REQUIRED : 
                    TeamRegistration.PaymentStatus.PENDING)
                .build();
        
        TeamRegistration saved = teamRegistrationRepository.save(teamRegistration);
        log.info("Team '{}' registered for event '{}' with {} members", 
                teamName, event.getTitle(), teamSize);
        
        // Send notification to user (team leader)
        notificationService.createNotification(
            userId,
            "Team Registration Confirmed",
            String.format("Your team '%s' has been successfully registered for '%s'", teamName, event.getTitle()),
            com.campus.EventInClubs.domain.model.Notification.NotificationType.SYSTEM,
            event.getId(),
            "EVENT"
        );
        
        // Send notification to club admin
        notificationService.createNotification(
            event.getClub().getAdminUser().getId(),
            "New Team Registration",
            String.format("Team '%s' registered for '%s' with %d members", teamName, event.getTitle(), teamSize),
            com.campus.EventInClubs.domain.model.Notification.NotificationType.SYSTEM,
            event.getId(),
            "EVENT"
        );
        
        return convertToDto(saved);
    }
    
    public List<TeamRegistrationDto> getTeamsByEvent(Long eventId) {
        return teamRegistrationRepository.findByEventId(eventId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    public List<TeamRegistrationDto> getTeamsByUser(Long userId) {
        return teamRegistrationRepository.findByRegisteredById(userId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    public TeamRegistrationDto getTeamById(Long teamId) {
        TeamRegistration team = teamRegistrationRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team registration not found"));
        return convertToDto(team);
    }
    
    public void cancelTeamRegistration(Long teamId, Long userId) {
        TeamRegistration team = teamRegistrationRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team registration not found"));
        
        // Only the person who registered can cancel
        if (!team.getRegisteredBy().getId().equals(userId)) {
            throw new RuntimeException("Only the team leader can cancel the registration");
        }
        
        team.setStatus(TeamRegistration.RegistrationStatus.CANCELLED);
        teamRegistrationRepository.save(team);
        
        log.info("Team '{}' registration cancelled for event '{}'", 
                team.getTeamName(), team.getEvent().getTitle());
    }
    
    private TeamRegistrationDto convertToDto(TeamRegistration team) {
        List<String> rollNumbers = Arrays.asList(team.getMemberRollNumbers().split(","));
        List<String> names = (team.getMemberNames() != null && !team.getMemberNames().isEmpty()) 
                ? Arrays.asList(team.getMemberNames().split(",")) 
                : null;
        List<String> emails = (team.getMemberEmails() != null && !team.getMemberEmails().isEmpty()) 
                ? Arrays.asList(team.getMemberEmails().split(",")) 
                : null;
        
        return TeamRegistrationDto.builder()
                .id(team.getId())
                .eventId(team.getEvent().getId())
                .eventTitle(team.getEvent().getTitle())
                .teamName(team.getTeamName())
                .teamSize(team.getTeamSize())
                .memberRollNumbers(rollNumbers)
                .memberNames(names)
                .memberEmails(emails)
                .registeredById(team.getRegisteredBy().getId())
                .registeredByName(team.getRegisteredBy().getName())
                .registeredByEmail(team.getRegisteredBy().getEmail())
                .status(team.getStatus().name())
                .registrationNotes(team.getRegistrationNotes())
                .paymentStatus(team.getPaymentStatus().name())
                .registeredAt(team.getRegisteredAt())
                .build();
    }
}
