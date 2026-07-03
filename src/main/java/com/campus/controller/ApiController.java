package com.campus.controller;

import com.campus.dto.StatsDTO;
import com.campus.model.*;
import com.campus.service.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api")
public class ApiController {

    private static final Long CURRENT_USER_ID = 1L;

    private final UserService userService;
    private final PostService postService;
    private final FriendService friendService;
    private final CommentService commentService;
    private final StatsService statsService;

    public ApiController(UserService userService, PostService postService,
                         FriendService friendService, CommentService commentService,
                         StatsService statsService) {
        this.userService = userService;
        this.postService = postService;
        this.friendService = friendService;
        this.commentService = commentService;
        this.statsService = statsService;
    }

    @PostMapping("/posts")
    public ResponseEntity<?> createPost(@RequestParam String content,
                                        @RequestParam Visibility visibility,
                                        @RequestParam(required = false) String photoUrl) {
        List<String> photos = new ArrayList<>();
        if (photoUrl != null && !photoUrl.isBlank()) {
            photos.add(photoUrl);
        }
        Post post = new Post(null, CURRENT_USER_ID, content, visibility, photos);
        Post saved = postService.createPost(post);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/stats")
    public ResponseEntity<StatsDTO> getStats() {
        Set<Long> friendIds = friendService.getFriendIds(CURRENT_USER_ID);
        StatsDTO stats = statsService.getStats(CURRENT_USER_ID, friendIds);
        return ResponseEntity.ok(stats);
    }

    @PostMapping("/friend-request")
    public ResponseEntity<?> sendFriendRequest(@RequestParam Long toUserId) {
        FriendRequest req = friendService.addRequest(CURRENT_USER_ID, toUserId);
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
        Comment comment = commentService.addComment(postId, CURRENT_USER_ID, replyToId, content);
        Map<String, Object> result = new HashMap<>();
        result.put("comment", comment);
        result.put("user", userService.findById(CURRENT_USER_ID));
        if (replyToId != null) {
            Comment replyTo = commentService.findById(replyToId);
            if (replyTo != null) {
                result.put("replyToUser", userService.findById(replyTo.getUserId()));
            }
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/relationship")
    public ResponseEntity<String> getRelationship(@RequestParam Long targetUserId) {
        return ResponseEntity.ok(friendService.getRelationship(CURRENT_USER_ID, targetUserId));
    }
}
