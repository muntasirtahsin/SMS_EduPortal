package com.eduportal.models;

public class CourseApplication {
    private int id;
    private String courseId;
    private String courseName;
    private String teacherId;
    private String teacherName;
    private String status;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getCourseId() { return courseId; }
    public void setCourseId(String courseId) { this.courseId = courseId; }
    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }
    public String getTeacherId() { return teacherId; }
    public void setTeacherId(String teacherId) { this.teacherId = teacherId; }
    public String getTeacherName() { return teacherName; }
    public void setTeacherName(String teacherName) { this.teacherName = teacherName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
