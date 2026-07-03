package com.campus.dto;

public class StatsDTO {
    private int postCount;
    private int friendCount;
    private int pendingRequestCount;
    private int visiblePhotoCount;

    public StatsDTO() {}

    public StatsDTO(int postCount, int friendCount, int pendingRequestCount, int visiblePhotoCount) {
        this.postCount = postCount;
        this.friendCount = friendCount;
        this.pendingRequestCount = pendingRequestCount;
        this.visiblePhotoCount = visiblePhotoCount;
    }

    public int getPostCount() { return postCount; }
    public void setPostCount(int postCount) { this.postCount = postCount; }
    public int getFriendCount() { return friendCount; }
    public void setFriendCount(int friendCount) { this.friendCount = friendCount; }
    public int getPendingRequestCount() { return pendingRequestCount; }
    public void setPendingRequestCount(int pendingRequestCount) { this.pendingRequestCount = pendingRequestCount; }
    public int getVisiblePhotoCount() { return visiblePhotoCount; }
    public void setVisiblePhotoCount(int visiblePhotoCount) { this.visiblePhotoCount = visiblePhotoCount; }
}
