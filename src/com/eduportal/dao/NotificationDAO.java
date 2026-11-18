package com.eduportal.dao;

import com.eduportal.models.Notification;
import com.eduportal.utils.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class NotificationDAO {

    public List<Notification> getNotificationsForUser(String userId) {
        List<Notification> list = new ArrayList<>();
        String query = "SELECT * FROM notifications WHERE user_id = ? ORDER BY created_at DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, userId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Notification n = new Notification();
                n.setId(rs.getInt("id"));
                n.setUserId(rs.getString("user_id"));
                n.setTitle(rs.getString("title"));
                n.setMessage(rs.getString("message"));
                n.setType(rs.getString("type"));
                n.setIsRead(rs.getInt("is_read"));
                n.setCreatedAt(rs.getString("created_at"));
                list.add(n);
            }
        } catch (SQLException e) {
            System.err.println("[NotificationDAO] getNotificationsForUser Error: " + e.getMessage());
        }
        return list;
    }

    public boolean createNotification(Notification n) {
        String query = "INSERT INTO notifications (user_id, title, message, type) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, n.getUserId());
            pstmt.setString(2, n.getTitle());
            pstmt.setString(3, n.getMessage());
            pstmt.setString(4, n.getType());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[NotificationDAO] createNotification Error: " + e.getMessage());
            return false;
        }
    }
}
