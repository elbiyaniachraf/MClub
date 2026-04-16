package io.droidevs.mclub.mapper;

import io.droidevs.mclub.domain.Membership;
import io.droidevs.mclub.dto.MembershipDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", config = CentralMapperConfig.class)
public interface MembershipMapper {
    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "club.id", target = "clubId")
    @Mapping(source = "user.fullName", target = "userFullName")
    @Mapping(source = "user.email", target = "userEmail")
    MembershipDto toDto(Membership m);
}
