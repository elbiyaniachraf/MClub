package io.droidevs.mclub.repository;
import io.droidevs.mclub.domain.Membership;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
public interface MembershipRepository extends JpaRepository<Membership, UUID> {
    List<Membership> findByClubId(UUID clubId);
    Optional<Membership> findByUserIdAndClubId(UUID userId, UUID clubId);

    // For club admin UI pages
    List<Membership> findByClubIdAndStatus(UUID clubId, io.droidevs.mclub.domain.MembershipStatus status);
}
