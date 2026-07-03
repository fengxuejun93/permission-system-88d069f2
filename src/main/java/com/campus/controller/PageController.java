package com.campus.controller;

import com.campus.dto.StatsDTO;
import com.campus.model.*;
import com.campus.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Controller
public class PageController {

    private static final Long CURRENT_USER_ID = 1L;

    private final UserService userService;
    private final PostService postService;
    private final FriendService friendService;
    private final CommentService commentService;
    private final StatsService statsService;

    public PageController(UserService userService, PostService postService,
                          FriendService friendService, CommentService commentService,
                          StatsService statsService) {
        this.userService = userService;
        this.postService = postService;
        this.friendService = friendService;
        this.commentService = commentService;
        this.statsService = statsService;
    }

    private void addCommonAttributes(Model model) {
        User currentUser = userService.findById(CURRENT_USER_ID);
        Set<Long> friendIds = friendService.getFriendIds(CURRENT_USER_ID);
        StatsDTO stats = statsService.getStats(CURRENT_USER_ID, friendIds);
        List<FriendRequest> pendingRequests = friendService.getPendingRequests(CURRENT_USER_ID);

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("stats", stats);
        model.addAttribute("pendingRequests", pendingRequests);
        model.addAttribute("pendingCount", pendingRequests.size());
    }

    @GetMapping("/")
    public String index(Model model) {
        addCommonAttributes(model);

        Set<Long> friendIds = friendService.getFriendIds(CURRENT_USER_ID);
        List<Post> posts = postService.getVisiblePosts(CURRENT_USER_ID, friendIds);

        // 为每条动态附加作者信息和评论数
        List<Map<String, Object>> feedItems = new ArrayList<>();
        for (Post post : posts) {
            Map<String, Object> item = new HashMap<>();
            item.put("post", post);
            item.put("author", userService.findById(post.getUserId()));
            item.put("commentCount", commentService.getCommentsByPostId(post.getId()).size());
            feedItems.add(item);
        }

        model.addAttribute("feedItems", feedItems);
        return "index";
    }

    @GetMapping("/post/{id}")
    public String postDetail(@PathVariable Long id, Model model) {
        addCommonAttributes(model);

        Post post = postService.findById(id);
        if (post == null) return "redirect:/";

        // 可见性检查
        Set<Long> friendIds = friendService.getFriendIds(CURRENT_USER_ID);
        if (!isPostVisible(post, CURRENT_USER_ID, friendIds)) {
            return "redirect:/";
        }

        User author = userService.findById(post.getUserId());
        List<Comment> comments = commentService.getCommentsByPostId(id);

        // 构建评论树
        List<Map<String, Object>> commentItems = new ArrayList<>();
        for (Comment c : comments) {
            Map<String, Object> item = new HashMap<>();
            item.put("comment", c);
            item.put("user", userService.findById(c.getUserId()));
            if (c.getReplyToId() != null) {
                Comment replyTo = commentService.findById(c.getReplyToId());
                if (replyTo != null) {
                    item.put("replyToUser", userService.findById(replyTo.getUserId()));
                }
            }
            commentItems.add(item);
        }

        model.addAttribute("post", post);
        model.addAttribute("author", author);
        model.addAttribute("commentItems", commentItems);
        model.addAttribute("isOwner", post.getUserId().equals(CURRENT_USER_ID));
        return "post-detail";
    }

    @GetMapping("/friends")
    public String friends(Model model) {
        addCommonAttributes(model);

        Set<Long> friendIds = friendService.getFriendIds(CURRENT_USER_ID);
        List<User> friends = userService.findByIds(friendIds);

        List<FriendRequest> sentRequests = friendService.getSentRequests(CURRENT_USER_ID);
        List<FriendRequest> pendingRequests = friendService.getPendingRequests(CURRENT_USER_ID);

        // 所有非好友用户（排除自己）
        List<User> allUsers = userService.findAll();
        List<Map<String, Object>> discoverUsers = new ArrayList<>();
        for (User u : allUsers) {
            if (!u.getId().equals(CURRENT_USER_ID) && !friendIds.contains(u.getId())) {
                Map<String, Object> item = new HashMap<>();
                item.put("user", u);
                item.put("relationship", friendService.getRelationship(CURRENT_USER_ID, u.getId()));
                discoverUsers.add(item);
            }
        }

        model.addAttribute("friends", friends);
        model.addAttribute("sentRequests", sentRequests);
        model.addAttribute("receivedRequests", pendingRequests);
        model.addAttribute("discoverUsers", discoverUsers);
        return "friends";
    }

    @GetMapping("/profile/{id}")
    public String profile(@PathVariable Long id, Model model) {
        addCommonAttributes(model);

        User profileUser = userService.findById(id);
        if (profileUser == null) return "redirect:/";

        Set<Long> friendIds = friendService.getFriendIds(CURRENT_USER_ID);
        List<Post> userPosts = postService.getVisiblePostsByUser(id, CURRENT_USER_ID, friendIds);

        String relationship = friendService.getRelationship(CURRENT_USER_ID, id);

        model.addAttribute("profileUser", profileUser);
        model.addAttribute("userPosts", userPosts);
        model.addAttribute("relationship", relationship);
        model.addAttribute("isSelf", id.equals(CURRENT_USER_ID));
        return "profile";
    }

    private boolean isPostVisible(Post p, Long currentUserId, Set<Long> friendIds) {
        return switch (p.getVisibility()) {
            case PUBLIC -> true;
            case FRIENDS -> p.getUserId().equals(currentUserId) || friendIds.contains(p.getUserId());
            case PRIVATE -> p.getUserId().equals(currentUserId);
        };
    }
}
