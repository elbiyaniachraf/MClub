package io.droidevs.mclub.mapper;

import io.droidevs.mclub.domain.User;
import io.droidevs.mclub.dto.RegisterRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", config = CentralMapperConfig.class)
public interface UserEntityMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "password", ignore = true) // encoded in AuthService
    @Mapping(target = "role", ignore = true)     // derived in AuthService
    User toEntity(RegisterRequest request);
}

