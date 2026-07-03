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
import java.util.stream.Collectors;

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
        model.addAttribute("isAdmin", currentUser != null && currentUser.isAdmin());
        model.addAttribute("isSystemAdmin", currentUser != null && currentUser.isSystemAdmin());
    }

    @PostMapping("/switch-user")
    public String switchUser(@RequestParam Long userId, HttpSession session) {
        if (userService.findById(userId) != null) {
            sessionHelper.setCurrentUserId(session, userId);
        }
        return "redirect:/";
    }

    @GetMapping("/")
    public String index(@RequestParam(required = false) String visibility,
                        @RequestParam(required = false) String status,
                        @RequestParam(required = false) String authorId,
                        @RequestParam(required = false) String keyword,
                        @RequestParam(required = false) String relationship,
                        @RequestParam(required = false) String hasComment,
                        Model model) {
        addCommonAttributes(model);

        Long uid = currentUserId();
        User currentUser = userService.findById(uid);
        boolean isAdmin = currentUser != null && currentUser.isAdmin();
        Set<Long> friendIds = friendService.getFriendIds(uid);

        // 获取动态列表：普通用户看可见动态，管理员额外能看到被隐藏的（但不是PRIVATE的）
        List<Post> posts;
        if (isAdmin) {
            // 管理员可以看到所有非PRIVATE的动态（包括HIDDEN的）
            posts = postService.getAllPosts().stream()
                    .filter(p -> {
                        // PRIVATE的仅作者自己和管理员不能看（管理员不绕过仅自己可见）
                        if (p.getVisibility() == Visibility.PRIVATE && !p.getUserId().equals(uid)) {
                            return false;
                        }
                        // DRAFT只看自己的
                        if (p.getStatus() == PostStatus.DRAFT && !p.getUserId().equals(uid)) {
                            return false;
                        }
                        return true;
                    })
                    .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                    .collect(Collectors.toList());
        } else {
            posts = postService.getVisiblePosts(uid, friendIds);
        }

        // 筛选
        if (visibility != null && !visibility.isBlank()) {
            Visibility v = Visibility.valueOf(visibility);
            posts = posts.stream().filter(p -> p.getVisibility() == v).collect(Collectors.toList());
        }
        if (status != null && !status.isBlank()) {
            PostStatus s = PostStatus.valueOf(status);
            posts = posts.stream().filter(p -> p.getStatus() == s).collect(Collectors.toList());
        }
        if (authorId != null && !authorId.isBlank()) {
            Long aid = Long.valueOf(authorId);
            posts = posts.stream().filter(p -> p.getUserId().equals(aid)).collect(Collectors.toList());
        }
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.toLowerCase();
            posts = posts.stream().filter(p -> {
                User author = userService.findById(p.getUserId());
                boolean matchAuthor = author != null && (author.getDisplayName().toLowerCase().contains(kw)
                        || author.getClassName().toLowerCase().contains(kw));
                boolean matchContent = p.getContent().toLowerCase().contains(kw);
                boolean matchPhoto = p.getPhotoUrls().stream().anyMatch(url -> url.toLowerCase().contains(kw));
                return matchAuthor || matchContent || matchPhoto;
            }).collect(Collectors.toList());
        }
        if (relationship != null && !relationship.isBlank()) {
            posts = posts.stream().filter(p -> {
                String rel = friendService.getRelationship(uid, p.getUserId());
                return rel.equals(relationship);
            }).collect(Collectors.toList());
        }
        if (hasComment != null && !hasComment.isBlank()) {
            boolean wantComment = "yes".equals(hasComment);
            posts = posts.stream().filter(p -> {
                int cnt = commentService.getCommentsByPostId(p.getId()).size();
                return wantComment ? cnt > 0 : cnt == 0;
            }).collect(Collectors.toList());
        }

        List<Map<String, Object>> feedItems = new ArrayList<>();
        for (Post post : posts) {
            Map<String, Object> item = new HashMap<>();
            item.put("post", post);
            item.put("author", userService.findById(post.getUserId()));
            item.put("commentCount", commentService.getCommentsByPostId(post.getId()).size());
            item.put("likeCount", likeService.countByPostId(post.getId()));
            item.put("hasLiked", likeService.hasLiked(post.getId(), uid));
            // 管理员标记：是否是被隐藏的、是否缺照片
            item.put("adminHidden", isAdmin && post.getStatus() == PostStatus.HIDDEN && !post.getUserId().equals(uid));
            item.put("adminMissingPhoto", isAdmin && post.getContent().contains("照片") && post.getPhotoUrls().isEmpty());
            feedItems.add(item);
        }

        model.addAttribute("feedItems", feedItems);
        model.addAttribute("filterVisibility", visibility);
        model.addAttribute("filterStatus", status);
        model.addAttribute("filterAuthorId", authorId);
        model.addAttribute("filterKeyword", keyword);
        model.addAttribute("filterRelationship", relationship);
        model.addAttribute("filterHasComment", hasComment);
        return "index";
    }

    @GetMapping("/post/{id}")
    public String postDetail(@PathVariable Long id, Model model) {
        addCommonAttributes(model);

        Long uid = currentUserId();
        User currentUser = userService.findById(uid);
        boolean isAdmin = currentUser != null && currentUser.isAdmin();
        Post post = postService.findById(id);
        if (post == null) {
            model.addAttribute("postNotFound", true);
            return "post-detail";
        }

        model.addAttribute("postNotFound", false);

        Set<Long> friendIds = friendService.getFriendIds(uid);
        boolean visible = isPostVisible(post, uid, friendIds, isAdmin);

        User author = userService.findById(post.getUserId());

        model.addAttribute("post", post);
        model.addAttribute("author", author);
        model.addAttribute("isOwner", post.getUserId().equals(uid));
        model.addAttribute("isAdmin", isAdmin);

        if (!visible) {
            model.addAttribute("accessDenied", true);
            String reason = postService.getDeniedReason(post, uid, friendIds);
            model.addAttribute("deniedReason", reason);
            String relationship = friendService.getRelationship(uid, author.getId());
            model.addAttribute("authorRelationship", relationship);
            return "post-detail";
        }

        model.addAttribute("accessDenied", false);
        // 管理员看到的特殊标记
        model.addAttribute("adminHiddenMark", isAdmin && post.getStatus() == PostStatus.HIDDEN && !post.getUserId().equals(uid));
        model.addAttribute("adminMissingPhotoMark", isAdmin && post.getContent().contains("照片") && post.getPhotoUrls().isEmpty());

        List<Comment> comments = commentService.getCommentsByPostId(id);

        Map<Long, List<Map<String, Object>>> repliesMap = new LinkedHashMap<>();
        List<Map<String, Object>> topComments = new ArrayList<>();

        for (Comment c : comments) {
            Map<String, Object> item = new HashMap<>();
            item.put("comment", c);
            item.put("user", userService.findById(c.getUserId()));
            item.put("canDelete", c.getUserId().equals(uid));
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
    public String friends(@RequestParam(required = false) String keyword,
                          @RequestParam(required = false) String online,
                          @RequestParam(required = false) String group,
                          Model model) {
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

        // 好友搜索筛选
        String kw = (keyword != null && !keyword.isBlank()) ? keyword.toLowerCase() : null;

        List<User> filteredFriends = friends;
        if (kw != null) {
            filteredFriends = filteredFriends.stream().filter(u ->
                    u.getDisplayName().toLowerCase().contains(kw) || u.getClassName().toLowerCase().contains(kw)
            ).collect(Collectors.toList());
        }
        if ("true".equals(online) || "false".equals(online)) {
            boolean wantOnline = "true".equals(online);
            filteredFriends = filteredFriends.stream().filter(u -> u.isOnline() == wantOnline).collect(Collectors.toList());
        }

        // 好友申请搜索
        List<FriendRequest> filteredReceived = pendingRequests;
        List<FriendRequest> filteredSent = sentRequests;
        if (kw != null) {
            filteredReceived = filteredReceived.stream().filter(r -> {
                User from = userService.findById(r.getFromUserId());
                return from != null && (from.getDisplayName().toLowerCase().contains(kw) || from.getClassName().toLowerCase().contains(kw));
            }).collect(Collectors.toList());
            filteredSent = filteredSent.stream().filter(r -> {
                User to = userService.findById(r.getToUserId());
                return to != null && (to.getDisplayName().toLowerCase().contains(kw) || to.getClassName().toLowerCase().contains(kw));
            }).collect(Collectors.toList());
        }

        // 发现同学搜索
        List<Map<String, Object>> filteredDiscover = discoverUsers;
        if (kw != null) {
            filteredDiscover = filteredDiscover.stream().filter(item -> {
                User u = (User) item.get("user");
                return u.getDisplayName().toLowerCase().contains(kw) || u.getClassName().toLowerCase().contains(kw);
            }).collect(Collectors.toList());
        }
        if ("true".equals(online) || "false".equals(online)) {
            boolean wantOnline = "true".equals(online);
            filteredDiscover = filteredDiscover.stream().filter(item -> {
                User u = (User) item.get("user");
                return u.isOnline() == wantOnline;
            }).collect(Collectors.toList());
        }

        // 统计（分组筛选用）
        int friendCount = friends.size();
        int pendingReceivedCount = pendingRequests.size();
        int pendingSentCount = sentRequests.size();
        int strangerCount = discoverUsers.size();

        model.addAttribute("friends", filteredFriends);
        model.addAttribute("allFriends", friends);
        model.addAttribute("sentRequests", filteredSent);
        model.addAttribute("allSentRequests", sentRequests);
        model.addAttribute("receivedRequests", filteredReceived);
        model.addAttribute("allReceivedRequests", pendingRequests);
        model.addAttribute("discoverUsers", filteredDiscover);
        model.addAttribute("allDiscoverUsers", discoverUsers);
        model.addAttribute("filterKeyword", keyword);
        model.addAttribute("filterOnline", online);
        model.addAttribute("filterGroup", group);
        model.addAttribute("friendCount", friendCount);
        model.addAttribute("pendingReceivedCount", pendingReceivedCount);
        model.addAttribute("pendingSentCount", pendingSentCount);
        model.addAttribute("strangerCount", strangerCount);
        return "friends";
    }

    @GetMapping("/profile/{id}")
    public String profile(@PathVariable Long id, Model model) {
        addCommonAttributes(model);

        Long uid = currentUserId();
        User currentUser = userService.findById(uid);
        boolean isAdmin = currentUser != null && currentUser.isAdmin();
        User profileUser = userService.findById(id);
        if (profileUser == null) return "redirect:/";

        Set<Long> friendIds = friendService.getFriendIds(uid);
        List<Post> userPosts = postService.getVisiblePostsByUser(id, uid, friendIds);
        String relationship = friendService.getRelationship(uid, id);

        // 管理员额外能看到被隐藏的（非PRIVATE）
        List<Map<String, Object>> postItems = new ArrayList<>();
        for (Post post : userPosts) {
            Map<String, Object> item = new HashMap<>();
            item.put("post", post);
            item.put("commentCount", commentService.getCommentsByPostId(post.getId()).size());
            item.put("likeCount", likeService.countByPostId(post.getId()));
            item.put("adminHiddenMark", isAdmin && post.getStatus() == PostStatus.HIDDEN && !post.getUserId().equals(uid));
            item.put("adminMissingPhotoMark", isAdmin && post.getContent().contains("照片") && post.getPhotoUrls().isEmpty());
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

        // 管理员在他人主页也能看到该用户的隐藏动态（但不是PRIVATE）
        List<Map<String, Object>> adminHiddenItems = null;
        if (isAdmin && !id.equals(uid)) {
            List<Post> allTargetPosts = postService.getAllPostsByUser(id);
            adminHiddenItems = new ArrayList<>();
            for (Post post : allTargetPosts) {
                // 只展示普通用户看不到的隐藏动态
                if (post.getStatus() == PostStatus.HIDDEN && post.getVisibility() != Visibility.PRIVATE) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("post", post);
                    item.put("commentCount", commentService.getCommentsByPostId(post.getId()).size());
                    item.put("likeCount", likeService.countByPostId(post.getId()));
                    adminHiddenItems.add(item);
                }
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
        model.addAttribute("adminHiddenItems", adminHiddenItems);
        model.addAttribute("hasPhotos", hasPhotos);
        model.addAttribute("relationship", relationship);
        model.addAttribute("isSelf", id.equals(uid));
        model.addAttribute("totalPostCount", totalPostCount);
        model.addAttribute("profileFriendCount", profileFriendCount);
        model.addAttribute("pendingRequest", pendingReq);
        return "profile";
    }

    @GetMapping("/debug")
    public String debug(Model model) {
        addCommonAttributes(model);
        Long uid = currentUserId();
        Set<Long> friendIds = friendService.getFriendIds(uid);
        User currentUser = userService.findById(uid);
        boolean isAdmin = currentUser != null && currentUser.isAdmin();

        List<Post> allPosts = postService.getVisiblePosts(uid, friendIds);
        List<Post> myPosts = postService.getAllPostsByUser(uid);

        // 管理员可见的隐藏动态
        List<Post> adminHiddenPosts = Collections.emptyList();
        if (isAdmin) {
            adminHiddenPosts = postService.getAllPosts().stream()
                    .filter(p -> p.getStatus() == PostStatus.HIDDEN && !p.getUserId().equals(uid))
                    .collect(Collectors.toList());
        }

        model.addAttribute("allVisiblePosts", allPosts);
        model.addAttribute("myPosts", myPosts);
        model.addAttribute("adminHiddenPosts", adminHiddenPosts);
        return "debug";
    }

    private boolean isPostVisible(Post p, Long currentUserId, Set<Long> friendIds, boolean isAdmin) {
        // 管理员可以看到HIDDEN的，但不能看PRIVATE（非自己的）
        if (p.getStatus() == PostStatus.HIDDEN) {
            if (p.getUserId().equals(currentUserId)) return true;
            if (isAdmin && p.getVisibility() != Visibility.PRIVATE) return true;
            return false;
        }
        if (p.getStatus() == PostStatus.DRAFT) {
            return p.getUserId().equals(currentUserId);
        }
        return switch (p.getVisibility()) {
            case PUBLIC -> true;
            case FRIENDS -> p.getUserId().equals(currentUserId) || friendIds.contains(p.getUserId());
            case PRIVATE -> p.getUserId().equals(currentUserId);
        };
    }
}
