package com.campus.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class User {
    private Long id;
    private String username;
    private String displayName;
    private String avatar;
    private String className;
    private boolean online;
    private String relationshipStatus;
    private String bio;
    private UserRole role;
    private LocalDateTime createdAt;

    public User() {}

    public User(Long id, String username, String displayName, String avatar, String className,
                boolean online, String relationshipStatus, String bio) {
        this.id = id;
        this.username = username;
        this.displayName = displayName;
        this.avatar = avatar;
        this.className = className;
        this.online = online;
        this.relationshipStatus = relationshipStatus;
        this.bio = bio;
        this.role = UserRole.STUDENT;
        this.createdAt = LocalDateTime.now();
    }

    public User(Long id, String username, String displayName, String avatar, String className,
                boolean online, String relationshipStatus, String bio, UserRole role) {
        this.id = id;
        this.username = username;
        this.displayName = displayName;
        this.avatar = avatar;
        this.className = className;
        this.online = online;
        this.relationshipStatus = relationshipStatus;
        this.bio = bio;
        this.role = role != null ? role : UserRole.STUDENT;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
    public boolean isOnline() { return online; }
    public void setOnline(boolean online) { this.online = online; }
    public String getRelationshipStatus() { return relationshipStatus; }
    public void setRelationshipStatus(String relationshipStatus) { this.relationshipStatus = relationshipStatus; }
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public boolean isAdmin() {
        return role == UserRole.SYSTEM_ADMIN || role == UserRole.CLASS_ADMIN;
    }

    public boolean isSystemAdmin() {
        return role == UserRole.SYSTEM_ADMIN;
    }
}
