package io.droidevs.mclub.mapper;

import io.droidevs.mclub.domain.Comment;
import io.droidevs.mclub.dto.CommentDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", config = CentralMapperConfig.class)
public interface CommentMapper {

    /**
     * Maps a Comment entity to CommentDto and enriches with like metadata.
     * Replies are initialized to an empty list by default; service may attach replies after mapping.
     */
    @Mapping(source = "c.id", target = "id")
    @Mapping(source = "c.parentId", target = "parentId")
    @Mapping(source = "c.author.id", target = "authorId")
    @Mapping(source = "c.author.fullName", target = "authorFullName")
    @Mapping(source = "c.createdAt", target = "createdAt")
    @Mapping(source = "likeCount", target = "likeCount")
    @Mapping(source = "likedByMe", target = "likedByMe")
    @Mapping(expression = "java(c.isDeleted() ? \"[deleted]\" : c.getContent())", target = "content")
    @Mapping(expression = "java(new java.util.ArrayList<>())", target = "replies")
    @Mapping(constant = "0", target = "replyCount")
    @Mapping(constant = "false", target = "hasMoreReplies")
    CommentDto toDto(Comment c, long likeCount, boolean likedByMe);
}

