package com.campus.EventInClubs.controller;

import com.campus.EventInClubs.dto.TeamRegistrationDto;
import com.campus.EventInClubs.service.TeamRegistrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/team-registrations")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class TeamRegistrationController {
    
    private final TeamRegistrationService teamRegistrationService;
    
    @PostMapping("/register")
    public ResponseEntity<?> registerTeam(
            @RequestParam Long eventId,
            @RequestParam Long userId,
            @RequestParam String teamName,
            @RequestParam List<String> memberRollNumbers,
            @RequestParam(required = false) String notes) {
        try {
            log.info("Team registration request - Event: {}, User: {}, Team: {}, Members: {}", 
                    eventId, userId, teamName, memberRollNumbers.size());
            
            TeamRegistrationDto registration = teamRegistrationService.registerTeam(
                    eventId, userId, teamName, memberRollNumbers, notes);
            
            return ResponseEntity.ok(registration);
        } catch (RuntimeException e) {
            log.error("Error registering team: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error registering team", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Internal server error"));
        }
    }
    
    @GetMapping("/event/{eventId}")
    public ResponseEntity<List<TeamRegistrationDto>> getTeamsByEvent(@PathVariable Long eventId) {
        try {
            List<TeamRegistrationDto> teams = teamRegistrationService.getTeamsByEvent(eventId);
            return ResponseEntity.ok(teams);
        } catch (Exception e) {
            log.error("Error fetching teams for event: {}", eventId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<TeamRegistrationDto>> getTeamsByUser(@PathVariable Long userId) {
        try {
            List<TeamRegistrationDto> teams = teamRegistrationService.getTeamsByUser(userId);
            return ResponseEntity.ok(teams);
        } catch (Exception e) {
            log.error("Error fetching teams for user: {}", userId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/{teamId}")
    public ResponseEntity<?> getTeamById(@PathVariable Long teamId) {
        try {
            TeamRegistrationDto team = teamRegistrationService.getTeamById(teamId);
            return ResponseEntity.ok(team);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error fetching team: {}", teamId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @DeleteMapping("/{teamId}")
    public ResponseEntity<?> cancelTeamRegistration(
            @PathVariable Long teamId,
            @RequestParam Long userId) {
        try {
            teamRegistrationService.cancelTeamRegistration(teamId, userId);
            return ResponseEntity.ok(Map.of("message", "Team registration cancelled successfully"));
        } catch (RuntimeException e) {
            log.error("Error cancelling team registration: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error cancelling team registration", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Internal server error"));
        }
    }
}
