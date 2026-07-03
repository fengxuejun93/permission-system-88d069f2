package com.campus.service;

import com.campus.dto.StatsDTO;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class StatsService {

    private final PostService postService;
    private final FriendService friendService;

    public StatsService(PostService postService, FriendService friendService) {
        this.postService = postService;
        this.friendService = friendService;
    }

    public StatsDTO getStats(Long currentUserId, Set<Long> friendIds) {
        int postCount = postService.countPostsByUser(currentUserId);
        int friendCount = friendService.getFriendCount(currentUserId);
        int pendingRequestCount = friendService.getPendingRequests(currentUserId).size();
        int visiblePhotoCount = postService.countVisiblePhotos(currentUserId, friendIds);

        return new StatsDTO(postCount, friendCount, pendingRequestCount, visiblePhotoCount);
    }
}
