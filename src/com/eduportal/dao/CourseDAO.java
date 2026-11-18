package com.eduportal.dao;

import com.eduportal.models.Course;
import com.eduportal.models.CourseApplication;
import com.eduportal.utils.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CourseDAO {

    public List<Course> getAllCourses() {
        List<Course> list = new ArrayList<>();
        String query = "SELECT c.*, u.name AS teacher_name FROM courses c LEFT JOIN users u ON c.teacher_id = u.id ORDER BY c.target_semester, c.course_id";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                list.add(mapCourse(rs));
            }
        } catch (SQLException e) {
            System.err.println("[CourseDAO] getAllCourses Error: " + e.getMessage());
        }
        return list;
    }

    public List<Course> getCoursesByTargetSemester(int targetSemester) {
        List<Course> list = new ArrayList<>();
        String query = "SELECT c.*, u.name AS teacher_name FROM courses c LEFT JOIN users u ON c.teacher_id = u.id WHERE c.target_semester = ? ORDER BY c.course_id";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, targetSemester);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                list.add(mapCourse(rs));
            }
        } catch (SQLException e) {
            System.err.println("[CourseDAO] getCoursesByTargetSemester Error: " + e.getMessage());
        }
        return list;
    }

    public List<Course> getCoursesByTeacher(String teacherId) {
        List<Course> list = new ArrayList<>();
        String query = "SELECT c.*, u.name AS teacher_name FROM courses c LEFT JOIN users u ON c.teacher_id = u.id WHERE c.teacher_id = ? ORDER BY c.course_id";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, teacherId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                list.add(mapCourse(rs));
            }
        } catch (SQLException e) {
            System.err.println("[CourseDAO] getCoursesByTeacher Error: " + e.getMessage());
        }
        return list;
    }

    public List<Course> getAvailableCoursesForTeachers() {
        return getAvailableCoursesForTeachers(0);
    }

    public List<Course> getAvailableCoursesForTeachers(int targetSemester) {
        return getAvailableCoursesForTeachers(targetSemester, null);
    }

    public List<Course> getAvailableCoursesForTeachers(int targetSemester, String teacherId) {
        List<Course> list = new ArrayList<>();
        String query = "SELECT c.*, u.name AS teacher_name FROM courses c " +
                "LEFT JOIN users u ON c.teacher_id = u.id WHERE c.teacher_id IS NULL " +
                "AND (? = 0 OR c.target_semester = ?) " +
                "AND (? IS NULL OR NOT EXISTS (SELECT 1 FROM course_applications a " +
                "WHERE a.course_id = c.course_id AND a.teacher_id = ? AND a.status = 'pending')) " +
                "ORDER BY c.target_semester, c.course_id";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, targetSemester);
            stmt.setInt(2, targetSemester);
            stmt.setString(3, teacherId);
            stmt.setString(4, teacherId);
            try (ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) list.add(mapCourse(rs));
            }
        } catch (SQLException e) {
            System.err.println("[CourseDAO] getAvailableCoursesForTeachers Error: " + e.getMessage());
        }
        return list;
    }

    public List<CourseApplication> getPendingApplications() {
        List<CourseApplication> list = new ArrayList<>();
        String query = "SELECT a.id, a.course_id, c.course_name, a.teacher_id, u.name AS teacher_name, a.status " +
                "FROM course_applications a JOIN courses c ON a.course_id = c.course_id " +
                "JOIN users u ON a.teacher_id = u.id WHERE a.status = 'pending' ORDER BY a.created_at";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                CourseApplication application = new CourseApplication();
                application.setId(rs.getInt("id"));
                application.setCourseId(rs.getString("course_id"));
                application.setCourseName(rs.getString("course_name"));
                application.setTeacherId(rs.getString("teacher_id"));
                application.setTeacherName(rs.getString("teacher_name"));
                application.setStatus(rs.getString("status"));
                list.add(application);
            }
        } catch (SQLException e) {
            System.err.println("[CourseDAO] getPendingApplications Error: " + e.getMessage());
        }
        return list;
    }

    public boolean applyForCourse(String courseId, String teacherId) {
        String query = "INSERT INTO course_applications (course_id, teacher_id, status) " +
                "SELECT ?, ?, 'pending' WHERE EXISTS " +
                "(SELECT 1 FROM courses WHERE course_id = ? AND teacher_id IS NULL) " +
                "AND NOT EXISTS (SELECT 1 FROM course_applications " +
                "WHERE course_id = ? AND teacher_id = ? AND status = 'pending') " +
                "ON CONFLICT(course_id, teacher_id) DO UPDATE SET status = 'pending' " +
                "WHERE course_applications.status <> 'pending'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, courseId);
            pstmt.setString(2, teacherId);
            pstmt.setString(3, courseId);
            pstmt.setString(4, courseId);
            pstmt.setString(5, teacherId);
            boolean applied = pstmt.executeUpdate() > 0;
            if (applied && !new MessageDAO().sendMessageToRole(teacherId, "admin",
                    "New teacher course application: " + courseId,
                    "Teacher " + teacherId + " applied to teach course " + courseId
                            + ". Review this request in Course Management.")) {
                System.err.println("[CourseDAO] Course application was saved, but admin notification could not be sent.");
            }
            return applied;
        } catch (SQLException e) {
            System.err.println("[CourseDAO] applyForCourse Error: " + e.getMessage());
            return false;
        }
    }

    private boolean closePendingApplications(Connection conn, String courseId, String courseName,
                                             String assignedTeacherId) throws SQLException {
        String selectQuery = "SELECT teacher_id FROM course_applications WHERE course_id = ? AND status = 'pending'";
        List<String> applicants = new ArrayList<>();
        try (PreparedStatement select = conn.prepareStatement(selectQuery)) {
            select.setString(1, courseId);
            try (ResultSet rs = select.executeQuery()) {
                while (rs.next()) applicants.add(rs.getString("teacher_id"));
            }
        }

        String updateQuery = "UPDATE course_applications SET status = ? " +
                "WHERE course_id = ? AND teacher_id = ? AND status = 'pending'";
        try (PreparedStatement update = conn.prepareStatement(updateQuery)) {
            for (String applicantId : applicants) {
                boolean accepted = applicantId.equals(assignedTeacherId);
                update.setString(1, accepted ? "approved" : "revoked");
                update.setString(2, courseId);
                update.setString(3, applicantId);
                update.executeUpdate();
                String outcome = accepted ? "approved" : "revoked";
                if (!sendApplicationOutcome(conn, applicantId, courseId, courseName, outcome)) return false;
            }
        }
        return true;
    }

    private boolean sendApplicationOutcome(Connection conn, String teacherId, String courseId,
                                           String courseName, String outcome) throws SQLException {
        String adminQuery = "SELECT id FROM users WHERE role = 'admin' ORDER BY id LIMIT 1";
        String adminId;
        try (PreparedStatement admin = conn.prepareStatement(adminQuery);
             ResultSet rs = admin.executeQuery()) {
            if (!rs.next()) {
                System.err.println("[CourseDAO] Cannot notify teacher about application: no admin account exists.");
                return false;
            }
            adminId = rs.getString("id");
        }
        boolean accepted = "approved".equals(outcome);
        boolean revoked = "revoked".equals(outcome);
        String subject = accepted
                ? "Course application accepted: " + courseId
                : revoked ? "Course application revoked: " + courseId
                : "Course application update: " + courseId;
        String body = accepted
                ? "Good news! Your application to teach " + courseName + " (" + courseId
                    + ") has been accepted. The course has been assigned to you. Thank you for applying."
                : revoked
                ? "Thank you for your interest in teaching " + courseName + " (" + courseId
                    + "). Your application has been revoked because the course was assigned to another teacher. "
                    + "Please apply again in a future semester if the course becomes available."
                : "Thank you for your interest in teaching " + courseName + " (" + courseId
                    + "). Your application was not approved this time. We encourage you to apply again in a future semester.";
        try (PreparedStatement message = conn.prepareStatement(
                "INSERT INTO messages (sender_id, receiver_id, subject, body) VALUES (?, ?, ?, ?)")) {
            message.setString(1, adminId);
            message.setString(2, teacherId);
            message.setString(3, subject);
            message.setString(4, body);
            return message.executeUpdate() == 1;
        }
    }

    public boolean approveApplication(int applicationId) {
        String query = "SELECT a.course_id, a.teacher_id, c.course_name FROM course_applications a " +
                "JOIN courses c ON c.course_id = a.course_id WHERE a.id = ? AND a.status = 'pending'";
        try (Connection conn = DatabaseConnection.getConnection()) {
            boolean originalAutoCommit = conn.getAutoCommit();
            try {
                conn.setAutoCommit(false);
                try (PreparedStatement select = conn.prepareStatement(query)) {
                    select.setInt(1, applicationId);
                    try (ResultSet rs = select.executeQuery()) {
                        if (!rs.next()) {
                            conn.rollback();
                            return false;
                        }
                        String courseId = rs.getString("course_id");
                        String teacherId = rs.getString("teacher_id");
                        String courseName = rs.getString("course_name");
                        try (PreparedStatement assign = conn.prepareStatement(
                                "UPDATE courses SET teacher_id = ? WHERE course_id = ? AND teacher_id IS NULL");
                             PreparedStatement approve = conn.prepareStatement(
                                     "UPDATE course_applications SET status = 'approved' WHERE id = ? AND status = 'pending'")) {
                            assign.setString(1, teacherId);
                            assign.setString(2, courseId);
                            if (assign.executeUpdate() != 1) {
                                conn.rollback();
                                return false;
                            }
                            approve.setInt(1, applicationId);
                            if (approve.executeUpdate() != 1) {
                                conn.rollback();
                                return false;
                            }
                            if (!closePendingApplications(conn, courseId, courseName, teacherId)) {
                                conn.rollback();
                                return false;
                            }
                        }
                    }
                }
                if (!sendApplicationOutcomeForApprovedApplication(conn, applicationId)) {
                    conn.rollback();
                    return false;
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
            System.err.println("[CourseDAO] approveApplication Error: " + e.getMessage());
            return false;
        }
    }

    public boolean rejectApplication(int applicationId) {
        String selectQuery = "SELECT a.course_id, a.teacher_id, c.course_name FROM course_applications a " +
                "JOIN courses c ON c.course_id = a.course_id WHERE a.id = ? AND a.status = 'pending'";
        String updateQuery = "UPDATE course_applications SET status = 'rejected' WHERE id = ? AND status = 'pending'";
        try (Connection conn = DatabaseConnection.getConnection()) {
            boolean originalAutoCommit = conn.getAutoCommit();
            try {
                conn.setAutoCommit(false);
                String courseId;
                String teacherId;
                String courseName;
                try (PreparedStatement select = conn.prepareStatement(selectQuery)) {
                    select.setInt(1, applicationId);
                    try (ResultSet rs = select.executeQuery()) {
                        if (!rs.next()) {
                            conn.rollback();
                            return false;
                        }
                        courseId = rs.getString("course_id");
                        teacherId = rs.getString("teacher_id");
                        courseName = rs.getString("course_name");
                    }
                }
                try (PreparedStatement update = conn.prepareStatement(updateQuery)) {
                    update.setInt(1, applicationId);
                    if (update.executeUpdate() != 1
                            || !sendApplicationOutcome(conn, teacherId, courseId, courseName, "rejected")) {
                        conn.rollback();
                        return false;
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
            System.err.println("[CourseDAO] rejectApplication Error: " + e.getMessage());
            return false;
        }
    }

    private boolean sendApplicationOutcomeForApprovedApplication(Connection conn, int applicationId)
            throws SQLException {
        String query = "SELECT a.teacher_id, a.course_id, c.course_name FROM course_applications a " +
                "JOIN courses c ON c.course_id = a.course_id WHERE a.id = ? AND a.status = 'approved'";
        try (PreparedStatement select = conn.prepareStatement(query)) {
            select.setInt(1, applicationId);
            try (ResultSet rs = select.executeQuery()) {
                return rs.next() && sendApplicationOutcome(conn, rs.getString("teacher_id"),
                        rs.getString("course_id"), rs.getString("course_name"), "approved");
            }
        }
    }

    public List<Course> searchCourses(String keyword) {
        List<Course> list = new ArrayList<>();
        String query = "SELECT c.*, u.name AS teacher_name FROM courses c LEFT JOIN users u ON c.teacher_id = u.id WHERE LOWER(c.course_id) LIKE ? OR LOWER(c.course_name) LIKE ? ORDER BY c.course_id";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            String k = "%" + keyword.toLowerCase() + "%";
            pstmt.setString(1, k);
            pstmt.setString(2, k);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                list.add(mapCourse(rs));
            }
        } catch (SQLException e) {
            System.err.println("[CourseDAO] searchCourses Error: " + e.getMessage());
        }
        return list;
    }

    public boolean addCourse(Course c) {
        String query = "INSERT INTO courses (course_id, course_name, credits, teacher_id, semester, target_semester) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, c.getCourseId());
            pstmt.setString(2, c.getCourseName());
            pstmt.setInt(3, c.getCredits());
            pstmt.setString(4, c.getTeacherId());
            pstmt.setString(5, c.getSemester() != null ? c.getSemester() : "Spring 2026");
            pstmt.setInt(6, c.getTargetSemester() > 0 ? c.getTargetSemester() : 1);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[CourseDAO] addCourse Error: " + e.getMessage());
            return false;
        }
    }

    public boolean updateCourse(String courseId, String courseName, int credits, String teacherId, int targetSemester) {
        String query = "UPDATE courses SET course_name = ?, credits = ?, teacher_id = ?, target_semester = ? WHERE course_id = ?";
        try (Connection conn = DatabaseConnection.getConnection()) {
            boolean originalAutoCommit = conn.getAutoCommit();
            try {
                conn.setAutoCommit(false);
                try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                    pstmt.setString(1, courseName);
                    pstmt.setInt(2, credits);
                    pstmt.setString(3, (teacherId == null || teacherId.trim().isEmpty()) ? null : teacherId);
                    pstmt.setInt(4, targetSemester);
                    pstmt.setString(5, courseId);
                    if (pstmt.executeUpdate() == 0) {
                        conn.rollback();
                        return false;
                    }
                }
                if (teacherId != null && !teacherId.trim().isEmpty()
                        && !closePendingApplications(conn, courseId, courseName, teacherId)) {
                    conn.rollback();
                    return false;
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
            System.err.println("[CourseDAO] updateCourse Error: " + e.getMessage());
            return false;
        }
    }

    public boolean isCourseAssignedToTeacher(String courseId, String teacherId) {
        if (teacherId == null || teacherId.trim().isEmpty()) return false;
        String query = "SELECT COUNT(*) FROM courses WHERE course_id = ? AND teacher_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, courseId);
            pstmt.setString(2, teacherId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("[CourseDAO] isCourseAssignedToTeacher Error: " + e.getMessage());
        }
        return false;
    }

    public boolean assignTeacher(String courseId, String teacherId) {
        for (Course course : getAllCourses()) {
            if (course.getCourseId().equals(courseId)) {
                return updateCourse(courseId, course.getCourseName(), course.getCredits(),
                        teacherId, course.getTargetSemester());
            }
        }
        return false;
    }

    public boolean deleteCourse(String courseId) {
        String query = "DELETE FROM courses WHERE course_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, courseId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[CourseDAO] deleteCourse Error: " + e.getMessage());
            return false;
        }
    }

    private Course mapCourse(ResultSet rs) throws SQLException {
        Course c = new Course();
        c.setCourseId(rs.getString("course_id"));
        c.setCourseName(rs.getString("course_name"));
        c.setCredits(rs.getInt("credits"));
        c.setTeacherId(rs.getString("teacher_id"));
        try { c.setTeacherName(rs.getString("teacher_name")); } catch (Exception ignored) {}
        c.setSemester(rs.getString("semester"));
        try { c.setTargetSemester(rs.getInt("target_semester")); } catch (Exception ignored) {}
        return c;
    }
}
