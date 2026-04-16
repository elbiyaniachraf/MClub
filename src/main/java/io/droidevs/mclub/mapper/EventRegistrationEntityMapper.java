package io.droidevs.mclub.mapper;

import io.droidevs.mclub.domain.EventRegistration;
import io.droidevs.mclub.dto.RegistrationDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", config = CentralMapperConfig.class)
public interface EventRegistrationEntityMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "event", ignore = true)
    @Mapping(target = "registeredAt", ignore = true)
    EventRegistration toEntity(RegistrationDto dto);
}

