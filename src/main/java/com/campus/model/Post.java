package com.campus.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Post {
    private Long id;
    private Long userId;
    private String content;
    private Visibility visibility;
    private List<String> photoUrls;
    private LocalDateTime createdAt;

    public Post() {
        this.photoUrls = new ArrayList<>();
    }

    public Post(Long id, Long userId, String content, Visibility visibility, List<String> photoUrls) {
        this.id = id;
        this.userId = userId;
        this.content = content;
        this.visibility = visibility;
        this.photoUrls = photoUrls != null ? photoUrls : new ArrayList<>();
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Visibility getVisibility() { return visibility; }
    public void setVisibility(Visibility visibility) { this.visibility = visibility; }
    public List<String> getPhotoUrls() { return photoUrls; }
    public void setPhotoUrls(List<String> photoUrls) { this.photoUrls = photoUrls; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
