package io.droidevs.mclub.mapper;

import io.droidevs.mclub.domain.Comment;
import io.droidevs.mclub.domain.User;
import io.droidevs.mclub.dto.CommentDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class CommentMapperTest {

    @Autowired
    private CommentMapper commentMapper;

    @Test
    void toDto_mapsLikeMetadataAndDeletedContent() {
        User author = User.builder().id(UUID.randomUUID()).fullName("A").build();

        Comment c = Comment.builder()
                .id(UUID.randomUUID())
                .author(author)
                .content("secret")
                .deleted(true)
                .createdAt(LocalDateTime.now())
                .build();

        CommentDto dto = commentMapper.toDto(c, 7L, true);
        assertNotNull(dto);
        assertEquals(c.getId(), dto.getId());
        assertEquals(author.getId(), dto.getAuthorId());
        assertEquals("A", dto.getAuthorFullName());
        assertEquals(7L, dto.getLikeCount());
        assertTrue(dto.isLikedByMe());
        assertEquals("[deleted]", dto.getContent());
        assertNotNull(dto.getReplies());
        assertTrue(dto.getReplies().isEmpty());
    }
}

