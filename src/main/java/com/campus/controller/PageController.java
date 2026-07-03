package com.campus.controller;

import com.campus.config.SessionHelper;
import com.campus.dto.StatsDTO;
import com.campus.model.*;
import com.campus.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.*;

@Controller
public class PageController {

    private final UserService userService;
    private final PostService postService;
    private final FriendService friendService;
    private final CommentService commentService;
    private final StatsService statsService;
    private final LikeService likeService;
    private final SessionHelper sessionHelper;

    public PageController(UserService userService, PostService postService,
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

    private Long currentUserId() {
        return sessionHelper.getCurrentUserId();
    }

    private void addCommonAttributes(Model model) {
        Long uid = currentUserId();
        User currentUser = userService.findById(uid);
        Set<Long> friendIds = friendService.getFriendIds(uid);
        StatsDTO stats = statsService.getStats(uid, friendIds);
        List<FriendRequest> pendingRequests = friendService.getPendingRequests(uid);
        List<User> allUsers = userService.findAll();

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("stats", stats);
        model.addAttribute("pendingRequests", pendingRequests);
        model.addAttribute("pendingCount", pendingRequests.size());
        model.addAttribute("currentUserId", uid);
        model.addAttribute("friendIds", friendIds);
        model.addAttribute("allUsers", allUsers);
    }

    @PostMapping("/switch-user")
    public String switchUser(@RequestParam Long userId, HttpSession session) {
        if (userService.findById(userId) != null) {
            sessionHelper.setCurrentUserId(session, userId);
        }
        return "redirect:/";
    }

    @GetMapping("/")
    public String index(Model model) {
        addCommonAttributes(model);

        Long uid = currentUserId();
        Set<Long> friendIds = friendService.getFriendIds(uid);
        List<Post> posts = postService.getVisiblePosts(uid, friendIds);

        List<Map<String, Object>> feedItems = new ArrayList<>();
        for (Post post : posts) {
            Map<String, Object> item = new HashMap<>();
            item.put("post", post);
            item.put("author", userService.findById(post.getUserId()));
            item.put("commentCount", commentService.getCommentsByPostId(post.getId()).size());
            item.put("likeCount", likeService.countByPostId(post.getId()));
            item.put("hasLiked", likeService.hasLiked(post.getId(), uid));
            feedItems.add(item);
        }

        model.addAttribute("feedItems", feedItems);
        return "index";
    }

    @GetMapping("/post/{id}")
    public String postDetail(@PathVariable Long id, Model model) {
        addCommonAttributes(model);

        Long uid = currentUserId();
        Post post = postService.findById(id);
        if (post == null) return "redirect:/";

        Set<Long> friendIds = friendService.getFriendIds(uid);
        boolean visible = isPostVisible(post, uid, friendIds);

        User author = userService.findById(post.getUserId());

        model.addAttribute("post", post);
        model.addAttribute("author", author);
        model.addAttribute("isOwner", post.getUserId().equals(uid));

        if (!visible) {
            model.addAttribute("accessDenied", true);
            String reason = postService.getDeniedReason(post, uid, friendIds);
            model.addAttribute("deniedReason", reason);
            // 好友关系信息，用于权限提示页的操作入口
            String relationship = friendService.getRelationship(uid, author.getId());
            model.addAttribute("authorRelationship", relationship);
            return "post-detail";
        }

        model.addAttribute("accessDenied", false);
        List<Comment> comments = commentService.getCommentsByPostId(id);

        Map<Long, List<Map<String, Object>>> repliesMap = new LinkedHashMap<>();
        List<Map<String, Object>> topComments = new ArrayList<>();

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
            if (c.getReplyToId() == null) {
                topComments.add(item);
            } else {
                repliesMap.computeIfAbsent(c.getReplyToId(), k -> new ArrayList<>()).add(item);
            }
        }

        model.addAttribute("topComments", topComments);
        model.addAttribute("repliesMap", repliesMap);
        model.addAttribute("commentCount", comments.size());
        model.addAttribute("likeCount", likeService.countByPostId(id));
        model.addAttribute("hasLiked", likeService.hasLiked(id, uid));
        return "post-detail";
    }

    @GetMapping("/friends")
    public String friends(Model model) {
        addCommonAttributes(model);

        Long uid = currentUserId();
        Set<Long> friendIds = friendService.getFriendIds(uid);
        List<User> friends = userService.findByIds(friendIds);

        List<FriendRequest> sentRequests = friendService.getSentRequests(uid);
        List<FriendRequest> pendingRequests = friendService.getPendingRequests(uid);

        List<User> allUsers = userService.findAll();
        List<Map<String, Object>> discoverUsers = new ArrayList<>();
        for (User u : allUsers) {
            if (!u.getId().equals(uid) && !friendIds.contains(u.getId())) {
                Map<String, Object> item = new HashMap<>();
                item.put("user", u);
                item.put("relationship", friendService.getRelationship(uid, u.getId()));
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

        Long uid = currentUserId();
        User profileUser = userService.findById(id);
        if (profileUser == null) return "redirect:/";

        Set<Long> friendIds = friendService.getFriendIds(uid);
        List<Post> userPosts = postService.getVisiblePostsByUser(id, uid, friendIds);
        String relationship = friendService.getRelationship(uid, id);

        List<Map<String, Object>> postItems = new ArrayList<>();
        for (Post post : userPosts) {
            Map<String, Object> item = new HashMap<>();
            item.put("post", post);
            item.put("commentCount", commentService.getCommentsByPostId(post.getId()).size());
            item.put("likeCount", likeService.countByPostId(post.getId()));
            postItems.add(item);
        }

        int totalPostCount = postService.countPostsByUser(id);
        int profileFriendCount = friendService.getFriendCount(id);

        // 如果是自己的主页，额外传入所有动态（含草稿和隐藏的）
        List<Map<String, Object>> allPostItems = null;
        if (id.equals(uid)) {
            List<Post> allPosts = postService.getAllPostsByUser(uid);
            allPostItems = new ArrayList<>();
            for (Post post : allPosts) {
                Map<String, Object> item = new HashMap<>();
                item.put("post", post);
                item.put("commentCount", commentService.getCommentsByPostId(post.getId()).size());
                item.put("likeCount", likeService.countByPostId(post.getId()));
                allPostItems.add(item);
            }
        }

        FriendRequest pendingReq = null;
        for (FriendRequest r : friendService.getAllRequests()) {
            if (r.getStatus() == FriendRequest.Status.PENDING) {
                if ((r.getFromUserId().equals(uid) && r.getToUserId().equals(id))
                        || (r.getFromUserId().equals(id) && r.getToUserId().equals(uid))) {
                    pendingReq = r;
                    break;
                }
            }
        }

        boolean hasPhotos = userPosts.stream().anyMatch(p -> !p.getPhotoUrls().isEmpty());

        model.addAttribute("profileUser", profileUser);
        model.addAttribute("postItems", postItems);
        model.addAttribute("allPostItems", allPostItems);
        model.addAttribute("hasPhotos", hasPhotos);
        model.addAttribute("relationship", relationship);
        model.addAttribute("isSelf", id.equals(uid));
        model.addAttribute("totalPostCount", totalPostCount);
        model.addAttribute("profileFriendCount", profileFriendCount);
        model.addAttribute("pendingRequest", pendingReq);
        return "profile";
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
