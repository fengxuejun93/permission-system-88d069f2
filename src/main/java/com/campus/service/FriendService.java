package com.campus.service;

import com.campus.model.FriendRequest;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
public class FriendService {
    private final Map<Long, FriendRequest> requests = new LinkedHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(100);

    public FriendRequest addRequest(Long fromUserId, Long toUserId) {
        // 检查是否已存在
        for (FriendRequest r : requests.values()) {
            if ((r.getFromUserId().equals(fromUserId) && r.getToUserId().equals(toUserId))
                    || (r.getFromUserId().equals(toUserId) && r.getToUserId().equals(fromUserId))) {
                if (r.getStatus() == FriendRequest.Status.ACCEPTED) return r;
                if (r.getStatus() == FriendRequest.Status.PENDING) return r;
            }
        }
        FriendRequest req = new FriendRequest(idGenerator.incrementAndGet(), fromUserId, toUserId, FriendRequest.Status.PENDING);
        requests.put(req.getId(), req);
        return req;
    }

    public FriendRequest acceptRequest(Long requestId) {
        FriendRequest req = requests.get(requestId);
        if (req != null && req.getStatus() == FriendRequest.Status.PENDING) {
            req.setStatus(FriendRequest.Status.ACCEPTED);
        }
        return req;
    }

    public FriendRequest rejectRequest(Long requestId) {
        FriendRequest req = requests.get(requestId);
        if (req != null && req.getStatus() == FriendRequest.Status.PENDING) {
            req.setStatus(FriendRequest.Status.REJECTED);
        }
        return req;
    }

    /**
     * 获取某用户的所有好友ID
     */
    public Set<Long> getFriendIds(Long userId) {
        Set<Long> friends = new HashSet<>();
        for (FriendRequest r : requests.values()) {
            if (r.getStatus() == FriendRequest.Status.ACCEPTED) {
                if (r.getFromUserId().equals(userId)) friends.add(r.getToUserId());
                else if (r.getToUserId().equals(userId)) friends.add(r.getFromUserId());
            }
        }
        return friends;
    }

    public int getFriendCount(Long userId) {
        return getFriendIds(userId).size();
    }

    /**
     * 获取发给某用户的待处理申请
     */
    public List<FriendRequest> getPendingRequests(Long userId) {
        return requests.values().stream()
                .filter(r -> r.getToUserId().equals(userId) && r.getStatus() == FriendRequest.Status.PENDING)
                .collect(Collectors.toList());
    }

    /**
     * 获取某用户发出的待处理申请
     */
    public List<FriendRequest> getSentRequests(Long userId) {
        return requests.values().stream()
                .filter(r -> r.getFromUserId().equals(userId) && r.getStatus() == FriendRequest.Status.PENDING)
                .collect(Collectors.toList());
    }

    public List<FriendRequest> getAllRequests() {
        return new ArrayList<>(requests.values());
    }

    public FriendRequest findById(Long id) {
        return requests.get(id);
    }

    /**
     * 获取两个用户之间的关系状态
     */
    public String getRelationship(Long currentUserId, Long targetUserId) {
        if (currentUserId.equals(targetUserId)) return "SELF";
        Set<Long> friends = getFriendIds(currentUserId);
        if (friends.contains(targetUserId)) return "FRIEND";

        for (FriendRequest r : requests.values()) {
            if (r.getStatus() == FriendRequest.Status.PENDING) {
                if (r.getFromUserId().equals(currentUserId) && r.getToUserId().equals(targetUserId)) return "SENT";
                if (r.getFromUserId().equals(targetUserId) && r.getToUserId().equals(currentUserId)) return "RECEIVED";
            }
        }
        return "NONE";
    }
}
