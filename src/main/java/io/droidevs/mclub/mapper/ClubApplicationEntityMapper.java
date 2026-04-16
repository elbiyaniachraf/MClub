package io.droidevs.mclub.mapper;

import io.droidevs.mclub.domain.ClubApplication;
import io.droidevs.mclub.dto.ClubApplicationDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", config = CentralMapperConfig.class)
public interface ClubApplicationEntityMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "submittedBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    ClubApplication toEntity(ClubApplicationDto dto);
}

