package com.eduportal.dao;

import com.eduportal.models.Deadline;
import com.eduportal.utils.DatabaseConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class DeadlineDAO {

    public List<Deadline> getAllSystemDeadlines() {
        List<Deadline> list = new ArrayList<>();
        String query = "SELECT * FROM system_deadlines ORDER BY id";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                Deadline d = new Deadline();
                d.setId(rs.getInt("id"));
                d.setDeadlineKey(rs.getString("deadline_key"));
                d.setTitle(rs.getString("title"));
                d.setStartDate(rs.getString("start_date"));
                d.setEndDate(rs.getString("end_date"));
                d.setIsActive(rs.getInt("is_active"));
                list.add(d);
            }
        } catch (SQLException e) {
            System.err.println("[DeadlineDAO] getAllSystemDeadlines Error: " + e.getMessage());
        }
        return list;
    }

    public Deadline getDeadlineByKey(String key) {
        String query = "SELECT * FROM system_deadlines WHERE deadline_key = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, key);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                Deadline d = new Deadline();
                d.setId(rs.getInt("id"));
                d.setDeadlineKey(rs.getString("deadline_key"));
                d.setTitle(rs.getString("title"));
                d.setStartDate(rs.getString("start_date"));
                d.setEndDate(rs.getString("end_date"));
                d.setIsActive(rs.getInt("is_active"));
                return d;
            }
        } catch (SQLException e) {
            System.err.println("[DeadlineDAO] getDeadlineByKey Error: " + e.getMessage());
        }
        return null;
    }

    public boolean addSystemDeadline(String key, String title, String startDate, String endDate, boolean isActive) {
        String query = "INSERT INTO system_deadlines (deadline_key, title, start_date, end_date, is_active) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, key);
            pstmt.setString(2, title);
            pstmt.setString(3, startDate);
            pstmt.setString(4, endDate);
            pstmt.setInt(5, isActive ? 1 : 0);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[DeadlineDAO] addSystemDeadline Error: " + e.getMessage());
            return false;
        }
    }

    /** Update only the start and end dates of a deadline (title is fixed) */
    public boolean updateDeadlineDates(String key, String startDate, String endDate) {
        String query = "UPDATE system_deadlines SET start_date = ?, end_date = ? WHERE deadline_key = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, startDate);
            pstmt.setString(2, endDate);
            pstmt.setString(3, key);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[DeadlineDAO] updateDeadlineDates Error: " + e.getMessage());
            return false;
        }
    }

    public boolean updateSystemDeadline(String key, String title, String startDate, String endDate, boolean isActive) {
        String query = "UPDATE system_deadlines SET title = ?, start_date = ?, end_date = ?, is_active = ? WHERE deadline_key = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, title);
            pstmt.setString(2, startDate);
            pstmt.setString(3, endDate);
            pstmt.setInt(4, isActive ? 1 : 0);
            pstmt.setString(5, key);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[DeadlineDAO] updateSystemDeadline Error: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteSystemDeadline(int id) {
        String query = "DELETE FROM system_deadlines WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[DeadlineDAO] deleteSystemDeadline Error: " + e.getMessage());
            return false;
        }
    }

    /** Check if a deadline is active (within date range) */
    public boolean isDeadlineActive(String key) {
        Deadline d = getDeadlineByKey(key);
        if (d == null) return false;

        String end = d.getEndDate();
        if (end == null || end.isEmpty()) return true;

        try {
            LocalDate endDate = LocalDate.parse(end);
            return !LocalDate.now().isAfter(endDate);
        } catch (Exception e) {
            return true;
        }
    }

    /** Check if a deadline is active for a specific user (checks personal permission too) */
    public boolean isDeadlineActiveForUser(String key, String userId) {
        // First check global deadline
        if (isDeadlineActive(key)) return true;

        // Then check individual permission with expiry
        return hasPermission(userId, key);
    }

    // ---- Deadline Permission Methods ----

    /** Grant permission to a user for a specific deadline with an expiry date */
    public boolean grantPermission(String userId, String deadlineKey, String expiryDate) {
        // Remove any existing permission first
        revokePermission(userId, deadlineKey);
        String query = "INSERT INTO deadline_permissions (user_id, deadline_key, expiry_date) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, userId);
            pstmt.setString(2, deadlineKey);
            pstmt.setString(3, expiryDate);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[DeadlineDAO] grantPermission Error: " + e.getMessage());
            return false;
        }
    }

    /** Revoke permission for a user on a specific deadline */
    public boolean revokePermission(String userId, String deadlineKey) {
        String query = "DELETE FROM deadline_permissions WHERE user_id = ? AND deadline_key = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, userId);
            pstmt.setString(2, deadlineKey);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[DeadlineDAO] revokePermission Error: " + e.getMessage());
            return false;
        }
    }

    /** Check if a user has a valid (non-expired) permission for a deadline */
    public boolean hasPermission(String userId, String deadlineKey) {
        String query = "SELECT expiry_date FROM deadline_permissions WHERE user_id = ? AND deadline_key = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, userId);
            pstmt.setString(2, deadlineKey);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String expiry = rs.getString("expiry_date");
                if (expiry == null || expiry.isEmpty()) return true;
                try {
                    LocalDate expiryDate = LocalDate.parse(expiry);
                    return !LocalDate.now().isAfter(expiryDate);
                } catch (Exception e) {
                    return true;
                }
            }
        } catch (SQLException e) {
            System.err.println("[DeadlineDAO] hasPermission Error: " + e.getMessage());
        }
        return false;
    }

    /** Get all active permissions (for the admin permission list view) */
    public List<String[]> getAllPermissions() {
        List<String[]> list = new ArrayList<>();
        String query = "SELECT dp.id, dp.user_id, u.name, dp.deadline_key, sd.title, dp.expiry_date, dp.granted_at " +
                "FROM deadline_permissions dp " +
                "JOIN users u ON dp.user_id = u.id " +
                "JOIN system_deadlines sd ON dp.deadline_key = sd.deadline_key " +
                "ORDER BY dp.granted_at DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                String[] row = new String[7];
                row[0] = String.valueOf(rs.getInt("id"));
                row[1] = rs.getString("user_id");
                row[2] = rs.getString("name");
                row[3] = rs.getString("deadline_key");
                row[4] = rs.getString("title");
                row[5] = rs.getString("expiry_date");
                row[6] = rs.getString("granted_at");
                list.add(row);
            }
        } catch (SQLException e) {
            System.err.println("[DeadlineDAO] getAllPermissions Error: " + e.getMessage());
        }
        return list;
    }

    /** Remove a permission by its ID */
    public boolean revokePermissionById(int permissionId) {
        String query = "DELETE FROM deadline_permissions WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, permissionId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[DeadlineDAO] revokePermissionById Error: " + e.getMessage());
            return false;
        }
    }

    /** Clean up expired permissions automatically */
    public void cleanExpiredPermissions() {
        String query = "DELETE FROM deadline_permissions WHERE expiry_date < ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, LocalDate.now().toString());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[DeadlineDAO] cleanExpiredPermissions Error: " + e.getMessage());
        }
    }
}
