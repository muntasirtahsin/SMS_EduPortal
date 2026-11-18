package com.eduportal.dao;

import com.eduportal.models.Message;
import com.eduportal.utils.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MessageDAO {

    public List<Message> getInboxMessages(String userId) {
        List<Message> list = new ArrayList<>();
        String query = "SELECT m.*, u.name AS sender_name FROM messages m " +
                "JOIN users u ON m.sender_id = u.id " +
                "WHERE m.receiver_id = ? ORDER BY m.created_at DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, userId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                list.add(mapMessage(rs));
            }
        } catch (SQLException e) {
            System.err.println("[MessageDAO] getInboxMessages Error: " + e.getMessage());
        }
        return list;
    }

    public List<Message> getSentMessages(String userId) {
        List<Message> list = new ArrayList<>();
        String query = "SELECT m.*, u.name AS receiver_name FROM messages m " +
                "JOIN users u ON m.receiver_id = u.id " +
                "WHERE m.sender_id = ? ORDER BY m.created_at DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, userId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                list.add(mapMessage(rs));
            }
        } catch (SQLException e) {
            System.err.println("[MessageDAO] getSentMessages Error: " + e.getMessage());
        }
        return list;
    }

    public boolean sendMessage(Message m) {
        String query = "INSERT INTO messages (sender_id, receiver_id, subject, body) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, m.getSenderId());
            pstmt.setString(2, m.getReceiverId());
            pstmt.setString(3, m.getSubject());
            pstmt.setString(4, m.getBody());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[MessageDAO] sendMessage Error: " + e.getMessage());
            return false;
        }
    }

    public boolean sendMessageToRole(String senderId, String role, String subject, String body) {
        String query = "INSERT INTO messages (sender_id, receiver_id, subject, body) " +
                "SELECT ?, id, ?, ? FROM users WHERE LOWER(role) = LOWER(?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, senderId);
            pstmt.setString(2, subject);
            pstmt.setString(3, body);
            pstmt.setString(4, role);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[MessageDAO] sendMessageToRole Error: " + e.getMessage());
            return false;
        }
    }

    public boolean markAsRead(int messageId) {
        String query = "UPDATE messages SET is_read = 1 WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, messageId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[MessageDAO] markAsRead Error: " + e.getMessage());
            return false;
        }
    }

    public int getUnreadCount(String userId) {
        String query = "SELECT COUNT(*) AS unread FROM messages WHERE receiver_id = ? AND is_read = 0";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, userId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getInt("unread");
        } catch (SQLException e) {
            System.err.println("[MessageDAO] getUnreadCount Error: " + e.getMessage());
        }
        return 0;
    }

    private Message mapMessage(ResultSet rs) throws SQLException {
        Message m = new Message();
        m.setId(rs.getInt("id"));
        m.setSenderId(rs.getString("sender_id"));
        try { m.setSenderName(rs.getString("sender_name")); } catch (Exception ignored) {}
        m.setReceiverId(rs.getString("receiver_id"));
        try { m.setReceiverName(rs.getString("receiver_name")); } catch (Exception ignored) {}
        m.setSubject(rs.getString("subject"));
        m.setBody(rs.getString("body"));
        m.setIsRead(rs.getInt("is_read"));
        m.setCreatedAt(rs.getString("created_at"));
        return m;
    }
}
