package io.droidevs.mclub.mapper;

import io.droidevs.mclub.domain.EventRegistration;
import org.mapstruct.Mapper;
import org.mapstruct.ObjectFactory;

@Mapper(componentModel = "spring", config = CentralMapperConfig.class)
public interface EventRegistrationFactoryMapper {

    @ObjectFactory
    default EventRegistration create() {
        return new EventRegistration();
    }
}

