package com.campus.dto;

public class StatsDTO {
    private int postCount;            // 我的PUBLISHED动态数
    private int friendCount;
    private int pendingRequestCount;
    private int visiblePhotoCount;    // 当前视角可见照片总数
    private int publicPhotoCount;     // 公开可见照片数
    private int friendsOnlyPhotoCount;// 仅好友可见照片数
    private int privatePhotoCount;    // 仅自己可见照片数
    private int commentCount;         // 当前用户可见动态的评论/回复总数
    private int visiblePostCount;     // 当前视角可见动态总数

    public StatsDTO() {}

    public int getPostCount() { return postCount; }
    public void setPostCount(int postCount) { this.postCount = postCount; }
    public int getFriendCount() { return friendCount; }
    public void setFriendCount(int friendCount) { this.friendCount = friendCount; }
    public int getPendingRequestCount() { return pendingRequestCount; }
    public void setPendingRequestCount(int pendingRequestCount) { this.pendingRequestCount = pendingRequestCount; }
    public int getVisiblePhotoCount() { return visiblePhotoCount; }
    public void setVisiblePhotoCount(int visiblePhotoCount) { this.visiblePhotoCount = visiblePhotoCount; }
    public int getPublicPhotoCount() { return publicPhotoCount; }
    public void setPublicPhotoCount(int publicPhotoCount) { this.publicPhotoCount = publicPhotoCount; }
    public int getFriendsOnlyPhotoCount() { return friendsOnlyPhotoCount; }
    public void setFriendsOnlyPhotoCount(int friendsOnlyPhotoCount) { this.friendsOnlyPhotoCount = friendsOnlyPhotoCount; }
    public int getPrivatePhotoCount() { return privatePhotoCount; }
    public void setPrivatePhotoCount(int privatePhotoCount) { this.privatePhotoCount = privatePhotoCount; }
    public int getCommentCount() { return commentCount; }
    public void setCommentCount(int commentCount) { this.commentCount = commentCount; }
    public int getVisiblePostCount() { return visiblePostCount; }
    public void setVisiblePostCount(int visiblePostCount) { this.visiblePostCount = visiblePostCount; }
}
