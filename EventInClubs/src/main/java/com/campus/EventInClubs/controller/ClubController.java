package com.campus.EventInClubs.controller;

import com.campus.EventInClubs.domain.model.Role;
import com.campus.EventInClubs.domain.model.User;
import com.campus.EventInClubs.dto.ClubDto;
import com.campus.EventInClubs.service.ClubService;
import com.campus.EventInClubs.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/clubs")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class ClubController {
    
    private final ClubService clubService;
    private final UserService userService;
    
    @GetMapping
    public ResponseEntity<List<ClubDto>> getAllActiveClubs() {
        try {
            List<ClubDto> clubs = clubService.getAllActiveClubs();
            log.info("Returning {} clubs to frontend", clubs.size());
            return ResponseEntity.ok(clubs);
        } catch (Exception e) {
            log.error("Error fetching all clubs", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/top")
    public ResponseEntity<List<ClubDto>> getTopClubs() {
        try {
            List<ClubDto> clubs = clubService.getTopClubs();
            return ResponseEntity.ok(clubs);
        } catch (Exception e) {
            log.error("Error fetching top clubs", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/category/{category}")
    public ResponseEntity<List<ClubDto>> getClubsByCategory(@PathVariable String category) {
        try {
            List<ClubDto> clubs = clubService.getClubsByCategory(category);
            return ResponseEntity.ok(clubs);
        } catch (Exception e) {
            log.error("Error fetching clubs by category: {}", category, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/top/category/{category}")
    public ResponseEntity<List<ClubDto>> getTopClubsByCategory(@PathVariable String category) {
        try {
            List<ClubDto> clubs = clubService.getTopClubsByCategory(category);
            return ResponseEntity.ok(clubs);
        } catch (Exception e) {
            log.error("Error fetching top clubs by category: {}", category, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/search")
    public ResponseEntity<List<ClubDto>> searchClubs(@RequestParam String q) {
        try {
            if (q == null || q.trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            
            List<ClubDto> clubs = clubService.searchClubs(q.trim());
            return ResponseEntity.ok(clubs);
        } catch (Exception e) {
            log.error("Error searching clubs with query: {}", q, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ClubDto> getClubById(@PathVariable Long id) {
        try {
            return clubService.getClubById(id)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error fetching club with id: {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/name/{name}")
    public ResponseEntity<ClubDto> getClubByName(@PathVariable String name) {
        try {
            return clubService.getClubByName(name)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error fetching club with name: {}", name, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @PostMapping
    public ResponseEntity<?> createClub(@RequestBody ClubDto clubDto, @RequestParam(required = false) Long adminUserId) {
        try {
            // For now, allow creation without authentication
            // TODO: Re-enable authentication once JWT is working
            
            if (clubDto.getName() == null || clubDto.getName().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Club name is required"));
            }
            
            // Use provided adminUserId or default to 1 (first CLUB_ADMIN user)
            Long userId = adminUserId != null ? adminUserId : 1L;
            
            ClubDto createdClub = clubService.createClub(clubDto, userId);
            return ResponseEntity.ok(createdClub);
        } catch (RuntimeException e) {
            log.error("Error creating club: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error creating club", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Internal server error"));
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<ClubDto> updateClub(@PathVariable Long id, @RequestBody ClubDto clubDto, @RequestParam Long userId) {
        try {
            ClubDto updatedClub = clubService.updateClub(id, clubDto, userId);
            return ResponseEntity.ok(updatedClub);
        } catch (RuntimeException e) {
            log.error("Error updating club: {}", e.getMessage());
            return ResponseEntity.badRequest().body(null);
        } catch (Exception e) {
            log.error("Error updating club with id: {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteClub(@PathVariable Long id, @RequestParam Long userId) {
        try {
            clubService.deleteClub(id, userId);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            log.error("Error deleting club: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error deleting club with id: {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/categories")
    public ResponseEntity<List<String>> getAvailableCategories() {
        try {
            // This would typically come from a configuration or be dynamically generated
            List<String> categories = List.of(
                "Technology", "Design", "Engineering", "Arts", "Sports", 
                "Business", "Science", "Literature", "Music", "Dance",
                "Photography", "Gaming", "Environment", "Health", "Education"
            );
            return ResponseEntity.ok(categories);
        } catch (Exception e) {
            log.error("Error fetching categories", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/admin/{adminUserId}")
    public ResponseEntity<List<ClubDto>> getClubsByAdminUser(@PathVariable Long adminUserId) {
        try {
            List<ClubDto> clubs = clubService.getClubsByAdminUser(adminUserId);
            log.info("Returning {} clubs for admin user {}", clubs.size(), adminUserId);
            return ResponseEntity.ok(clubs);
        } catch (Exception e) {
            log.error("Error fetching clubs for admin user: {}", adminUserId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    // Super Admin endpoints for club approval
    @GetMapping("/pending")
    public ResponseEntity<List<ClubDto>> getPendingClubs() {
        try {
            List<ClubDto> pendingClubs = clubService.getPendingClubs();
            return ResponseEntity.ok(pendingClubs);
        } catch (Exception e) {
            log.error("Error fetching pending clubs", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @PostMapping("/{id}/approve")
    public ResponseEntity<ClubDto> approveClub(@PathVariable Long id) {
        try {
            ClubDto approvedClub = clubService.approveClub(id);
            return ResponseEntity.ok(approvedClub);
        } catch (RuntimeException e) {
            log.error("Error approving club: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error approving club with id: {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @PostMapping("/{id}/reject")
    public ResponseEntity<ClubDto> rejectClub(@PathVariable Long id) {
        try {
            ClubDto rejectedClub = clubService.rejectClub(id);
            return ResponseEntity.ok(rejectedClub);
        } catch (RuntimeException e) {
            log.error("Error rejecting club: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error rejecting club with id: {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
