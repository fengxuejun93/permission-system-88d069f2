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
    private final LikeService likeService;

    public PageController(UserService userService, PostService postService,
                          FriendService friendService, CommentService commentService,
                          StatsService statsService, LikeService likeService) {
        this.userService = userService;
        this.postService = postService;
        this.friendService = friendService;
        this.commentService = commentService;
        this.statsService = statsService;
        this.likeService = likeService;
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
        model.addAttribute("currentUserId", CURRENT_USER_ID);
        model.addAttribute("friendIds", friendIds);
    }

    @GetMapping("/")
    public String index(Model model) {
        addCommonAttributes(model);

        Set<Long> friendIds = friendService.getFriendIds(CURRENT_USER_ID);
        List<Post> posts = postService.getVisiblePosts(CURRENT_USER_ID, friendIds);

        List<Map<String, Object>> feedItems = new ArrayList<>();
        for (Post post : posts) {
            Map<String, Object> item = new HashMap<>();
            item.put("post", post);
            item.put("author", userService.findById(post.getUserId()));
            item.put("commentCount", commentService.getCommentsByPostId(post.getId()).size());
            item.put("likeCount", likeService.countByPostId(post.getId()));
            item.put("hasLiked", likeService.hasLiked(post.getId(), CURRENT_USER_ID));
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

        Set<Long> friendIds = friendService.getFriendIds(CURRENT_USER_ID);
        boolean visible = isPostVisible(post, CURRENT_USER_ID, friendIds);

        User author = userService.findById(post.getUserId());

        // 无论是否可见，都传入基本信息用于权限提示页
        model.addAttribute("post", post);
        model.addAttribute("author", author);
        model.addAttribute("isOwner", post.getUserId().equals(CURRENT_USER_ID));

        if (!visible) {
            // 不可见：展示权限提示，但仍保留作者信息和返回入口
            model.addAttribute("accessDenied", true);
            String reason = switch (post.getVisibility()) {
                case FRIENDS -> "该动态仅好友可见";
                case PRIVATE -> "该动态仅发布者自己可见";
                case PUBLIC -> "";
            };
            model.addAttribute("deniedReason", reason);
            return "post-detail";
        }

        model.addAttribute("accessDenied", false);
        List<Comment> comments = commentService.getCommentsByPostId(id);

        // 构建评论树：先找顶级评论，再找回复
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
        model.addAttribute("hasLiked", likeService.hasLiked(id, CURRENT_USER_ID));
        return "post-detail";
    }

    @GetMapping("/friends")
    public String friends(Model model) {
        addCommonAttributes(model);

        Set<Long> friendIds = friendService.getFriendIds(CURRENT_USER_ID);
        List<User> friends = userService.findByIds(friendIds);

        List<FriendRequest> sentRequests = friendService.getSentRequests(CURRENT_USER_ID);
        List<FriendRequest> pendingRequests = friendService.getPendingRequests(CURRENT_USER_ID);

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

        // 为个人主页的动态附加点赞和评论数
        List<Map<String, Object>> postItems = new ArrayList<>();
        for (Post post : userPosts) {
            Map<String, Object> item = new HashMap<>();
            item.put("post", post);
            item.put("commentCount", commentService.getCommentsByPostId(post.getId()).size());
            item.put("likeCount", likeService.countByPostId(post.getId()));
            postItems.add(item);
        }

        // 发布者的总动态数（包含不可见的）
        int totalPostCount = postService.countPostsByUser(id);
        // 好友数
        int profileFriendCount = friendService.getFriendCount(id);

        // 找到当前用户与目标用户之间的好友申请（如果有的话）
        FriendRequest pendingReq = null;
        for (FriendRequest r : friendService.getAllRequests()) {
            if (r.getStatus() == FriendRequest.Status.PENDING) {
                if ((r.getFromUserId().equals(CURRENT_USER_ID) && r.getToUserId().equals(id))
                        || (r.getFromUserId().equals(id) && r.getToUserId().equals(CURRENT_USER_ID))) {
                    pendingReq = r;
                    break;
                }
            }
        }

        boolean hasPhotos = userPosts.stream().anyMatch(p -> !p.getPhotoUrls().isEmpty());

        model.addAttribute("profileUser", profileUser);
        model.addAttribute("postItems", postItems);
        model.addAttribute("hasPhotos", hasPhotos);
        model.addAttribute("relationship", relationship);
        model.addAttribute("isSelf", id.equals(CURRENT_USER_ID));
        model.addAttribute("totalPostCount", totalPostCount);
        model.addAttribute("profileFriendCount", profileFriendCount);
        model.addAttribute("pendingRequest", pendingReq);
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
