package io.droidevs.mclub.mapper;
import io.droidevs.mclub.domain.Event;
import io.droidevs.mclub.dto.EventDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", config = CentralMapperConfig.class)
public interface EventMapper {
    @Mapping(source = "club.id", target = "clubId")
    @Mapping(source = "createdBy.id", target = "createdById")
    EventDto toDto(Event event);
}
