package com.eduportal.models;

public class NewsTicker {
    private int id;
    private String content;
    private String targetRole = "all"; // all, student, teacher
    private int isActive = 1;
    private String createdAt;

    public NewsTicker() {}

    public NewsTicker(String content, String targetRole, int isActive) {
        this.content = content;
        this.targetRole = targetRole;
        this.isActive = isActive;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getTargetRole() { return targetRole; }
    public void setTargetRole(String targetRole) { this.targetRole = targetRole; }

    public int getIsActive() { return isActive; }
    public boolean isActive() { return isActive == 1; }
    public void setIsActive(int isActive) { this.isActive = isActive; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
