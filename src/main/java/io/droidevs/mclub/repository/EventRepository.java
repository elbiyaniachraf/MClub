package io.droidevs.mclub.repository;

import io.droidevs.mclub.domain.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventRepository extends JpaRepository<Event, UUID> {
    List<Event> findByClubId(UUID clubId);

    List<Event> findTop5ByClubIdOrderByStartDateDesc(UUID clubId);

    @Query("select e from Event e join fetch e.club where e.id = :id")
    Optional<Event> findByIdWithClub(@Param("id") UUID id);
}
