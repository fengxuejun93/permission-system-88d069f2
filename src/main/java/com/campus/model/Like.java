package com.campus.model;

public class Like {
    private Long id;
    private Long postId;
    private Long userId;

    public Like() {}

    public Like(Long id, Long postId, Long userId) {
        this.id = id;
        this.postId = postId;
        this.userId = userId;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getPostId() { return postId; }
    public void setPostId(Long postId) { this.postId = postId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Like)) return false;
        Like like = (Like) o;
        return postId.equals(like.postId) && userId.equals(like.userId);
    }

    @Override
    public int hashCode() {
        return 31 * postId.hashCode() + userId.hashCode();
    }
}
