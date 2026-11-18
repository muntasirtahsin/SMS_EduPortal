package com.eduportal.dao;

import com.eduportal.models.AttendanceRecord;
import com.eduportal.utils.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AttendanceDAO {

    public List<AttendanceRecord> getCourseAttendance(String courseId, String date) {
        List<AttendanceRecord> records = new ArrayList<>();
        String query = "SELECT DISTINCT e.student_id, u.name AS student_name, c.course_id, c.course_name, " +
                "? AS attendance_date, COALESCE(a.status, 'Unmarked') AS status " +
                "FROM enrollments e JOIN users u ON e.student_id = u.id " +
                "JOIN courses c ON e.course_id = c.course_id " +
                "LEFT JOIN attendance_records a ON a.course_id = e.course_id AND a.student_id = e.student_id " +
                "AND a.attendance_date = ? WHERE e.course_id = ? ORDER BY e.student_id";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, date);
            pstmt.setString(2, date);
            pstmt.setString(3, courseId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) records.add(mapAttendance(rs));
            }
        } catch (SQLException e) {
            System.err.println("[AttendanceDAO] getCourseAttendance Error: " + e.getMessage());
        }
        return records;
    }

    public List<AttendanceRecord> getStudentAttendance(String studentId) {
        List<AttendanceRecord> records = new ArrayList<>();
        String query = "SELECT a.student_id, u.name AS student_name, a.course_id, c.course_name, " +
                "a.attendance_date, a.status FROM attendance_records a " +
                "JOIN users u ON a.student_id = u.id JOIN courses c ON a.course_id = c.course_id " +
                "WHERE a.student_id = ? ORDER BY a.attendance_date DESC, a.course_id";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, studentId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) records.add(mapAttendance(rs));
            }
        } catch (SQLException e) {
            System.err.println("[AttendanceDAO] getStudentAttendance Error: " + e.getMessage());
        }
        return records;
    }

    public List<AttendanceRecord> getAllStudentAttendance() {
        List<AttendanceRecord> records = new ArrayList<>();
        String query = "SELECT a.student_id, u.name AS student_name, a.course_id, c.course_name, " +
                "a.attendance_date, a.status FROM attendance_records a " +
                "JOIN users u ON a.student_id = u.id JOIN courses c ON a.course_id = c.course_id " +
                "ORDER BY a.attendance_date DESC, a.course_id, a.student_id";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) records.add(mapAttendance(rs));
        } catch (SQLException e) {
            System.err.println("[AttendanceDAO] getAllStudentAttendance Error: " + e.getMessage());
        }
        return records;
    }

    public boolean saveStudentAttendance(String courseId, String studentId, String date, String status, String markedBy) {
        String query = "INSERT INTO attendance_records (course_id, student_id, attendance_date, status, marked_by) " +
                "VALUES (?, ?, ?, ?, ?) ON CONFLICT(course_id, student_id, attendance_date) " +
                "DO UPDATE SET status = excluded.status, marked_by = excluded.marked_by";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, courseId);
            pstmt.setString(2, studentId);
            pstmt.setString(3, date);
            pstmt.setString(4, status);
            pstmt.setString(5, markedBy);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[AttendanceDAO] saveStudentAttendance Error: " + e.getMessage());
            return false;
        }
    }

    public boolean clearStudentAttendance(String courseId, String studentId, String date) {
        String query = "DELETE FROM attendance_records WHERE course_id = ? AND student_id = ? AND attendance_date = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, courseId);
            pstmt.setString(2, studentId);
            pstmt.setString(3, date);
            return pstmt.executeUpdate() >= 0;
        } catch (SQLException e) {
            System.err.println("[AttendanceDAO] clearStudentAttendance Error: " + e.getMessage());
            return false;
        }
    }

    private AttendanceRecord mapAttendance(ResultSet rs) throws SQLException {
        AttendanceRecord record = new AttendanceRecord();
        record.setStudentId(rs.getString("student_id"));
        record.setStudentName(rs.getString("student_name"));
        record.setCourseId(rs.getString("course_id"));
        record.setCourseName(rs.getString("course_name"));
        record.setAttendanceDate(rs.getString("attendance_date"));
        record.setStatus(rs.getString("status"));
        return record;
    }
}
