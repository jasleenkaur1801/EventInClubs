package com.campus.EventInClubs.controller;

import com.campus.EventInClubs.domain.model.ClubAdminRequest;
import com.campus.EventInClubs.domain.model.User;
import com.campus.EventInClubs.service.ClubAdminRequestService;
import com.campus.EventInClubs.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/club-admin-requests")
@RequiredArgsConstructor
@CrossOrigin(origins = {
    "http://localhost:5173",
    "http://localhost:3000",
    "https://event-manager-mauve.vercel.app",
    "http://event-manager-mauve.vercel.app"
})
public class ClubAdminRequestController {

    private final ClubAdminRequestService clubAdminRequestService;
    private final UserService userService;

    // Get all club admin requests (Super Admin only)
    @GetMapping
    public ResponseEntity<?> getAllRequests(Authentication authentication) {
        try {
            User superAdmin = userService.findByEmail(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            List<ClubAdminRequest> requests = clubAdminRequestService.getAllRequests();
            
            List<Map<String, Object>> response = requests.stream()
                    .map(req -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", req.getId());
                        map.put("name", req.getName());
                        map.put("email", req.getEmail());
                        map.put("status", req.getStatus().name());
                        map.put("requestedAt", req.getRequestedAt().toString());
                        map.put("approvedAt", req.getApprovedAt() != null ? req.getApprovedAt().toString() : "");
                        map.put("rejectionReason", req.getRejectionReason() != null ? req.getRejectionReason() : "");
                        return map;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // Get pending club admin requests (Super Admin only)
    @GetMapping("/pending")
    public ResponseEntity<?> getPendingRequests(Authentication authentication) {
        try {
            User superAdmin = userService.findByEmail(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            List<ClubAdminRequest> requests = clubAdminRequestService.getPendingRequests();
            
            List<Map<String, Object>> response = requests.stream()
                    .map(req -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", req.getId());
                        map.put("name", req.getName());
                        map.put("email", req.getEmail());
                        map.put("status", req.getStatus().name());
                        map.put("requestedAt", req.getRequestedAt().toString());
                        return map;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // Approve a club admin request (Super Admin only)
    @PostMapping("/{requestId}/approve")
    public ResponseEntity<?> approveRequest(
            @PathVariable Long requestId,
            @RequestBody(required = false) Map<String, Long> payload,
            Authentication authentication) {
        try {
            User superAdmin = userService.findByEmail(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Club ID is optional - can be null
            Long clubId = (payload != null) ? payload.get("clubId") : null;

            clubAdminRequestService.approveRequest(requestId, superAdmin.getId(), clubId);

            return ResponseEntity.ok(Map.of("message", "Club admin request approved successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // Reject a club admin request (Super Admin only)
    @PostMapping("/{requestId}/reject")
    public ResponseEntity<?> rejectRequest(
            @PathVariable Long requestId,
            @RequestBody Map<String, String> payload,
            Authentication authentication) {
        try {
            User superAdmin = userService.findByEmail(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            String reason = payload.getOrDefault("reason", "No reason provided");

            clubAdminRequestService.rejectRequest(requestId, superAdmin.getId(), reason);

            return ResponseEntity.ok(Map.of("message", "Club admin request rejected"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
