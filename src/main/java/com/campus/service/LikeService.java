package com.campus.service;

import com.campus.model.Like;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
public class LikeService {
    private final Map<Long, Like> likes = new LinkedHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(100);

    /**
     * 切换点赞：已赞则取消，未赞则添加
     * @return true=已赞, false=取消赞
     */
    public boolean toggleLike(Long postId, Long userId) {
        for (Like like : likes.values()) {
            if (like.getPostId().equals(postId) && like.getUserId().equals(userId)) {
                likes.remove(like.getId());
                return false;
            }
        }
        Like newLike = new Like(idGenerator.incrementAndGet(), postId, userId);
        likes.put(newLike.getId(), newLike);
        return true;
    }

    public int countByPostId(Long postId) {
        return (int) likes.values().stream()
                .filter(l -> l.getPostId().equals(postId))
                .count();
    }

    public boolean hasLiked(Long postId, Long userId) {
        return likes.values().stream()
                .anyMatch(l -> l.getPostId().equals(postId) && l.getUserId().equals(userId));
    }

    public void addInitialLike(Long postId, Long userId) {
        Like like = new Like(idGenerator.incrementAndGet(), postId, userId);
        likes.put(like.getId(), like);
    }
}
