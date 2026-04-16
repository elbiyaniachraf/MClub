package io.droidevs.mclub.mapper;

import io.droidevs.mclub.domain.AttendanceMethod;
import io.droidevs.mclub.domain.EventAttendance;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", config = CentralMapperConfig.class)
public interface EventAttendanceFactoryMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "event", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "checkedInBy", ignore = true)
    @Mapping(target = "checkedInAt", ignore = true)
    EventAttendance create(AttendanceMethod method);
}

