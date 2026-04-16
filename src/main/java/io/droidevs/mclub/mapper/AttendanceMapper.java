package io.droidevs.mclub.mapper;

import io.droidevs.mclub.domain.EventAttendance;
import io.droidevs.mclub.dto.AttendanceRecordDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", config = CentralMapperConfig.class)
public interface AttendanceMapper {

    @Mapping(source = "event.id", target = "eventId")
    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "user.email", target = "userEmail")
    @Mapping(expression = "java(a.getMethod() != null ? a.getMethod().name() : null)", target = "method")
    AttendanceRecordDto toDto(EventAttendance a);
}

