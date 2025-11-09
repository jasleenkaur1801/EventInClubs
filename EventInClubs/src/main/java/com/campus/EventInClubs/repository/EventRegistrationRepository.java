package com.campus.EventInClubs.repository;

import com.campus.EventInClubs.domain.model.EventRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventRegistrationRepository extends JpaRepository<EventRegistration, Long> {
    
    // Find all registrations for a specific event
    List<EventRegistration> findByEventIdOrderByRegisteredAtDesc(Long eventId);
    
    // Find registration by event and user
    Optional<EventRegistration> findByEventIdAndUserId(Long eventId, Long userId);
    
    // Count registrations contributing to capacity (REGISTERED + ATTENDED + NO_SHOW)
    @Query("SELECT COUNT(er) FROM EventRegistration er WHERE er.event.id = :eventId AND er.status IN ('REGISTERED', 'ATTENDED', 'NO_SHOW')")
    Long countActiveByEventId(@Param("eventId") Long eventId);
    
    // Count registrations by status for an event
    @Query("SELECT COUNT(er) FROM EventRegistration er WHERE er.event.id = :eventId AND er.status = :status")
    Long countByEventIdAndStatus(@Param("eventId") Long eventId, @Param("status") EventRegistration.RegistrationStatus status);
    
    // Find all registrations for a user
    List<EventRegistration> findByUserIdOrderByRegisteredAtDesc(Long userId);
    
    // Check if user is registered for an event (any status)
    boolean existsByEventIdAndUserId(Long eventId, Long userId);
    
    // Check if user is registered for an event with specific status
    boolean existsByEventIdAndUserIdAndStatus(Long eventId, Long userId, EventRegistration.RegistrationStatus status);
    
    // Get registrations with payment status
    @Query("SELECT er FROM EventRegistration er WHERE er.event.id = :eventId AND er.paymentStatus = :paymentStatus")
    List<EventRegistration> findByEventIdAndPaymentStatus(@Param("eventId") Long eventId, @Param("paymentStatus") EventRegistration.PaymentStatus paymentStatus);
    
    // Find registration by event and user email
    @Query("SELECT er FROM EventRegistration er INNER JOIN er.user u WHERE er.event.id = :eventId AND LOWER(u.email) = LOWER(:email)")
    Optional<EventRegistration> findByEventIdAndUserEmail(@Param("eventId") Long eventId, @Param("email") String email);
    
    // Check if an email is already registered for an event (any status except CANCELLED)
    @Query("SELECT CASE WHEN COUNT(er) > 0 THEN true ELSE false END FROM EventRegistration er INNER JOIN er.user u WHERE er.event.id = :eventId AND LOWER(u.email) = LOWER(:email) AND er.status != 'CANCELLED'")
    boolean existsByEventIdAndUserEmail(@Param("eventId") Long eventId, @Param("email") String email);
}
