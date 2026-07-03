package com.campus.service;

import com.campus.dto.StatsDTO;
import com.campus.model.Visibility;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class StatsService {

    private final PostService postService;
    private final FriendService friendService;
    private final CommentService commentService;

    public StatsService(PostService postService, FriendService friendService, CommentService commentService) {
        this.postService = postService;
        this.friendService = friendService;
        this.commentService = commentService;
    }

    public StatsDTO getStats(Long currentUserId, Set<Long> friendIds) {
        StatsDTO stats = new StatsDTO();
        stats.setPostCount(postService.countPostsByUser(currentUserId));
        stats.setFriendCount(friendService.getFriendCount(currentUserId));
        stats.setPendingRequestCount(friendService.getPendingRequests(currentUserId).size());
        stats.setVisiblePhotoCount(postService.countVisiblePhotos(currentUserId, friendIds));
        stats.setPublicPhotoCount(postService.countPhotosByVisibility(Visibility.PUBLIC, currentUserId, friendIds));
        stats.setFriendsOnlyPhotoCount(postService.countPhotosByVisibility(Visibility.FRIENDS, currentUserId, friendIds));
        stats.setPrivatePhotoCount(postService.countPhotosByVisibility(Visibility.PRIVATE, currentUserId, friendIds));
        stats.setVisiblePostCount(postService.countVisiblePosts(currentUserId, friendIds));
        stats.setCommentCount(commentService.countCommentsForVisiblePosts(currentUserId, friendIds, postService));
        return stats;
    }
}
