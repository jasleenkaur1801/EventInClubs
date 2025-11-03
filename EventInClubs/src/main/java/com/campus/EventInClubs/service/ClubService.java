package com.campus.EventInClubs.service;

import com.campus.EventInClubs.domain.model.ApprovalStatus;
import com.campus.EventInClubs.domain.model.Club;
import com.campus.EventInClubs.domain.model.User;
import com.campus.EventInClubs.dto.ClubDto;

import java.util.ArrayList;
import com.campus.EventInClubs.repository.ClubRepository;
import com.campus.EventInClubs.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ClubService {
    
    private final ClubRepository clubRepository;
    private final UserRepository userRepository;
    
    public List<ClubDto> getAllActiveClubs() {
        try {
            // Only return clubs that are active AND approved
            List<Club> clubs = clubRepository.findByIsActiveTrueAndApprovalStatus(ApprovalStatus.APPROVED);
            log.info("Found {} active and approved clubs", clubs.size());
            return clubs.stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error fetching active clubs", e);
            return new ArrayList<>();
        }
    }
    
    public List<ClubDto> getPendingClubs() {
        return clubRepository.findByApprovalStatus(ApprovalStatus.PENDING)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    public ClubDto approveClub(Long clubId) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new RuntimeException("Club not found with id: " + clubId));
        
        club.setApprovalStatus(ApprovalStatus.APPROVED);
        club.setIsActive(true);
        Club savedClub = clubRepository.save(club);
        return convertToDto(savedClub);
    }
    
    public ClubDto rejectClub(Long clubId) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new RuntimeException("Club not found with id: " + clubId));
        
        club.setApprovalStatus(ApprovalStatus.REJECTED);
        club.setIsActive(false);
        Club savedClub = clubRepository.save(club);
        return convertToDto(savedClub);
    }
    
    public List<ClubDto> getClubsByCategory(String category) {
        List<Club> clubs = clubRepository.findByCategory(category);
        return clubs.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    public List<ClubDto> getTopClubs() {
        List<Club> clubs = clubRepository.findTopClubs();
        return clubs.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    public List<ClubDto> getTopClubsByCategory(String category) {
        List<Club> clubs = clubRepository.findTopClubsByCategory(category);
        return clubs.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    public List<ClubDto> searchClubs(String searchTerm) {
        List<Club> clubs = clubRepository.searchActiveClubs(searchTerm);
        return clubs.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    public Optional<ClubDto> getClubById(Long id) {
        return clubRepository.findById(id)
                .map(this::convertToDto);
    }
    
    public List<ClubDto> getClubsByAdminUser(Long adminUserId) {
        List<Club> clubs = clubRepository.findByAdminUserId(adminUserId);
        log.info("Found {} clubs for admin user {}", clubs.size(), adminUserId);
        return clubs.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    public Optional<ClubDto> getClubByName(String name) {
        return clubRepository.findByName(name)
                .map(this::convertToDto);
    }
    
    public ClubDto createClub(ClubDto clubDto, Long adminUserId) {
        // Check if user exists
        User adminUser = userRepository.findById(adminUserId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + adminUserId));
        
        // Prevent Super Admin from being assigned as club admin
        if (adminUser.getRole() == com.campus.EventInClubs.domain.model.Role.SUPER_ADMIN) {
            throw new RuntimeException("Super Admin cannot be assigned as club admin. Only regular users can manage clubs.");
        }
        
        // Check if club name already exists
        if (clubRepository.existsByName(clubDto.getName())) {
            throw new RuntimeException("Club with this name already exists");
        }
        
        if (clubDto.getShortName() != null && clubRepository.existsByShortName(clubDto.getShortName())) {
            throw new RuntimeException("Club with this short name already exists");
        }
        
        // Create club with APPROVED status - auto-approved
        Club club = Club.builder()
                .name(clubDto.getName())
                .description(clubDto.getDescription())
                .category(clubDto.getCategory())
                .shortName(clubDto.getShortName())
                .memberCount(clubDto.getMemberCount() != null ? clubDto.getMemberCount() : 0)
                .eventCount(clubDto.getEventCount() != null ? clubDto.getEventCount() : 0)
                .rating(clubDto.getRating() != null ? clubDto.getRating() : 0.0)
                .logoUrl(clubDto.getLogoUrl())
                .adminUser(adminUser)
                .isActive(true) // Active immediately
                .approvalStatus(ApprovalStatus.APPROVED) // Auto-approved
                .build();
        
        Club savedClub = clubRepository.save(club);
        log.info("Created and auto-approved new club: {} (Admin: {})", savedClub.getName(), adminUser.getName());
        
        return convertToDto(savedClub);
    }
    
    public ClubDto updateClub(Long id, ClubDto clubDto, Long userId) {
        Club club = clubRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Club not found"));
        
        // Check if user is admin of the club or super admin
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (!club.getAdminUser().getId().equals(userId) && 
            user.getRole() != com.campus.EventInClubs.domain.model.Role.SUPER_ADMIN) {
            throw new RuntimeException("User does not have permission to update this club");
        }
        
        // Update fields
        if (clubDto.getName() != null && !clubDto.getName().equals(club.getName())) {
            if (clubRepository.existsByName(clubDto.getName())) {
                throw new RuntimeException("Club with this name already exists");
            }
            club.setName(clubDto.getName());
        }
        
        if (clubDto.getDescription() != null) {
            club.setDescription(clubDto.getDescription());
        }
        
        if (clubDto.getCategory() != null) {
            club.setCategory(clubDto.getCategory());
        }
        
        if (clubDto.getShortName() != null && !clubDto.getShortName().equals(club.getShortName())) {
            if (clubRepository.existsByShortName(clubDto.getShortName())) {
                throw new RuntimeException("Club with this short name already exists");
            }
            club.setShortName(clubDto.getShortName());
        }
        
        if (clubDto.getLogoUrl() != null) {
            club.setLogoUrl(clubDto.getLogoUrl());
            log.info("Updated logo for club: {}", club.getName());
        }
        
        Club updatedClub = clubRepository.save(club);
        log.info("Updated club: {}", updatedClub.getName());
        
        return convertToDto(updatedClub);
    }
    
    public void deleteClub(Long id, Long userId) {
        Club club = clubRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Club not found"));
        
        // Check if user is admin of the club or super admin
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (!club.getAdminUser().getId().equals(userId) && 
            user.getRole() != com.campus.EventInClubs.domain.model.Role.SUPER_ADMIN) {
            throw new RuntimeException("User does not have permission to delete this club");
        }
        
        // Soft delete - set as inactive
        club.setIsActive(false);
        clubRepository.save(club);
        log.info("Deactivated club: {}", club.getName());
    }
    
    public void updateClubStats(Long clubId) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new RuntimeException("Club not found"));
        
        // This method would be called after member count or event count changes
        // For now, we'll just save the club to trigger the @PreUpdate
        clubRepository.save(club);
    }
    
    private ClubDto convertToDto(Club club) {
        return ClubDto.builder()
                .id(club.getId())
                .name(club.getName())
                .description(club.getDescription())
                .category(club.getCategory())
                .shortName(club.getShortName())
                .memberCount(club.getMemberCount())
                .eventCount(club.getEventCount())
                .rating(club.getRating())
                .logoUrl(club.getLogoUrl())
                .adminUserId(club.getAdminUser() != null ? club.getAdminUser().getId() : null)
                .adminUserName(club.getAdminUser() != null ? club.getAdminUser().getName() : null)
                .approvalStatus(club.getApprovalStatus() != null ? club.getApprovalStatus().toString() : null)
                .isActive(club.getIsActive())
                .createdAt(club.getCreatedAt())
                .updatedAt(club.getUpdatedAt())
                .build();
    }
}
