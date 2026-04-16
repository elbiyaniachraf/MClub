package io.droidevs.mclub.service;

import io.droidevs.mclub.domain.*;
import io.droidevs.mclub.dto.CommentCreateRequest;
import io.droidevs.mclub.dto.CommentDto;
import io.droidevs.mclub.exception.ForbiddenException;
import io.droidevs.mclub.mapper.CommentMapper;
import io.droidevs.mclub.repository.*;
import io.droidevs.mclub.security.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final UserRepository userRepository;

    private final EventRepository eventRepository;
    private final ActivityRepository activityRepository;

    private final CommentMapper commentMapper;
    private final io.droidevs.mclub.mapper.CommentFactoryMapper commentFactoryMapper;
    private final io.droidevs.mclub.mapper.CommentLikeFactoryMapper commentLikeFactoryMapper;

    @Transactional(readOnly = true)
    public List<CommentDto> getThread(CommentTargetType targetType, UUID targetId, String currentUserEmail) {
        User me = currentUserEmail != null ? userRepository.findByEmail(currentUserEmail).orElse(null) : null;
        UUID myId = me != null ? me.getId() : null;

        // Fetch all comments for target with author eagerly loaded
        List<Comment> all = commentRepository.findThreadWithAuthor(targetType, targetId);

        // Precompute like counts + likedByMe (simple approach via per-comment lookups; OK for small sizes).
        // If you need scale, we can replace with aggregate queries.
        Map<UUID, Long> likeCounts = new HashMap<>();
        Set<UUID> likedIdsByMe = new HashSet<>();

        for (Comment c : all) {
            likeCounts.put(c.getId(), commentLikeRepository.countByCommentId(c.getId()));
            if (myId != null && commentLikeRepository.findByCommentIdAndUserId(c.getId(), myId).isPresent()) {
                likedIdsByMe.add(c.getId());
            }
        }

        Map<UUID, CommentDto> dtoById = all.stream()
                .collect(Collectors.toMap(Comment::getId,
                        c -> commentMapper.toDto(c, likeCounts.getOrDefault(c.getId(), 0L), likedIdsByMe.contains(c.getId()))));

        // Root list
        List<CommentDto> roots = new ArrayList<>();
        for (Comment c : all) {
            CommentDto dto = dtoById.get(c.getId());
            if (c.getParentId() == null) {
                roots.add(dto);
            } else {
                CommentDto parent = dtoById.get(c.getParentId());
                if (parent != null) {
                    // replies list is initialized by mapper
                    parent.getReplies().add(dto);
                } else {
                    // Orphan reply: treat as root
                    roots.add(dto);
                }
            }
        }

        // Populate replyCount/hasMoreReplies for the full thread model too
        for (CommentDto r : roots) {
            populateReplyMetaRecursive(r);
        }

        return roots;
    }

    /**
     * Preview helper for detail pages: returns the newest root comments, each with reply metadata.
     * Replies are not populated (only replyCount/hasMoreReplies for UI text).
     */
    @Transactional(readOnly = true)
    public List<CommentDto> getRootPreview(CommentTargetType targetType,
                                          UUID targetId,
                                          String currentUserEmail,
                                          int limit) {
        List<CommentDto> roots = getThread(targetType, targetId, currentUserEmail);
        // newest first for preview (detail pages typically show latest activity)
        roots.sort(Comparator.comparing(CommentDto::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed());

        List<CommentDto> out = roots.stream().limit(Math.max(0, limit)).toList();
        // Ensure replies aren't rendered in preview (we show only replyCount text)
        for (CommentDto c : out) {
            c.setReplies(Collections.emptyList());
        }
        return out;
    }

    /**
     * Returns a thread where each root comment shows only a preview of its first reply.
     * Nested replies beyond that are not included to keep UI compact.
     */
    @Transactional(readOnly = true)
    public List<CommentDto> getThreadWithReplyPreview(CommentTargetType targetType,
                                                      UUID targetId,
                                                      String currentUserEmail,
                                                      int repliesPreviewLimitPerRoot) {
        List<CommentDto> roots = getThread(targetType, targetId, currentUserEmail);

        // For each root comment: keep only the first N direct replies, drop deeper nesting.
        for (CommentDto root : roots) {
            List<CommentDto> direct = root.getReplies() != null ? root.getReplies() : Collections.emptyList();
            root.setReplyCount(direct.size());
            root.setHasMoreReplies(direct.size() > repliesPreviewLimitPerRoot);

            List<CommentDto> trimmed = direct.stream().limit(Math.max(0, repliesPreviewLimitPerRoot)).toList();
            // Remove deeper nesting from the preview replies
            List<CommentDto> cleaned = new ArrayList<>();
            for (CommentDto r : trimmed) {
                r.setReplies(Collections.emptyList());
                r.setReplyCount(0);
                r.setHasMoreReplies(false);
                cleaned.add(r);
            }
            root.setReplies(cleaned);
        }

        return roots;
    }

    /**
     * Load ALL direct replies for a given parent comment (children only; no deep nesting).
     * This powers the "See more replies" expansion on the comments page.
     */
    @Transactional(readOnly = true)
    public List<CommentDto> getDirectReplies(UUID parentCommentId, String currentUserEmail) {
        User me = currentUserEmail != null ? userRepository.findByEmail(currentUserEmail).orElse(null) : null;
        UUID myId = me != null ? me.getId() : null;

        List<Comment> replies = commentRepository.findRepliesWithAuthor(parentCommentId);

        Map<UUID, Long> likeCounts = new HashMap<>();
        Set<UUID> likedIdsByMe = new HashSet<>();
        for (Comment c : replies) {
            likeCounts.put(c.getId(), commentLikeRepository.countByCommentId(c.getId()));
            if (myId != null && commentLikeRepository.findByCommentIdAndUserId(c.getId(), myId).isPresent()) {
                likedIdsByMe.add(c.getId());
            }
        }

        List<CommentDto> out = replies.stream()
                .map(c -> commentMapper.toDto(c, likeCounts.getOrDefault(c.getId(), 0L), likedIdsByMe.contains(c.getId())))
                .collect(Collectors.toList());

        // Ensure child-of-reply lists are empty for this endpoint to keep UI bounded.
        for (CommentDto dto : out) {
            dto.setReplies(Collections.emptyList());
            dto.setReplyCount(0);
            dto.setHasMoreReplies(false);
        }

        return out;
    }

    private void populateReplyMetaRecursive(CommentDto node) {
        if (node == null) {
            return;
        }
        List<CommentDto> replies = node.getReplies() != null ? node.getReplies() : Collections.emptyList();
        node.setReplyCount(replies.size());
        node.setHasMoreReplies(false);
        for (CommentDto r : replies) {
            populateReplyMetaRecursive(r);
        }
    }

    @Transactional
    public CommentDto addComment(CommentTargetType targetType, UUID targetId, CommentCreateRequest request, String studentEmail) {
        User student = userRepository.findByEmail(studentEmail).orElseThrow();

        // Minimal guard: only STUDENT should post comments (controller also enforces)
        if (student.getRole() != Role.STUDENT) {
            throw new ForbiddenException("Only students can comment");
        }

        // Validate target exists
        if (targetType == CommentTargetType.EVENT) {
            eventRepository.findById(targetId).orElseThrow();
        } else {
            activityRepository.findById(targetId).orElseThrow();
        }

        UUID parentId = request.getParentId();
        if (parentId != null) {
            Comment parent = commentRepository.findById(parentId).orElseThrow();
            if (parent.getTargetType() != targetType || !parent.getTargetId().equals(targetId)) {
                throw new ForbiddenException("Parent comment does not belong to this target");
            }
        }

        Comment comment = commentFactoryMapper.create(targetType, targetId, parentId, request.getContent().trim());
        comment.setAuthor(student);
        comment.setDeleted(false);

        Comment saved = commentRepository.save(comment);
        return commentMapper.toDto(saved, 0L, false);
    }

    @Transactional
    public void toggleLike(UUID commentId, String studentEmail) {
        User student = userRepository.findByEmail(studentEmail).orElseThrow();
        if (student.getRole() != Role.STUDENT) {
            throw new ForbiddenException("Only students can like comments");
        }

        Comment comment = commentRepository.findById(commentId).orElseThrow();

        Optional<CommentLike> existing = commentLikeRepository.findByCommentIdAndUserId(commentId, student.getId());
        if (existing.isPresent()) {
            commentLikeRepository.delete(existing.get());
        } else {
            commentLikeRepository.save(commentLikeFactoryMapper.create(comment, student));
        }
    }

    @Transactional
    public CommentDto reply(UUID parentCommentId, CommentCreateRequest request, String studentEmail) {
        Comment parent = commentRepository.findById(parentCommentId).orElseThrow();

        CommentCreateRequest req = new CommentCreateRequest();
        req.setContent(request.getContent());
        req.setParentId(parent.getId());

        return addComment(parent.getTargetType(), parent.getTargetId(), req, studentEmail);
    }
}
