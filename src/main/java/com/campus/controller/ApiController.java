package com.campus.controller;

import com.campus.config.SessionHelper;
import com.campus.dto.StatsDTO;
import com.campus.model.*;
import com.campus.service.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final UserService userService;
    private final PostService postService;
    private final FriendService friendService;
    private final CommentService commentService;
    private final StatsService statsService;
    private final LikeService likeService;
    private final SessionHelper sessionHelper;

    public ApiController(UserService userService, PostService postService,
                         FriendService friendService, CommentService commentService,
                         StatsService statsService, LikeService likeService,
                         SessionHelper sessionHelper) {
        this.userService = userService;
        this.postService = postService;
        this.friendService = friendService;
        this.commentService = commentService;
        this.statsService = statsService;
        this.likeService = likeService;
        this.sessionHelper = sessionHelper;
    }

    private Long uid() {
        return sessionHelper.getCurrentUserId();
    }

    @PostMapping("/posts")
    public ResponseEntity<?> createPost(@RequestParam String content,
                                        @RequestParam Visibility visibility,
                                        @RequestParam(required = false) String photoUrl,
                                        @RequestParam(required = false, defaultValue = "PUBLISHED") PostStatus status) {
        List<String> photos = new ArrayList<>();
        if (photoUrl != null && !photoUrl.isBlank()) {
            photos.add(photoUrl);
        }
        Post post = new Post(null, uid(), content, visibility, status, photos);
        Post saved = postService.createPost(post);
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/posts/{postId}/status")
    public ResponseEntity<?> updatePostStatus(@PathVariable Long postId, @RequestParam PostStatus status) {
        Post post = postService.findById(postId);
        if (post == null) return ResponseEntity.notFound().build();
        if (!post.getUserId().equals(uid())) return ResponseEntity.status(403).body("无权操作");
        Post updated = postService.updateStatus(postId, status);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/posts/{postId}/visibility")
    public ResponseEntity<?> updatePostVisibility(@PathVariable Long postId, @RequestParam Visibility visibility) {
        Post post = postService.findById(postId);
        if (post == null) return ResponseEntity.notFound().build();
        if (!post.getUserId().equals(uid())) return ResponseEntity.status(403).body("无权操作");
        Post updated = postService.updateVisibility(postId, visibility);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<?> deletePost(@PathVariable Long postId) {
        Post post = postService.findById(postId);
        if (post == null) return ResponseEntity.notFound().build();
        if (!post.getUserId().equals(uid())) return ResponseEntity.status(403).body("无权操作");
        postService.deleteById(postId);
        return ResponseEntity.ok(Map.of("deleted", true));
    }

    @GetMapping("/stats")
    public ResponseEntity<StatsDTO> getStats() {
        Set<Long> friendIds = friendService.getFriendIds(uid());
        StatsDTO stats = statsService.getStats(uid(), friendIds);
        return ResponseEntity.ok(stats);
    }

    @PostMapping("/friend-request")
    public ResponseEntity<?> sendFriendRequest(@RequestParam Long toUserId) {
        FriendRequest req = friendService.addRequest(uid(), toUserId);
        return ResponseEntity.ok(req);
    }

    @PostMapping("/friend-request/{id}/accept")
    public ResponseEntity<?> acceptFriendRequest(@PathVariable Long id) {
        FriendRequest req = friendService.acceptRequest(id);
        if (req == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(req);
    }

    @PostMapping("/friend-request/{id}/reject")
    public ResponseEntity<?> rejectFriendRequest(@PathVariable Long id) {
        FriendRequest req = friendService.rejectRequest(id);
        if (req == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(req);
    }

    @PostMapping("/posts/{postId}/comments")
    public ResponseEntity<?> addComment(@PathVariable Long postId,
                                        @RequestParam String content,
                                        @RequestParam(required = false) Long replyToId) {
        // 可见性检查：不可见时不允许评论
        Post post = postService.findById(postId);
        if (post == null) return ResponseEntity.status(404).body(Map.of("error", "动态不存在"));
        Set<Long> friendIds = friendService.getFriendIds(uid());
        if (!isPostVisible(post, uid(), friendIds)) {
            return ResponseEntity.status(403).body(Map.of("error", "无权评论此动态", "reason", postService.getDeniedReason(post, uid(), friendIds)));
        }

        Comment comment = commentService.addComment(postId, uid(), replyToId, content);
        Map<String, Object> result = new HashMap<>();
        result.put("comment", comment);
        result.put("user", userService.findById(uid()));
        if (replyToId != null) {
            Comment replyTo = commentService.findById(replyToId);
            if (replyTo != null) {
                result.put("replyToUser", userService.findById(replyTo.getUserId()));
            }
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/posts/{postId}/like")
    public ResponseEntity<?> toggleLike(@PathVariable Long postId) {
        boolean liked = likeService.toggleLike(postId, uid());
        Map<String, Object> result = new HashMap<>();
        result.put("liked", liked);
        result.put("likeCount", likeService.countByPostId(postId));
        return ResponseEntity.ok(result);
    }

    @GetMapping("/relationship")
    public ResponseEntity<String> getRelationship(@RequestParam Long targetUserId) {
        return ResponseEntity.ok(friendService.getRelationship(uid(), targetUserId));
    }

    private boolean isPostVisible(Post p, Long currentUserId, Set<Long> friendIds) {
        if (p.getStatus() == PostStatus.HIDDEN || p.getStatus() == PostStatus.DRAFT) {
            return p.getUserId().equals(currentUserId);
        }
        return switch (p.getVisibility()) {
            case PUBLIC -> true;
            case FRIENDS -> p.getUserId().equals(currentUserId) || friendIds.contains(p.getUserId());
            case PRIVATE -> p.getUserId().equals(currentUserId);
        };
    }
}
