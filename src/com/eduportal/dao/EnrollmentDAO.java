package com.eduportal.dao;

import com.eduportal.models.Enrollment;
import com.eduportal.utils.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EnrollmentDAO {

    public List<Enrollment> getEnrollmentsByStudent(String studentId, String semester) {
        List<Enrollment> list = new ArrayList<>();
        boolean filterSem = (semester != null && !"All".equalsIgnoreCase(semester));
        String query = "SELECT e.*, c.course_name, c.credits, u.name AS student_name, t.name AS teacher_name FROM enrollments e " +
                    "JOIN courses c ON e.course_id = c.course_id " +
                    "JOIN users u ON e.student_id = u.id " +
                    "LEFT JOIN users t ON c.teacher_id = t.id " +
                    "WHERE e.student_id = ? " + (filterSem ? "AND e.semester = ? " : "") + "ORDER BY e.course_id";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, studentId);
            if (filterSem) pstmt.setString(2, semester);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                list.add(mapEnrollment(rs));
            }
        } catch (SQLException e) {
            System.err.println("[EnrollmentDAO] getEnrollmentsByStudent Error: " + e.getMessage());
        }
        return list;
    }

    public List<Enrollment> getEnrollmentsByCourse(String courseId) {
        List<Enrollment> list = new ArrayList<>();
        String query = "SELECT e.*, c.course_name, c.credits, u.name AS student_name, t.name AS teacher_name FROM enrollments e " +
                "JOIN courses c ON e.course_id = c.course_id " +
                "JOIN users u ON e.student_id = u.id " +
                "LEFT JOIN users t ON c.teacher_id = t.id " +
                "WHERE e.course_id = ? ORDER BY e.student_id";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, courseId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                list.add(mapEnrollment(rs));
            }
        } catch (SQLException e) {
            System.err.println("[EnrollmentDAO] getEnrollmentsByCourse Error: " + e.getMessage());
        }
        return list;
    }

    public Enrollment getEnrollmentForTeacherReview(int enrollmentId, String teacherId) {
        String query = "SELECT e.*, c.course_name, c.credits, u.name AS student_name, t.name AS teacher_name " +
                "FROM enrollments e JOIN courses c ON e.course_id = c.course_id " +
                "JOIN users u ON e.student_id = u.id LEFT JOIN users t ON c.teacher_id = t.id " +
                "WHERE e.id = ? AND c.teacher_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, enrollmentId);
            pstmt.setString(2, teacherId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? mapEnrollment(rs) : null;
            }
        } catch (SQLException e) {
            System.err.println("[EnrollmentDAO] getEnrollmentForTeacherReview Error: " + e.getMessage());
            return null;
        }
    }

    public List<Enrollment> getApprovedEnrollmentsByStudent(String studentId, String semester) {
        List<Enrollment> list = new ArrayList<>();
        boolean filterSem = semester != null && !"All".equalsIgnoreCase(semester);
        String query = "SELECT e.*, c.course_name, c.credits, u.name AS student_name, t.name AS teacher_name " +
                "FROM enrollments e JOIN courses c ON e.course_id = c.course_id " +
                "JOIN users u ON e.student_id = u.id LEFT JOIN users t ON c.teacher_id = t.id " +
                "WHERE e.student_id = ? AND e.result_status = 'approved' " +
                (filterSem ? "AND e.semester = ? " : "") + "ORDER BY e.course_id";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, studentId);
            if (filterSem) pstmt.setString(2, semester);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) list.add(mapEnrollment(rs));
            }
        } catch (SQLException e) {
            System.err.println("[EnrollmentDAO] getApprovedEnrollmentsByStudent Error: " + e.getMessage());
        }
        return list;
    }

    public List<Enrollment> getResultsForAdmin() {
        List<Enrollment> list = new ArrayList<>();
        String query = "SELECT e.*, c.course_name, c.credits, u.name AS student_name, t.name AS teacher_name " +
                "FROM enrollments e JOIN courses c ON e.course_id = c.course_id " +
                "JOIN users u ON e.student_id = u.id LEFT JOIN users t ON c.teacher_id = t.id " +
                "WHERE e.result_status IN ('pending', 'review_requested', 'approved') " +
                "ORDER BY CASE e.result_status WHEN 'pending' THEN 0 WHEN 'review_requested' THEN 1 " +
                "WHEN 'approved' THEN 2 ELSE 3 END, e.course_id, e.student_id";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) list.add(mapEnrollment(rs));
        } catch (SQLException e) {
            System.err.println("[EnrollmentDAO] getResultsForAdmin Error: " + e.getMessage());
        }
        return list;
    }

    public boolean setResultStatus(int enrollmentId, String status, String adminId) {
        if (!"approved".equals(status) && !"review_requested".equals(status)) return false;
        String allowedStatuses = "approved".equals(status)
                ? "'pending', 'review_requested'"
                : "'pending', 'review_requested', 'approved'";
        String selectQuery = "SELECT e.student_id, c.course_id, c.course_name FROM enrollments e " +
                "JOIN courses c ON e.course_id = c.course_id WHERE e.id = ?";
        String updateQuery = "UPDATE enrollments SET result_status = ? WHERE id = ? AND result_status IN ("
                + allowedStatuses + ")";
        String messageQuery = "INSERT INTO messages (sender_id, receiver_id, subject, body) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection()) {
            boolean originalAutoCommit = conn.getAutoCommit();
            try {
                conn.setAutoCommit(false);
                String studentId = null;
                String courseId = null;
                String courseName = null;
                try (PreparedStatement select = conn.prepareStatement(selectQuery)) {
                    select.setInt(1, enrollmentId);
                    try (ResultSet rs = select.executeQuery()) {
                        if (!rs.next()) {
                            conn.rollback();
                            return false;
                        }
                        studentId = rs.getString("student_id");
                        courseId = rs.getString("course_id");
                        courseName = rs.getString("course_name");
                    }
                }
                try (PreparedStatement update = conn.prepareStatement(updateQuery)) {
                    update.setString(1, status);
                    update.setInt(2, enrollmentId);
                    if (update.executeUpdate() != 1) {
                        conn.rollback();
                        return false;
                    }
                }
                if ("approved".equals(status)) {
                    try (PreparedStatement message = conn.prepareStatement(messageQuery)) {
                        message.setString(1, adminId);
                        message.setString(2, studentId);
                        message.setString(3, "Result published: " + courseId);
                        message.setString(4, "Your result for " + courseId + " - " + courseName
                                + " has been approved and published. You can view it in your Results page.");
                        message.executeUpdate();
                    }
                }
                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(originalAutoCommit);
            }
        } catch (SQLException e) {
            System.err.println("[EnrollmentDAO] setResultStatus Error: " + e.getMessage());
            return false;
        }
    }

    public boolean requestTeacherReview(int enrollmentId, String adminId) {
        String selectQuery = "SELECT e.student_id, student.name AS student_name, c.course_id, " +
                "c.course_name, c.teacher_id, e.result_status FROM enrollments e " +
                "JOIN users student ON e.student_id = student.id " +
                "JOIN courses c ON e.course_id = c.course_id WHERE e.id = ?";
        String updateQuery = "UPDATE enrollments SET result_status = 'review_requested' " +
                "WHERE id = ? AND result_status IN ('pending', 'approved')";
        String messageQuery = "INSERT INTO messages (sender_id, receiver_id, subject, body) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection()) {
            boolean originalAutoCommit = conn.getAutoCommit();
            try {
                conn.setAutoCommit(false);
                String teacherId;
                String studentName;
                String studentId;
                String courseId;
                String courseName;
                try (PreparedStatement select = conn.prepareStatement(selectQuery)) {
                    select.setInt(1, enrollmentId);
                    try (ResultSet rs = select.executeQuery()) {
                        if (!rs.next() || rs.getString("teacher_id") == null) {
                            conn.rollback();
                            return false;
                        }
                        teacherId = rs.getString("teacher_id");
                        studentName = rs.getString("student_name");
                        studentId = rs.getString("student_id");
                        courseId = rs.getString("course_id");
                        courseName = rs.getString("course_name");
                    }
                }
                try (PreparedStatement update = conn.prepareStatement(updateQuery)) {
                    update.setInt(1, enrollmentId);
                    if (update.executeUpdate() != 1) {
                        conn.rollback();
                        return false;
                    }
                }
                try (PreparedStatement message = conn.prepareStatement(messageQuery)) {
                    message.setString(1, adminId);
                    message.setString(2, teacherId);
                    message.setString(3, "Result review requested: " + courseId);
                    message.setString(4, "An administrator requested a review of " + studentName + " ("
                            + studentId + ") for " + courseId + " - " + courseName
                            + ". Open Enter Marks to review and update this result."
                            + "\n\n[[RESULT_REVIEW:" + enrollmentId + "]]");
                    message.executeUpdate();
                }
                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(originalAutoCommit);
            }
        } catch (SQLException e) {
            System.err.println("[EnrollmentDAO] requestTeacherReview Error: " + e.getMessage());
            return false;
        }
    }

    public boolean isStudentEnrolledInSemester(String studentId, String courseId, String semester) {
        String sem = (semester != null && !semester.isEmpty()) ? semester : "Spring 2026";
        String query = "SELECT COUNT(*) FROM enrollments WHERE student_id = ? AND course_id = ? AND semester = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, studentId);
            pstmt.setString(2, courseId);
            pstmt.setString(3, sem);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("[EnrollmentDAO] isStudentEnrolledInSemester Error: " + e.getMessage());
        }
        return false;
    }

    public Enrollment getLatestEnrollment(String studentId, String courseId, int currentSemester) {
        expireIncompleteEnrollments(studentId, currentSemester);
        String query = "SELECT e.*, c.course_name, c.credits, u.name AS student_name, t.name AS teacher_name " +
                "FROM enrollments e JOIN courses c ON e.course_id = c.course_id " +
                "JOIN users u ON e.student_id = u.id LEFT JOIN users t ON c.teacher_id = t.id " +
                "WHERE e.student_id = ? AND e.course_id = ? ORDER BY e.id DESC LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, studentId);
            pstmt.setString(2, courseId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? mapEnrollment(rs) : null;
            }
        } catch (SQLException e) {
            System.err.println("[EnrollmentDAO] getLatestEnrollment Error: " + e.getMessage());
            return null;
        }
    }

    public void expireIncompleteEnrollments(String studentId, int currentSemester) {
        String query = "UPDATE enrollments SET attempt_type = 'retake', result_status = 'expired_incomplete', " +
                "incomplete_due_semester = NULL WHERE student_id = ? AND attempt_type = 'incomplete' " +
                "AND incomplete_due_semester < ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, studentId);
            pstmt.setInt(2, currentSemester);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[EnrollmentDAO] expireIncompleteEnrollments Error: " + e.getMessage());
        }
    }

    public List<String> getSemestersForStudent(String studentId) {
        List<String> semesters = new ArrayList<>();
        String query = "SELECT semester FROM enrollments WHERE student_id = ? " +
                "GROUP BY semester ORDER BY MAX(id) DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, studentId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) semesters.add(rs.getString("semester"));
            }
        } catch (SQLException e) {
            System.err.println("[EnrollmentDAO] getSemestersForStudent Error: " + e.getMessage());
        }
        return semesters;
    }

    public boolean enrollStudent(String studentId, String courseId, String semester) {
        return enrollCourse(studentId, courseId, semester, "regular", 1);
    }

    public boolean enrollCourse(String studentId, String courseId, String semester,
                                String attemptType, int currentSemester) {
        String sem = (semester != null && !semester.isEmpty()) ? semester : "Spring 2026";
        if (isStudentEnrolledInSemester(studentId, courseId, sem)) return false;
        Enrollment previous = getLatestEnrollment(studentId, courseId, currentSemester);
        String type = attemptType == null ? "" : attemptType.trim().toLowerCase();
        if (previous == null) {
            if (!"regular".equals(type)) return false;
        } else if ("incomplete".equals(type)) {
            if (!"incomplete".equals(previous.getAttemptType())
                    || (!previous.isMidAbsent() && !previous.isFinalAbsent())
                    || previous.getIncompleteDueSemester() < currentSemester) return false;
        } else if ("retake".equals(type) || "improvement".equals(type)) {
            if ("incomplete".equals(previous.getAttemptType())
                    && (previous.isMidAbsent() || previous.isFinalAbsent())
                    && previous.getIncompleteDueSemester() >= currentSemester) return false;
            if (!"approved".equals(previous.getPublicationStatus())
                    && !"expired_incomplete".equals(previous.getPublicationStatus())) return false;
        } else {
            return false;
        }
        String query = "INSERT INTO enrollments (student_id, course_id, semester, result_status, attempt_type, " +
                "incomplete_due_semester, attendance, q1, q2, assignment, mid, final, mid_absent, final_absent) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, studentId);
            pstmt.setString(2, courseId);
            pstmt.setString(3, sem);
            pstmt.setString(4, "incomplete".equals(type) ? "incomplete" : "draft");
            pstmt.setString(5, type);
            if ("incomplete".equals(type)) pstmt.setInt(6, previous.getIncompleteDueSemester());
            else pstmt.setNull(6, Types.INTEGER);
            boolean carryMarks = "incomplete".equals(type) || "improvement".equals(type);
            pstmt.setDouble(7, carryMarks ? valueOrZero(previous.getAttendance()) : 0);
            pstmt.setDouble(8, carryMarks ? valueOrZero(previous.getQ1()) : 0);
            pstmt.setDouble(9, carryMarks ? valueOrZero(previous.getQ2()) : 0);
            pstmt.setDouble(10, carryMarks ? valueOrZero(previous.getAssignment()) : 0);
            pstmt.setDouble(11, carryMarks ? valueOrZero(previous.getMid()) : 0);
            pstmt.setDouble(12, carryMarks ? valueOrZero(previous.getFinalExam()) : 0);
            pstmt.setInt(13, carryMarks && previous.isMidAbsent() ? 1 : 0);
            pstmt.setInt(14, carryMarks && previous.isFinalAbsent() ? 1 : 0);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[EnrollmentDAO] enrollCourse Error: " + e.getMessage());
            return false;
        }
    }

    public boolean unenrollStudent(String studentId, String courseId) {
        String query = "DELETE FROM enrollments WHERE student_id = ? AND course_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, studentId);
            pstmt.setString(2, courseId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[EnrollmentDAO] unenrollStudent Error: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteEnrollmentById(int enrollmentId) {
        String query = "DELETE FROM enrollments WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, enrollmentId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[EnrollmentDAO] deleteEnrollmentById Error: " + e.getMessage());
            return false;
        }
    }

    public boolean updateMarks(int enrollmentId, String attendanceText, String q1Text, String q2Text,
                               String assignmentText, String midText, String finalText, String teacherId) {
        boolean absentMid = "absent".equalsIgnoreCase(midText == null ? "" : midText.trim());
        boolean absentFinal = "absent".equalsIgnoreCase(finalText == null ? "" : finalText.trim());
        Enrollment enrollment = getEnrollmentForUpdate(enrollmentId, teacherId);
        if (enrollment == null) return false;
        if (!"draft".equals(enrollment.getPublicationStatus())
                && !"pending".equals(enrollment.getPublicationStatus())
                && !"review_requested".equals(enrollment.getPublicationStatus())
                && !"incomplete".equals(enrollment.getPublicationStatus())) return false;

        String type = enrollment.getAttemptType() == null ? "regular" : enrollment.getAttemptType();
        boolean incompleteAttempt = "incomplete".equals(type);
        int currentSemester = enrollment.getStudentCurrentSemester();
        if (incompleteAttempt && (enrollment.getIncompleteDueSemester() < currentSemester
                || (absentMid && !enrollment.isMidAbsent())
                || (absentFinal && !enrollment.isFinalAbsent()))) return false;
        if ("improvement".equals(type) && (absentMid || absentFinal)) return false;

        Double attendance = null;
        Double q1 = null;
        Double q2 = null;
        Double assignment = null;
        Double mid = enrollment.getMid();
        Double finalExam = enrollment.getFinalExam();
        try {
            if (!incompleteAttempt && !"improvement".equals(type)) {
                attendance = parseMark(attendanceText, 5);
                q1 = parseMark(q1Text, 10);
                q2 = parseMark(q2Text, 10);
                assignment = parseMark(assignmentText, 5);
            }
            if (!incompleteAttempt || enrollment.isMidAbsent()) {
                if (!absentMid) mid = parseMark(midText, 30);
            }
            if (!incompleteAttempt || enrollment.isFinalAbsent()) {
                if (!absentFinal) finalExam = parseMark(finalText, 40);
            }
        } catch (NumberFormatException e) {
            return false;
        }
        boolean midIsAbsent = incompleteAttempt
                ? enrollment.isMidAbsent() && ((midText == null || midText.trim().isEmpty()) || absentMid) : absentMid;
        boolean finalIsAbsent = incompleteAttempt
                ? enrollment.isFinalAbsent() && ((finalText == null || finalText.trim().isEmpty()) || absentFinal) : absentFinal;
        String resultStatus = midIsAbsent || finalIsAbsent ? "incomplete" : "pending";
        String query;
        if (incompleteAttempt) {
            query = "UPDATE enrollments SET mid = ?, final = ?, mid_absent = ?, final_absent = ?, " +
                    "attempt_type = ?, incomplete_due_semester = ?, result_status = ? WHERE id = ?";
        } else if ("improvement".equals(type)) {
            query = "UPDATE enrollments SET mid = ?, final = ?, result_status = ? WHERE id = ?";
        } else {
            query = "UPDATE enrollments SET attendance = ?, q1 = ?, q2 = ?, assignment = ?, mid = ?, final = ?, " +
                    "mid_absent = ?, final_absent = ?, attempt_type = ?, incomplete_due_semester = ?, result_status = ? WHERE id = ?";
        }
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            if (incompleteAttempt) {
                pstmt.setObject(1, mid);
                pstmt.setObject(2, finalExam);
                pstmt.setInt(3, midIsAbsent ? 1 : 0);
                pstmt.setInt(4, finalIsAbsent ? 1 : 0);
                pstmt.setString(5, midIsAbsent || finalIsAbsent ? "incomplete" : "regular");
                if (midIsAbsent || finalIsAbsent) pstmt.setInt(6, enrollment.getIncompleteDueSemester());
                else pstmt.setNull(6, Types.INTEGER);
                pstmt.setString(7, resultStatus);
                pstmt.setInt(8, enrollmentId);
            } else if ("improvement".equals(type)) {
                pstmt.setObject(1, mid);
                pstmt.setObject(2, finalExam);
                pstmt.setString(3, resultStatus);
                pstmt.setInt(4, enrollmentId);
            } else {
                pstmt.setObject(1, attendance);
                pstmt.setObject(2, q1);
                pstmt.setObject(3, q2);
                pstmt.setObject(4, assignment);
                pstmt.setObject(5, mid);
                pstmt.setObject(6, finalExam);
                pstmt.setInt(7, midIsAbsent ? 1 : 0);
                pstmt.setInt(8, finalIsAbsent ? 1 : 0);
                pstmt.setString(9, midIsAbsent || finalIsAbsent ? "incomplete" : type);
                if (midIsAbsent || finalIsAbsent) pstmt.setInt(10, currentSemester + 2);
                else pstmt.setNull(10, Types.INTEGER);
                pstmt.setString(11, resultStatus);
                pstmt.setInt(12, enrollmentId);
            }
            boolean updated = pstmt.executeUpdate() > 0;
            if (updated && !new MessageDAO().sendMessageToRole(teacherId, "admin",
                    "Result submitted: " + enrollment.getCourseId(),
                    "Teacher " + teacherId + " submitted a result for " + enrollment.getStudentName()
                            + " (" + enrollment.getStudentId() + "), course " + enrollment.getCourseId()
                            + " - " + enrollment.getCourseName() + ". Review it in Approve Results.")) {
                System.err.println("[EnrollmentDAO] Result was saved, but admin notification could not be sent.");
            }
            return updated;
        } catch (SQLException e) {
            System.err.println("[EnrollmentDAO] updateMarks Error: " + e.getMessage());
            return false;
        }
    }

    private Enrollment getEnrollmentForUpdate(int enrollmentId, String teacherId) {
        String query = "SELECT e.*, c.course_name, c.credits, student.name AS student_name, " +
                "student.current_semester AS student_current_semester, t.name AS teacher_name " +
                "FROM enrollments e JOIN courses c ON e.course_id = c.course_id " +
                "JOIN users student ON e.student_id = student.id JOIN users t ON c.teacher_id = t.id " +
                "WHERE e.id = ? AND c.teacher_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, enrollmentId);
            pstmt.setString(2, teacherId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? mapEnrollment(rs) : null;
            }
        } catch (SQLException e) {
            System.err.println("[EnrollmentDAO] getEnrollmentForUpdate Error: " + e.getMessage());
            return null;
        }
    }

    private Double parseMark(String text, double maximum) {
        String value = text == null ? "" : text.trim();
        double mark = value.isEmpty() ? 0.0 : Double.parseDouble(value);
        if (mark < 0 || mark > maximum) {
            throw new NumberFormatException("Mark must be between 0 and " + maximum);
        }
        return mark;
    }

    private double valueOrZero(Double value) {
        return value == null ? 0.0 : value;
    }

    private Enrollment mapEnrollment(ResultSet rs) throws SQLException {
        Enrollment e = new Enrollment();
        e.setId(rs.getInt("id"));
        e.setStudentId(rs.getString("student_id"));
        try { e.setStudentName(rs.getString("student_name")); } catch (Exception ignored) {}
        e.setCourseId(rs.getString("course_id"));
        try { e.setCourseName(rs.getString("course_name")); } catch (Exception ignored) {}
        try { e.setCredits(rs.getInt("credits")); } catch (Exception ignored) {}
        e.setSemester(rs.getString("semester"));
        e.setPublicationStatus(rs.getString("result_status"));
        e.setTeacherName(rs.getString("teacher_name"));
        e.setAttemptType(rs.getString("attempt_type"));
        e.setIncompleteDueSemester(rs.getInt("incomplete_due_semester"));
        e.setMidAbsent(rs.getInt("mid_absent") != 0);
        e.setFinalAbsent(rs.getInt("final_absent") != 0);
        try { e.setStudentCurrentSemester(rs.getInt("student_current_semester")); } catch (SQLException ignored) {}
        e.setAttendance(rs.getObject("attendance") != null ? rs.getDouble("attendance") : 0.0);
        e.setQ1(rs.getObject("q1") != null ? rs.getDouble("q1") : 0.0);
        e.setQ2(rs.getObject("q2") != null ? rs.getDouble("q2") : 0.0);
        e.setAssignment(rs.getObject("assignment") != null ? rs.getDouble("assignment") : 0.0);
        e.setMid(rs.getObject("mid") != null ? rs.getDouble("mid") : 0.0);
        e.setFinalExam(rs.getObject("final") != null ? rs.getDouble("final") : 0.0);
        return e;
    }
}
