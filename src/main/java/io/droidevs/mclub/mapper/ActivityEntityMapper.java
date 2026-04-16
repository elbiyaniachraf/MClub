package io.droidevs.mclub.mapper;

import io.droidevs.mclub.domain.Activity;
import io.droidevs.mclub.dto.ActivityDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", config = CentralMapperConfig.class)
public interface ActivityEntityMapper {

    /**
     * Maps client DTO into a new Activity entity.
     * Service layer remains responsible for setting relationships and controlled fields.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "club", ignore = true)
    @Mapping(target = "event", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    Activity toEntity(ActivityDto dto);
}

