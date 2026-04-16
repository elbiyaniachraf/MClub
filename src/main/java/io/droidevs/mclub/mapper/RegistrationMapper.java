package io.droidevs.mclub.mapper;
import io.droidevs.mclub.domain.EventRegistration;
import io.droidevs.mclub.dto.RegistrationDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", config = CentralMapperConfig.class)
public interface RegistrationMapper {
    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "event.id", target = "eventId")
    RegistrationDto toDto(EventRegistration r);
}
