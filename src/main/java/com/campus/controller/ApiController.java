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
        if (content == null || content.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "内容不能为空", "hint", "请输入动态内容后重试"));
        }
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
        if (post == null) return ResponseEntity.status(404).body(Map.of("error", "动态不存在", "hint", "该动态可能已被删除", "action", "go_home"));
        if (!post.getUserId().equals(uid())) return ResponseEntity.status(403).body(Map.of("error", "无权修改此动态", "hint", "只能修改自己发布的动态"));
        if (post.getStatus() == status) return ResponseEntity.badRequest().body(Map.of("error", "动态已是该状态", "hint", "当前状态: " + status));
        Post updated = postService.updateStatus(postId, status);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/posts/{postId}/visibility")
    public ResponseEntity<?> updatePostVisibility(@PathVariable Long postId, @RequestParam Visibility visibility) {
        Post post = postService.findById(postId);
        if (post == null) return ResponseEntity.status(404).body(Map.of("error", "动态不存在", "hint", "该动态可能已被删除", "action", "go_home"));
        if (!post.getUserId().equals(uid())) return ResponseEntity.status(403).body(Map.of("error", "无权修改此动态", "hint", "只能修改自己发布的动态"));
        if (post.getVisibility() == visibility) return ResponseEntity.badRequest().body(Map.of("error", "可见范围已是该设置", "hint", "当前范围: " + visibility, "retry", true));
        Post updated = postService.updateVisibility(postId, visibility);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<?> deletePost(@PathVariable Long postId) {
        Post post = postService.findById(postId);
        if (post == null) return ResponseEntity.status(404).body(Map.of("error", "动态不存在", "hint", "该动态可能已被删除", "action", "go_home"));
        if (!post.getUserId().equals(uid())) return ResponseEntity.status(403).body(Map.of("error", "无权删除此动态", "hint", "只能删除自己发布的动态"));
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
        if (toUserId.equals(uid())) {
            return ResponseEntity.badRequest().body(Map.of("error", "不能加自己为好友", "hint", "请选择其他同学"));
        }
        User target = userService.findById(toUserId);
        if (target == null) {
            return ResponseEntity.status(404).body(Map.of("error", "用户不存在", "hint", "该用户可能已被移除"));
        }
        FriendRequest req = friendService.addRequest(uid(), toUserId);
        if (req == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "操作无效", "hint", "不能加自己为好友"));
        }
        // 根据已存在记录的状态返回不同反馈
        if (req.getStatus() == FriendRequest.Status.ACCEPTED) {
            return ResponseEntity.ok(Map.of("error", "你们已经是好友了", "hint", "无需重复申请", "status", "ALREADY_FRIEND", "requestId", req.getId()));
        }
        if (req.getStatus() == FriendRequest.Status.PENDING) {
            // 区分是已有申请还是新创建的
            if (req.getFromUserId().equals(uid())) {
                return ResponseEntity.ok(Map.of("message", "好友申请已发送", "status", "SENT", "requestId", req.getId()));
            } else {
                return ResponseEntity.ok(Map.of("error", "对方已向你发送过申请", "hint", "请到好友页处理待处理申请", "status", "RECEIVED", "requestId", req.getId()));
            }
        }
        if (req.getStatus() == FriendRequest.Status.REJECTED) {
            return ResponseEntity.badRequest().body(Map.of("error", "之前的申请已被拒绝", "hint", "对方已拒绝过你的申请，暂不能重新申请", "status", "REJECTED", "requestId", req.getId()));
        }
        return ResponseEntity.ok(req);
    }

    @PostMapping("/friend-request/{id}/accept")
    public ResponseEntity<?> acceptFriendRequest(@PathVariable Long id) {
        FriendRequest existing = friendService.findById(id);
        if (existing == null) {
            return ResponseEntity.status(404).body(Map.of("error", "申请不存在", "hint", "该申请可能已被撤回"));
        }
        if (existing.getStatus() != FriendRequest.Status.PENDING) {
            String statusText = existing.getStatus() == FriendRequest.Status.ACCEPTED ? "已同意" : "已拒绝";
            return ResponseEntity.badRequest().body(Map.of("error", "申请已处理", "hint", "该申请" + statusText + "，无需重复操作", "currentStatus", existing.getStatus().toString()));
        }
        if (!existing.getToUserId().equals(uid())) {
            return ResponseEntity.status(403).body(Map.of("error", "无权操作", "hint", "只能处理发给自己的申请"));
        }
        FriendRequest req = friendService.acceptRequest(id);
        return ResponseEntity.ok(req);
    }

    @PostMapping("/friend-request/{id}/reject")
    public ResponseEntity<?> rejectFriendRequest(@PathVariable Long id) {
        FriendRequest existing = friendService.findById(id);
        if (existing == null) {
            return ResponseEntity.status(404).body(Map.of("error", "申请不存在", "hint", "该申请可能已被撤回"));
        }
        if (existing.getStatus() != FriendRequest.Status.PENDING) {
            String statusText = existing.getStatus() == FriendRequest.Status.ACCEPTED ? "已同意" : "已拒绝";
            return ResponseEntity.badRequest().body(Map.of("error", "申请已处理", "hint", "该申请" + statusText + "，无需重复操作", "currentStatus", existing.getStatus().toString()));
        }
        if (!existing.getToUserId().equals(uid())) {
            return ResponseEntity.status(403).body(Map.of("error", "无权操作", "hint", "只能处理发给自己的申请"));
        }
        FriendRequest req = friendService.rejectRequest(id);
        return ResponseEntity.ok(req);
    }

    @PostMapping("/posts/{postId}/comments")
    public ResponseEntity<?> addComment(@PathVariable Long postId,
                                        @RequestParam String content,
                                        @RequestParam(required = false) Long replyToId) {
        if (content == null || content.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "评论内容不能为空", "hint", "请输入评论内容"));
        }
        Post post = postService.findById(postId);
        if (post == null) return ResponseEntity.status(404).body(Map.of("error", "动态不存在", "hint", "该动态可能已被删除", "action", "go_home"));
        Set<Long> friendIds = friendService.getFriendIds(uid());
        if (!isPostVisible(post, uid(), friendIds)) {
            return ResponseEntity.status(403).body(Map.of("error", "无权评论此动态", "reason", postService.getDeniedReason(post, uid(), friendIds), "hint", "只有可见动态才能评论", "action", "apply_friend"));
        }
        // 检查动态状态
        if (post.getStatus() == PostStatus.HIDDEN) {
            return ResponseEntity.badRequest().body(Map.of("error", "动态已隐藏", "hint", "隐藏状态的动态不能评论，请先恢复"));
        }
        if (post.getStatus() == PostStatus.DRAFT && !post.getUserId().equals(uid())) {
            return ResponseEntity.status(403).body(Map.of("error", "草稿动态不能评论", "hint", "草稿仅在发布后才能被评论"));
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
        Post post = postService.findById(postId);
        if (post == null) return ResponseEntity.status(404).body(Map.of("error", "动态不存在", "hint", "该动态可能已被删除"));
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
