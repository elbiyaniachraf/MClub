package io.droidevs.mclub.mapper;

import io.droidevs.mclub.domain.EventRating;
import org.mapstruct.Mapper;

/**
 * Factory-style mapper.
 *
 * <p>MapStruct cannot generate no-arg mapping methods; for pure "create empty entity" use a default
 * method so construction stays out of services and remains injectable/testable.
 */
@Mapper(componentModel = "spring", config = CentralMapperConfig.class)
public interface EventRatingFactoryMapper {

    default EventRating create() {
        return new EventRating();
    }
}

