package com.campus.model;

import java.time.LocalDateTime;

public class Comment {
    private Long id;
    private Long postId;
    private Long userId;
    private Long replyToId;
    private String content;
    private LocalDateTime createdAt;

    public Comment() {}

    public Comment(Long id, Long postId, Long userId, Long replyToId, String content) {
        this.id = id;
        this.postId = postId;
        this.userId = userId;
        this.replyToId = replyToId;
        this.content = content;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getPostId() { return postId; }
    public void setPostId(Long postId) { this.postId = postId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getReplyToId() { return replyToId; }
    public void setReplyToId(Long replyToId) { this.replyToId = replyToId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
