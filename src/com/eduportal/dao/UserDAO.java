package com.eduportal.dao;

import com.eduportal.models.Message;
import com.eduportal.models.User;
import com.eduportal.utils.AuthUtils;
import com.eduportal.utils.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    public User authenticate(String id, String password) {
        String query = "SELECT * FROM users WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String storedPw = rs.getString("password");
                if (AuthUtils.checkPassword(storedPw, password)) {
                    return mapUser(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] Auth Error: " + e.getMessage());
        }
        return null;
    }

    /** Check if a user ID exists in the database */
    public boolean userExists(String id) {
        String query = "SELECT COUNT(*) AS cnt FROM users WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getInt("cnt") > 0;
        } catch (SQLException e) {
            System.err.println("[UserDAO] userExists Error: " + e.getMessage());
        }
        return false;
    }

    public User getUserById(String id) {
        String query = "SELECT * FROM users WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return mapUser(rs);
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] getUserById Error: " + e.getMessage());
        }
        return null;
    }

    public List<User> getAllUsers() {
        List<User> list = new ArrayList<>();
        String query = "SELECT * FROM users ORDER BY role, id";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                list.add(mapUser(rs));
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] getAllUsers Error: " + e.getMessage());
        }
        return list;
    }

    public List<User> getUsersByRole(String role) {
        List<User> list = new ArrayList<>();
        String query = "SELECT * FROM users WHERE role = ? ORDER BY id";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, role);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                list.add(mapUser(rs));
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] getUsersByRole Error: " + e.getMessage());
        }
        return list;
    }

    public List<User> searchUsers(String keyword) {
        List<User> list = new ArrayList<>();
        String query = "SELECT * FROM users WHERE LOWER(id) LIKE ? OR LOWER(name) LIKE ? OR LOWER(role) LIKE ? ORDER BY role, id";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            String k = "%" + keyword.toLowerCase() + "%";
            pstmt.setString(1, k);
            pstmt.setString(2, k);
            pstmt.setString(3, k);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                list.add(mapUser(rs));
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] searchUsers Error: " + e.getMessage());
        }
        return list;
    }

    public boolean updateUserProfile(String id, String name, String phone) {
        if (phone != null && isPhoneNumberInUse(phone, id)) {
            return false;
        }
        String query = "UPDATE users SET name = ?, phone = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, name);
            pstmt.setString(2, phone);
            pstmt.setString(3, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[UserDAO] updateUserProfile Error: " + e.getMessage());
            return false;
        }
    }

    public boolean isPhoneNumberInUse(String phone, String excludedUserId) {
        if (phone == null || phone.trim().isEmpty()) return false;
        String normalized = phone.replaceAll("\\D", "");
        String query = "SELECT id, phone FROM users WHERE phone IS NOT NULL AND id <> ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, excludedUserId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String existingPhone = rs.getString("phone");
                    if (existingPhone != null && normalized.equals(existingPhone.replaceAll("\\D", ""))) {
                        return true;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] isPhoneNumberInUse Error: " + e.getMessage());
            return true;
        }
        return false;
    }

    public boolean updateDepartment(String id, String department) {
        String query = "UPDATE users SET department = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, department);
            pstmt.setString(2, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[UserDAO] updateDepartment Error: " + e.getMessage());
            return false;
        }
    }

    public boolean updateStudentSemester(String id, int semester) {
        String query = "UPDATE users SET current_semester = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, semester);
            pstmt.setString(2, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[UserDAO] updateStudentSemester Error: " + e.getMessage());
            return false;
        }
    }

    public boolean updateEmailAndOtp(String id, String email, String otp) {
        String query = "UPDATE users SET email = ?, email_otp = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, email);
            pstmt.setString(2, otp);
            pstmt.setString(3, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[UserDAO] updateEmailAndOtp Error: " + e.getMessage());
            return false;
        }
    }

    public boolean verifyEmail(String id) {
        String query = "UPDATE users SET email_verified = 1, email_otp = NULL WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[UserDAO] verifyEmail Error: " + e.getMessage());
            return false;
        }
    }

    public boolean removeEmail(String id) {
        String query = "UPDATE users SET email = NULL, email_verified = 0, email_otp = NULL WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[UserDAO] removeEmail Error: " + e.getMessage());
            return false;
        }
    }

    public boolean updatePassword(String id, String newPassword) {
        String hashedPw = AuthUtils.hashPassword(newPassword);
        String query = "UPDATE users SET password = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, hashedPw);
            pstmt.setString(2, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[UserDAO] updatePassword Error: " + e.getMessage());
            return false;
        }
    }

    public boolean createUser(User u) {
        String query = "INSERT INTO users (id, password, name, role, email, phone, department, current_semester) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, u.getId());
            pstmt.setString(2, AuthUtils.hashPassword(u.getPassword()));
            pstmt.setString(3, u.getName());
            pstmt.setString(4, u.getRole());
            pstmt.setString(5, u.getEmail());
            pstmt.setString(6, u.getPhone());
            pstmt.setString(7, u.getDepartment());
            pstmt.setInt(8, u.getCurrentSemester() > 0 ? u.getCurrentSemester() : 1);
            boolean ok = pstmt.executeUpdate() > 0;
            if (ok) {
                sendWelcomeMessage(u.getId(), u.getName());
            }
            return ok;
        } catch (SQLException e) {
            System.err.println("[UserDAO] createUser Error: " + e.getMessage());
            return false;
        }
    }

    public void sendWelcomeMessage(String userId, String userName) {
        MessageDAO messageDAO = new MessageDAO();
        String subject = "🎓 Welcome to EduPortal! Important Account Setup Guidelines";
        String body = "Welcome to EduPortal, " + (userName != null ? userName : "User") + "!\n\n" +
                "Here is a quick guide to help you navigate and set up your portal account:\n\n" +
                "1. 📧 EMAIL VERIFICATION: Please visit 'My Profile' settings to link and verify your email address. Email verification is required before sending direct messages to staff or students.\n\n" +
                "2. 📅 SYSTEM DEADLINES: All course registrations and grade submissions are governed by strict system deadlines set by management. Always check your portal dashboard for deadline dates. If a deadline has passed, contact management for individual permission.\n\n" +
                "3. 📚 PORTAL FEATURES: You can take courses, view grade reports, manage account settings, and communicate via internal messaging.\n\n" +
                "Best regards,\n" +
                "EduPortal System Administration";
        messageDAO.sendMessage(new Message("admin1", userId, subject, body));
    }

    public String getNextId(String role) {
        String prefix = role.toLowerCase();
        String query = "SELECT id FROM users WHERE role = ?";
        int max = 0;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, role.toLowerCase());
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                String id = rs.getString("id");
                if (id.startsWith(prefix)) {
                    try {
                        int num = Integer.parseInt(id.substring(prefix.length()));
                        if (num > max) max = num;
                    } catch (Exception ignored) {}
                }
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] getNextId Error: " + e.getMessage());
        }
        return prefix + (max + 1);
    }

    public boolean deleteUser(String id) {
        String query = "DELETE FROM users WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[UserDAO] deleteUser Error: " + e.getMessage());
            return false;
        }
    }

    private User mapUser(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getString("id"));
        u.setPassword(rs.getString("password"));
        u.setName(rs.getString("name"));
        u.setRole(rs.getString("role"));
        u.setEmail(rs.getString("email"));
        u.setPhone(rs.getString("phone"));
        u.setEmailVerified(rs.getInt("email_verified"));
        u.setEmailOtp(rs.getString("email_otp"));
        try { u.setDepartment(rs.getString("department")); } catch (Exception ignored) {}
        try { u.setCurrentSemester(rs.getInt("current_semester")); } catch (Exception ignored) {}
        try { u.setCreatedAt(rs.getString("created_at")); } catch (Exception ignored) {}
        return u;
    }
}
