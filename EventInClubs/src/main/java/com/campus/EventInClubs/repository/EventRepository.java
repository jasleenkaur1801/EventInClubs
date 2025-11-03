package com.campus.EventInClubs.repository;

import com.campus.EventInClubs.domain.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {
    
    List<Event> findByClubId(Long clubId);
    
    List<Event> findByOrganizerId(Long organizerId);
    
    List<Event> findByStatus(Event.EventStatus status);
    
    List<Event> findByType(Event.EventType type);
    
    @Query("SELECT e FROM Event e WHERE e.startDate >= :startDate AND e.startDate <= :endDate")
    List<Event> findEventsBetweenDates(@Param("startDate") LocalDateTime startDate, 
                                       @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT e FROM Event e WHERE e.status = 'PUBLISHED' AND " +
           "(e.registrationDeadline IS NULL OR e.registrationDeadline > :now) AND " +
           "(e.maxParticipants IS NULL OR e.currentParticipants < e.maxParticipants)")
    List<Event> findOpenForRegistration(@Param("now") LocalDateTime now);
    
    @Query("SELECT e FROM Event e WHERE e.startDate > :now ORDER BY e.startDate ASC")
    List<Event> findUpcomingEvents(@Param("now") LocalDateTime now);
    
    @Query("SELECT e FROM Event e WHERE e.startDate <= :now AND " +
           "(e.endDate IS NULL OR e.endDate >= :now) AND e.status = 'ONGOING'")
    List<Event> findOngoingEvents(@Param("now") LocalDateTime now);
    
    @Query("SELECT e FROM Event e WHERE e.club.id = :clubId AND e.status IN :statuses")
    List<Event> findByClubIdAndStatusIn(@Param("clubId") Long clubId, 
                                        @Param("statuses") List<Event.EventStatus> statuses);
    
    @Query("SELECT e FROM Event e WHERE LOWER(e.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(e.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(e.tags) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Event> searchEvents(@Param("keyword") String keyword);
    
    @Query("SELECT COUNT(e) FROM Event e WHERE e.club.id = :clubId")
    Long countByClubId(@Param("clubId") Long clubId);
    
    @Query("SELECT COUNT(e) FROM Event e WHERE e.organizer.id = :organizerId")
    Long countByOrganizerId(@Param("organizerId") Long organizerId);
    
    @Query("SELECT e FROM Event e WHERE e.acceptsIdeas = true AND e.status = 'PUBLISHED' AND " +
           "(e.ideaSubmissionDeadline IS NULL OR e.ideaSubmissionDeadline > :now)")
    List<Event> findEventsAcceptingIdeas(@Param("now") LocalDateTime now);
    
    @Query("SELECT e FROM Event e WHERE e.ideaSubmissionDeadline IS NOT NULL AND " +
           "e.ideaSubmissionDeadline < :oneHourAgo AND e.isActive = true AND " +
           "e.status IN ('PUBLISHED', 'REGISTRATION_CLOSED', 'ONGOING')")
    List<Event> findExpiredEvents(@Param("oneHourAgo") LocalDateTime oneHourAgo);
    
    long countByIsActiveTrue();
    
    @Query("SELECT DISTINCT e FROM Event e " +
           "LEFT JOIN FETCH e.club " +
           "LEFT JOIN FETCH e.organizer " +
           "LEFT JOIN FETCH e.hall " +
           "LEFT JOIN FETCH e.approvedBy " +
           "WHERE (e.isActive IS NULL OR e.isActive = true) " +
           "AND (e.status = 'PUBLISHED' OR e.status = 'APPROVED') " +
           "AND (e.approvalStatus IS NULL OR e.approvalStatus = 'APPROVED') " +
           "AND e.startDate IS NOT NULL " +
           "AND e.endDate IS NOT NULL " +
           "AND e.endDate > :now")
    List<Event> findActiveEventsWithRelations(@Param("now") LocalDateTime now);
}
