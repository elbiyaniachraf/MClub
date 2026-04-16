package io.droidevs.mclub.mapper;

import io.droidevs.mclub.domain.Membership;
import io.droidevs.mclub.dto.MembershipDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", config = CentralMapperConfig.class)
public interface MembershipEntityMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "club", ignore = true)
    @Mapping(target = "joinedAt", ignore = true)
    Membership toEntity(MembershipDto dto);
}

