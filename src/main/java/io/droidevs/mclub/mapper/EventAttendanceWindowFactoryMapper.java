package io.droidevs.mclub.mapper;

import io.droidevs.mclub.domain.Event;
import io.droidevs.mclub.domain.EventAttendanceWindow;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", config = CentralMapperConfig.class)
public interface EventAttendanceWindowFactoryMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "event", source = "event")
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "opensBeforeStartMinutes", ignore = true)
    @Mapping(target = "closesAfterStartMinutes", ignore = true)
    @Mapping(target = "tokenHash", ignore = true)
    @Mapping(target = "tokenRotatedAt", ignore = true)
    EventAttendanceWindow create(Event event);
}

