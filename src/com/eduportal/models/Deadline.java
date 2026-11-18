package com.eduportal.models;

public class Deadline {
    private int id;
    private String deadlineKey;
    private String title;
    private String startDate;
    private String endDate;
    private int isActive = 1;

    public Deadline() {}

    public Deadline(String deadlineKey, String title, String startDate, String endDate, int isActive) {
        this.deadlineKey = deadlineKey;
        this.title = title;
        this.startDate = startDate;
        this.endDate = endDate;
        this.isActive = isActive;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getDeadlineKey() { return deadlineKey; }
    public void setDeadlineKey(String deadlineKey) { this.deadlineKey = deadlineKey; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }

    public int getIsActive() { return isActive; }
    public void setIsActive(int isActive) { this.isActive = isActive; }
    public boolean isActive() { return isActive == 1; }
}
