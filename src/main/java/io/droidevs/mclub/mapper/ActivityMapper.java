package io.droidevs.mclub.mapper;
import io.droidevs.mclub.domain.Activity;
import io.droidevs.mclub.dto.ActivityDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", config = CentralMapperConfig.class)
public interface ActivityMapper {
    @Mapping(source = "club.id", target = "clubId")
    @Mapping(source = "event.id", target = "eventId")
    @Mapping(source = "createdBy.id", target = "createdById")
    ActivityDto toDto(Activity a);
}
