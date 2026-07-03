package com.campus.service;

import com.campus.model.Comment;
import com.campus.model.Post;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
public class CommentService {
    private final Map<Long, Comment> comments = new LinkedHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(100);

    public Comment addComment(Long postId, Long userId, Long replyToId, String content) {
        Comment comment = new Comment(idGenerator.incrementAndGet(), postId, userId, replyToId, content);
        comments.put(comment.getId(), comment);
        return comment;
    }

    public List<Comment> getCommentsByPostId(Long postId) {
        return comments.values().stream()
                .filter(c -> c.getPostId().equals(postId))
                .sorted(Comparator.comparing(Comment::getCreatedAt))
                .collect(Collectors.toList());
    }

    public Comment findById(Long id) {
        return comments.get(id);
    }

    public int countCommentsForVisiblePosts(Long currentUserId, Set<Long> friendIds, PostService postService) {
        List<Post> visiblePosts = postService.getVisiblePosts(currentUserId, friendIds);
        Set<Long> visiblePostIds = visiblePosts.stream().map(Post::getId).collect(Collectors.toSet());
        return (int) comments.values().stream()
                .filter(c -> visiblePostIds.contains(c.getPostId()))
                .count();
    }
}
