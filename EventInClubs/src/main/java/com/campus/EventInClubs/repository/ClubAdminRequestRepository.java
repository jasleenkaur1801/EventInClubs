package com.campus.EventInClubs.repository;

import com.campus.EventInClubs.domain.model.ClubAdminRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClubAdminRequestRepository extends JpaRepository<ClubAdminRequest, Long> {
    
    Optional<ClubAdminRequest> findByEmail(String email);
    
    boolean existsByEmail(String email);
    
    List<ClubAdminRequest> findByStatus(ClubAdminRequest.ClubAdminRequestStatus status);
    
    List<ClubAdminRequest> findAllByOrderByRequestedAtDesc();
}
