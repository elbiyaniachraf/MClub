package io.droidevs.mclub.mapper;

import io.droidevs.mclub.dto.EventCreateRequest;
import io.droidevs.mclub.dto.EventDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", config = CentralMapperConfig.class)
public interface EventCreateRequestMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    EventDto toDto(EventCreateRequest request);
}


