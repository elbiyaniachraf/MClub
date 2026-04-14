package io.droidevs.mclub.repository;
import io.droidevs.mclub.domain.Activity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;
public interface ActivityRepository extends JpaRepository<Activity, UUID> {
    List<Activity> findByClubId(UUID clubId);

    List<Activity> findTop5ByClubIdOrderByDateDesc(UUID clubId);
}
