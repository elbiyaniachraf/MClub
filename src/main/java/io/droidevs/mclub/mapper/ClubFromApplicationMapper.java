package io.droidevs.mclub.mapper;

import io.droidevs.mclub.domain.Club;
import io.droidevs.mclub.domain.ClubApplication;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", config = CentralMapperConfig.class)
public interface ClubFromApplicationMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Club toEntity(ClubApplication app);
}

