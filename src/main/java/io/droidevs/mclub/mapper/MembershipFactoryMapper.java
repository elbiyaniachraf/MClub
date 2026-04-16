package io.droidevs.mclub.mapper;

import io.droidevs.mclub.domain.Membership;
import io.droidevs.mclub.domain.MembershipStatus;
import io.droidevs.mclub.domain.ClubRole;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Factory-style mapper for creating Membership entities with controlled defaults.
 * Relationships (user/club) are set by the service.
 */
@Mapper(componentModel = "spring", config = CentralMapperConfig.class)
public interface MembershipFactoryMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "club", ignore = true)
    @Mapping(target = "joinedAt", ignore = true)
    Membership create(ClubRole role, MembershipStatus status);
}

