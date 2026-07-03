package com.campus.service;

import com.campus.model.Post;
import com.campus.model.Visibility;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
public class PostService {
    private final Map<Long, Post> posts = new LinkedHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(100);

    public Post createPost(Post post) {
        if (post.getId() == null) {
            post.setId(idGenerator.incrementAndGet());
        }
        posts.put(post.getId(), post);
        return post;
    }

    public Post findById(Long id) {
        return posts.get(id);
    }

    /**
     * 获取当前用户可见的动态列表
     * 规则：PUBLIC 所有人可见，FRIENDS 好友可见，PRIVATE 仅自己可见
     */
    public List<Post> getVisiblePosts(Long currentUserId, Set<Long> friendIds) {
        return posts.values().stream()
                .filter(p -> isPostVisible(p, currentUserId, friendIds))
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .collect(Collectors.toList());
    }

    public List<Post> getPostsByUser(Long userId) {
        return posts.values().stream()
                .filter(p -> p.getUserId().equals(userId))
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .collect(Collectors.toList());
    }

    public List<Post> getVisiblePostsByUser(Long targetUserId, Long currentUserId, Set<Long> friendIds) {
        return posts.values().stream()
                .filter(p -> p.getUserId().equals(targetUserId))
                .filter(p -> isPostVisible(p, currentUserId, friendIds))
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .collect(Collectors.toList());
    }

    private boolean isPostVisible(Post p, Long currentUserId, Set<Long> friendIds) {
        return switch (p.getVisibility()) {
            case PUBLIC -> true;
            case FRIENDS -> p.getUserId().equals(currentUserId) || friendIds.contains(p.getUserId());
            case PRIVATE -> p.getUserId().equals(currentUserId);
        };
    }

    public int countVisiblePhotos(Long currentUserId, Set<Long> friendIds) {
        return (int) posts.values().stream()
                .filter(p -> isPostVisible(p, currentUserId, friendIds))
                .mapToLong(p -> p.getPhotoUrls().size())
                .sum();
    }

    public int countPostsByUser(Long userId) {
        return (int) posts.values().stream()
                .filter(p -> p.getUserId().equals(userId))
                .count();
    }
}
