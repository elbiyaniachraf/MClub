package io.droidevs.mclub.mapper;

import io.droidevs.mclub.domain.Comment;
import io.droidevs.mclub.domain.CommentTargetType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.UUID;

@Mapper(componentModel = "spring", config = CentralMapperConfig.class)
public interface CommentFactoryMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "author", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "content", source = "content")
    Comment create(CommentTargetType targetType, UUID targetId, UUID parentId, String content);
}

