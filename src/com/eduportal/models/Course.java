package com.eduportal.models;

public class Course {
    private String courseId;
    private String courseName;
    private int credits;
    private String teacherId;
    private String teacherName;
    private String semester;
    private int targetSemester = 1;

    public Course() {}

    public Course(String courseId, String courseName, int credits, String teacherId, String semester, int targetSemester) {
        this.courseId = courseId;
        this.courseName = courseName;
        this.credits = credits;
        this.teacherId = teacherId;
        this.semester = semester;
        this.targetSemester = targetSemester;
    }

    public String getCourseId() { return courseId; }
    public void setCourseId(String courseId) { this.courseId = courseId; }

    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }

    public int getCredits() { return credits; }
    public void setCredits(int credits) { this.credits = credits; }

    public String getTeacherId() { return teacherId; }
    public void setTeacherId(String teacherId) { this.teacherId = teacherId; }

    public String getTeacherName() { return teacherName; }
    public void setTeacherName(String teacherName) { this.teacherName = teacherName; }

    public String getSemester() { return semester; }
    public void setSemester(String semester) { this.semester = semester; }

    public int getTargetSemester() { return targetSemester; }
    public void setTargetSemester(int targetSemester) { this.targetSemester = targetSemester; }

    @Override
    public String toString() {
        return courseId + " - " + courseName + " (Sem " + targetSemester + ", " + credits + " Credits)";
    }
}
