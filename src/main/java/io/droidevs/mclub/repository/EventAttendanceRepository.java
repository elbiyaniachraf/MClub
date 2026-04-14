package io.droidevs.mclub.repository;

import io.droidevs.mclub.domain.EventAttendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventAttendanceRepository extends JpaRepository<EventAttendance, UUID> {
    Optional<EventAttendance> findByEventIdAndUserId(UUID eventId, UUID userId);

    @Query("select a from EventAttendance a join fetch a.user u join fetch a.event e where e.id = :eventId")
    List<EventAttendance> findByEventId(@Param("eventId") UUID eventId);

    // Alias with a clearer name for callers
    @Query("select a from EventAttendance a join fetch a.user u join fetch a.event e where e.id = :eventId")
    List<EventAttendance> findByEventIdWithUserAndEvent(@Param("eventId") UUID eventId);

    long countByEventId(UUID eventId);
}
