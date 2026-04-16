package io.droidevs.mclub.mapper;

import io.droidevs.mclub.domain.Event;
import io.droidevs.mclub.dto.EventDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", config = CentralMapperConfig.class)
public interface EventEntityMapper {

    /**
     * Maps client DTO into a new Event entity.
     * Service layer remains responsible for setting relationships and controlled fields.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "club", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Event toEntity(EventDto dto);
}

