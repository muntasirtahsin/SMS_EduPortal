package com.eduportal.models;

public class Enrollment {
    private int id;
    private String studentId;
    private String studentName;
    private String courseId;
    private String courseName;
    private int credits;
    private String semester;
    private String resultStatus;
    private String teacherName;
    private String attemptType;
    private int incompleteDueSemester;
    private boolean midAbsent;
    private boolean finalAbsent;
    private int studentCurrentSemester;
    private Double attendance;
    private Double q1;
    private Double q2;
    private Double assignment;
    private Double mid;
    private Double finalExam;

    public Enrollment() {}

    public Enrollment(String studentId, String courseId, String semester) {
        this.studentId = studentId;
        this.courseId = courseId;
        this.semester = semester;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getCourseId() { return courseId; }
    public void setCourseId(String courseId) { this.courseId = courseId; }

    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }

    public int getCredits() { return credits; }
    public void setCredits(int credits) { this.credits = credits; }

    public String getSemester() { return semester; }
    public void setSemester(String semester) { this.semester = semester; }

    public String getPublicationStatus() { return resultStatus; }
    public void setPublicationStatus(String resultStatus) { this.resultStatus = resultStatus; }

    public String getTeacherName() { return teacherName; }
    public void setTeacherName(String teacherName) { this.teacherName = teacherName; }

    public String getAttemptType() { return attemptType; }
    public void setAttemptType(String attemptType) { this.attemptType = attemptType; }

    public int getIncompleteDueSemester() { return incompleteDueSemester; }
    public void setIncompleteDueSemester(int incompleteDueSemester) { this.incompleteDueSemester = incompleteDueSemester; }

    public boolean isMidAbsent() { return midAbsent; }
    public void setMidAbsent(boolean midAbsent) { this.midAbsent = midAbsent; }

    public boolean isFinalAbsent() { return finalAbsent; }
    public void setFinalAbsent(boolean finalAbsent) { this.finalAbsent = finalAbsent; }

    public int getStudentCurrentSemester() { return studentCurrentSemester; }
    public void setStudentCurrentSemester(int studentCurrentSemester) { this.studentCurrentSemester = studentCurrentSemester; }

    public Double getAttendance() { return attendance; }
    public void setAttendance(Double attendance) { this.attendance = attendance; }

    public Double getQ1() { return q1; }
    public void setQ1(Double q1) { this.q1 = q1; }

    public Double getQ2() { return q2; }
    public void setQ2(Double q2) { this.q2 = q2; }

    public Double getAssignment() { return assignment; }
    public void setAssignment(Double assignment) { this.assignment = assignment; }

    public Double getMid() { return mid; }
    public void setMid(Double mid) { this.mid = mid; }

    public Double getFinalExam() { return finalExam; }
    public void setFinalExam(Double finalExam) { this.finalExam = finalExam; }

    public double getTotalMarks() {
        double sum = 0.0;
        if (attendance != null) sum += attendance;
        if (q1 != null) sum += q1;
        if (q2 != null) sum += q2;
        if (assignment != null) sum += assignment;
        if (mid != null) sum += mid;
        if (finalExam != null) sum += finalExam;
        return sum;
    }

    public String getResultStatus() {
        return getTotalMarks() >= 40 ? "Pass" : "Fail";
    }

    public int getWaiverCredits() {
        double total = getTotalMarks();
        if (total >= 95) return 6;
        else if (total >= 85) return 3;
        return 0;
    }

    public double getGradePoint() {
        double total = getTotalMarks();
        if (total >= 95) return 4.0;
        else if (total >= 85) return 3.7;
        else if (total >= 80) return 3.3;
        else if (total >= 75) return 3.0;
        else if (total >= 70) return 2.7;
        else if (total >= 65) return 2.3;
        else if (total >= 60) return 2.0;
        else if (total >= 55) return 1.7;
        else if (total >= 50) return 1.3;
        else if (total >= 40) return 1.0;
        else return 0.0;
    }

    public String getLetterGrade() {
        double total = getTotalMarks();
        if (total >= 95) return "A+";
        else if (total >= 85) return "A";
        else if (total >= 80) return "B+";
        else if (total >= 75) return "B";
        else if (total >= 70) return "C+";
        else if (total >= 65) return "C";
        else if (total >= 60) return "D+";
        else if (total >= 55) return "D";
        else return "F";
    }
}
