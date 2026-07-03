package com.campus.service;

import com.campus.model.Post;
import com.campus.model.PostStatus;
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
        if (post.getStatus() == null) {
            post.setStatus(PostStatus.PUBLISHED);
        }
        posts.put(post.getId(), post);
        return post;
    }

    public Post findById(Long id) {
        return posts.get(id);
    }

    public List<Post> getAllPosts() {
        return new ArrayList<>(posts.values());
    }

    public void deleteById(Long id) {
        posts.remove(id);
    }

    public Post updateStatus(Long postId, PostStatus newStatus) {
        Post post = posts.get(postId);
        if (post != null) {
            post.setStatus(newStatus);
        }
        return post;
    }

    public Post updateVisibility(Long postId, Visibility newVisibility) {
        Post post = posts.get(postId);
        if (post != null) {
            post.setVisibility(newVisibility);
        }
        return post;
    }

    /**
     * 获取当前用户可见的动态列表
     * 规则：
     * - HIDDEN 动态仅作者自己可见（标为隐藏）
     * - DRAFT 动态仅作者自己可见
     * - PUBLISHED 动态按 visibility 规则：PUBLIC/FRIENDS/PRIVATE
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

    /**
     * 获取某用户的所有动态（不管可见性），用于本人视角的管理页
     */
    public List<Post> getAllPostsByUser(Long userId) {
        return posts.values().stream()
                .filter(p -> p.getUserId().equals(userId))
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .collect(Collectors.toList());
    }

    private boolean isPostVisible(Post p, Long currentUserId, Set<Long> friendIds) {
        // 隐藏/草稿动态只有作者可见
        if (p.getStatus() == PostStatus.HIDDEN || p.getStatus() == PostStatus.DRAFT) {
            return p.getUserId().equals(currentUserId);
        }
        // PUBLISHED 动态按可见性规则
        return switch (p.getVisibility()) {
            case PUBLIC -> true;
            case FRIENDS -> p.getUserId().equals(currentUserId) || friendIds.contains(p.getUserId());
            case PRIVATE -> p.getUserId().equals(currentUserId);
        };
    }

    public String getDeniedReason(Post p, Long currentUserId, Set<Long> friendIds) {
        if (p.getStatus() == PostStatus.HIDDEN) {
            return "该动态已被发布者隐藏";
        }
        if (p.getStatus() == PostStatus.DRAFT) {
            return "该动态为草稿，仅发布者可见";
        }
        return switch (p.getVisibility()) {
            case FRIENDS -> "该动态仅好友可见";
            case PRIVATE -> "该动态仅发布者自己可见";
            case PUBLIC -> "";
        };
    }

    public int countVisiblePhotos(Long currentUserId, Set<Long> friendIds) {
        return (int) posts.values().stream()
                .filter(p -> isPostVisible(p, currentUserId, friendIds))
                .mapToLong(p -> p.getPhotoUrls().size())
                .sum();
    }

    public int countPhotosByVisibility(Visibility visibility, Long currentUserId, Set<Long> friendIds) {
        return (int) posts.values().stream()
                .filter(p -> isPostVisible(p, currentUserId, friendIds))
                .filter(p -> p.getVisibility() == visibility)
                .mapToLong(p -> p.getPhotoUrls().size())
                .sum();
    }

    public int countPostsByUser(Long userId) {
        return (int) posts.values().stream()
                .filter(p -> p.getUserId().equals(userId) && p.getStatus() == PostStatus.PUBLISHED)
                .count();
    }

    public int countVisiblePosts(Long currentUserId, Set<Long> friendIds) {
        return (int) posts.values().stream()
                .filter(p -> isPostVisible(p, currentUserId, friendIds))
                .count();
    }
}
