package com.campus.EventInClubs.repository;

import com.campus.EventInClubs.domain.model.TeamRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TeamRegistrationRepository extends JpaRepository<TeamRegistration, Long> {
    
    List<TeamRegistration> findByEventId(Long eventId);
    
    List<TeamRegistration> findByRegisteredById(Long userId);
    
    @Query("SELECT tr FROM TeamRegistration tr WHERE tr.event.id = :eventId AND tr.memberRollNumbers LIKE %:rollNumber%")
    List<TeamRegistration> findByEventIdAndRollNumberContaining(@Param("eventId") Long eventId, @Param("rollNumber") String rollNumber);
    
    @Query("SELECT COUNT(tr) FROM TeamRegistration tr WHERE tr.event.id = :eventId")
    Long countTeamsByEventId(@Param("eventId") Long eventId);
    
    // Sum total team members for an event (only REGISTERED teams)
    @Query("SELECT COALESCE(SUM(tr.teamSize), 0) FROM TeamRegistration tr WHERE tr.event.id = :eventId AND tr.status = 'REGISTERED'")
    Long sumTeamMembersByEventId(@Param("eventId") Long eventId);
}
