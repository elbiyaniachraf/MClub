package io.droidevs.mclub.mapper;

import io.droidevs.mclub.domain.EventRating;
import io.droidevs.mclub.dto.EventRatingDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", config = CentralMapperConfig.class)
public interface EventRatingMapper {

    @Mapping(source = "event.id", target = "eventId")
    @Mapping(source = "student.email", target = "studentEmail")
    EventRatingDto toDto(EventRating rating);
}

