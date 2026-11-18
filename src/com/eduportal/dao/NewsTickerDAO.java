package com.eduportal.dao;

import com.eduportal.models.NewsTicker;
import com.eduportal.utils.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class NewsTickerDAO {

    public List<NewsTicker> getActiveTickersForRole(String role) {
        List<NewsTicker> list = new ArrayList<>();
        boolean isAdmin = "admin".equalsIgnoreCase(role);
        String query;
        if (isAdmin) {
            // Admin portal always sees all active tickers regardless of target audience
            query = "SELECT * FROM news_tickers WHERE is_active = 1 ORDER BY created_at DESC";
        } else {
            query = "SELECT * FROM news_tickers WHERE is_active = 1 AND (target_role = 'all' OR target_role = ?) ORDER BY created_at DESC";
        }

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            if (!isAdmin) {
                pstmt.setString(1, role != null ? role.toLowerCase() : "all");
            }
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                list.add(mapTicker(rs));
            }
        } catch (SQLException e) {
            System.err.println("[NewsTickerDAO] getActiveTickersForRole Error: " + e.getMessage());
        }
        return list;
    }

    public List<NewsTicker> getAllTickers() {
        List<NewsTicker> list = new ArrayList<>();
        String query = "SELECT * FROM news_tickers ORDER BY created_at DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                list.add(mapTicker(rs));
            }
        } catch (SQLException e) {
            System.err.println("[NewsTickerDAO] getAllTickers Error: " + e.getMessage());
        }
        return list;
    }

    public boolean addTicker(String content, String targetRole) {
        String query = "INSERT INTO news_tickers (content, target_role, is_active) VALUES (?, ?, 1)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, content);
            pstmt.setString(2, targetRole != null ? targetRole.toLowerCase() : "all");
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[NewsTickerDAO] addTicker Error: " + e.getMessage());
            return false;
        }
    }

    public boolean updateTicker(int id, String content, String targetRole) {
        String query = "UPDATE news_tickers SET content = ?, target_role = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, content);
            pstmt.setString(2, targetRole != null ? targetRole.toLowerCase() : "all");
            pstmt.setInt(3, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[NewsTickerDAO] updateTicker Error: " + e.getMessage());
            return false;
        }
    }

    public boolean toggleTicker(int id) {
        String query = "UPDATE news_tickers SET is_active = CASE WHEN is_active = 1 THEN 0 ELSE 1 END WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[NewsTickerDAO] toggleTicker Error: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteTicker(int id) {
        String query = "DELETE FROM news_tickers WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[NewsTickerDAO] deleteTicker Error: " + e.getMessage());
            return false;
        }
    }

    private NewsTicker mapTicker(ResultSet rs) throws SQLException {
        NewsTicker t = new NewsTicker();
        t.setId(rs.getInt("id"));
        t.setContent(rs.getString("content"));
        try { t.setTargetRole(rs.getString("target_role")); } catch (Exception ignored) {}
        t.setIsActive(rs.getInt("is_active"));
        t.setCreatedAt(rs.getString("created_at"));
        return t;
    }
}
