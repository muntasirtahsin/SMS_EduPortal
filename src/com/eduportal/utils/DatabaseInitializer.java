package com.eduportal.utils;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class DatabaseInitializer {

    public static void initializeDatabase() {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            // 1. Users Table
            stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                    "id TEXT PRIMARY KEY, " +
                    "password TEXT NOT NULL, " +
                    "name TEXT NOT NULL, " +
                    "role TEXT NOT NULL CHECK(role IN ('student','teacher','admin')), " +
                    "email TEXT, " +
                    "phone TEXT, " +
                    "department TEXT, " +
                    "email_verified INTEGER DEFAULT 0, " +
                    "email_otp TEXT, " +
                    "current_semester INTEGER DEFAULT 1, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

            // Schema Migration check for users (current_semester)
            try {
                stmt.execute("ALTER TABLE users ADD COLUMN current_semester INTEGER DEFAULT 1");
            } catch (Exception ignored) {}

            // Schema Migration check for users (department)
            try {
                stmt.execute("ALTER TABLE users ADD COLUMN department TEXT");
            } catch (Exception ignored) {}

            // 2. Courses Table
            stmt.execute("CREATE TABLE IF NOT EXISTS courses (" +
                    "course_id TEXT PRIMARY KEY, " +
                    "course_name TEXT NOT NULL, " +
                    "credits INTEGER NOT NULL DEFAULT 3, " +
                    "teacher_id TEXT, " +
                    "semester TEXT NOT NULL DEFAULT 'Spring 2026', " +
                    "target_semester INTEGER NOT NULL DEFAULT 1, " +
                    "FOREIGN KEY (teacher_id) REFERENCES users(id))");

            // Schema Migration check for courses (target_semester)
            try {
                stmt.execute("ALTER TABLE courses ADD COLUMN target_semester INTEGER NOT NULL DEFAULT 1");
            } catch (Exception ignored) {}

            // 3. Enrollments Table
            stmt.execute("CREATE TABLE IF NOT EXISTS enrollments (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "student_id TEXT NOT NULL, " +
                    "course_id TEXT NOT NULL, " +
                    "semester TEXT NOT NULL DEFAULT 'Spring 2026', " +
                    "attendance REAL DEFAULT 0, " +
                    "q1 REAL DEFAULT 0, " +
                    "q2 REAL DEFAULT 0, " +
                    "assignment REAL DEFAULT 0, " +
                    "mid REAL DEFAULT 0, " +
                    "final REAL DEFAULT 0, " +
                    "FOREIGN KEY (student_id) REFERENCES users(id), " +
                    "FOREIGN KEY (course_id) REFERENCES courses(course_id))");

            try {
                stmt.execute("ALTER TABLE enrollments ADD COLUMN result_status TEXT NOT NULL DEFAULT 'draft'");
                stmt.execute("UPDATE enrollments SET result_status = 'pending'");
            } catch (Exception ignored) {}
            try {
                stmt.execute("ALTER TABLE enrollments ADD COLUMN attempt_type TEXT NOT NULL DEFAULT 'regular'");
            } catch (Exception ignored) {}
            try {
                stmt.execute("ALTER TABLE enrollments ADD COLUMN incomplete_due_semester INTEGER");
            } catch (Exception ignored) {}
            try {
                stmt.execute("ALTER TABLE enrollments ADD COLUMN mid_absent INTEGER NOT NULL DEFAULT 0");
            } catch (Exception ignored) {}
            try {
                stmt.execute("ALTER TABLE enrollments ADD COLUMN final_absent INTEGER NOT NULL DEFAULT 0");
            } catch (Exception ignored) {}
            stmt.execute("CREATE TABLE IF NOT EXISTS app_settings (setting_key TEXT PRIMARY KEY, setting_value TEXT NOT NULL)");
            try (ResultSet marksScheme = stmt.executeQuery(
                    "SELECT setting_value FROM app_settings WHERE setting_key = 'weighted_marks_v2'")) {
                if (!marksScheme.next()) {
                    stmt.executeUpdate("UPDATE enrollments SET attendance = COALESCE(attendance, 0) / 2.0, " +
                            "assignment = COALESCE(assignment, 0) / 2.0, mid = COALESCE(mid, 0) * 1.5");
                    stmt.executeUpdate("INSERT INTO app_settings (setting_key, setting_value) VALUES " +
                            "('weighted_marks_v2', '1')");
                }
            }

            stmt.execute("CREATE TABLE IF NOT EXISTS course_applications (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "course_id TEXT NOT NULL, " +
                    "teacher_id TEXT NOT NULL, " +
                    "status TEXT NOT NULL DEFAULT 'pending', " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "UNIQUE(course_id, teacher_id), " +
                    "FOREIGN KEY (course_id) REFERENCES courses(course_id), " +
                    "FOREIGN KEY (teacher_id) REFERENCES users(id))");

            stmt.execute("CREATE TABLE IF NOT EXISTS attendance_records (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "course_id TEXT NOT NULL, " +
                    "student_id TEXT NOT NULL, " +
                    "attendance_date TEXT NOT NULL, " +
                    "status TEXT NOT NULL, " +
                    "marked_by TEXT NOT NULL, " +
                    "UNIQUE(course_id, student_id, attendance_date), " +
                    "FOREIGN KEY (course_id) REFERENCES courses(course_id), " +
                    "FOREIGN KEY (student_id) REFERENCES users(id), " +
                    "FOREIGN KEY (marked_by) REFERENCES users(id))");

            // 4. Messages Table
            stmt.execute("CREATE TABLE IF NOT EXISTS messages (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "sender_id TEXT NOT NULL, " +
                    "receiver_id TEXT NOT NULL, " +
                    "subject TEXT NOT NULL, " +
                    "body TEXT NOT NULL, " +
                    "is_read INTEGER DEFAULT 0, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

            // 5. Notifications Table
            stmt.execute("CREATE TABLE IF NOT EXISTS notifications (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "user_id TEXT NOT NULL, " +
                    "title TEXT NOT NULL, " +
                    "message TEXT NOT NULL, " +
                    "type TEXT DEFAULT 'info', " +
                    "is_read INTEGER DEFAULT 0, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

            // 6. System Deadlines Table (Enhanced)
            stmt.execute("CREATE TABLE IF NOT EXISTS system_deadlines (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "deadline_key TEXT UNIQUE NOT NULL, " +
                    "title TEXT NOT NULL, " +
                    "start_date TEXT, " +
                    "end_date TEXT, " +
                    "is_active INTEGER DEFAULT 1)");

            // 7. News Tickers Table
            stmt.execute("CREATE TABLE IF NOT EXISTS news_tickers (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "content TEXT NOT NULL, " +
                    "target_role TEXT DEFAULT 'all', " +
                    "is_active INTEGER DEFAULT 1, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

            // Schema Migration check for news_tickers (target_role)
            try {
                stmt.execute("ALTER TABLE news_tickers ADD COLUMN target_role TEXT DEFAULT 'all'");
            } catch (Exception ignored) {}

            // 8. Deadline Permissions Table (individual bypass permissions with expiry)
            stmt.execute("CREATE TABLE IF NOT EXISTS deadline_permissions (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "user_id TEXT NOT NULL, " +
                    "deadline_key TEXT NOT NULL, " +
                    "expiry_date TEXT NOT NULL, " +
                    "granted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY (user_id) REFERENCES users(id), " +
                    "FOREIGN KEY (deadline_key) REFERENCES system_deadlines(deadline_key))");

            // --- Seed Initial Data if Users Table is Empty ---
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) AS cnt FROM users");
            if (rs.next() && rs.getInt("cnt") == 0) {
                String adminPw = AuthUtils.hashPassword("@admin1@");
                String userPw  = AuthUtils.hashPassword("password123");

                stmt.execute("INSERT INTO users (id, password, name, role, department, current_semester) VALUES " +
                        "('admin1', '" + adminPw + "', 'System Administrator', 'admin', 'CSE', 1), " +
                        "('teacher1', '" + userPw + "', 'Dr. Alan Turing', 'teacher', 'CSE', 1), " +
                        "('teacher2', '" + userPw + "', 'Dr. Grace Hopper', 'teacher', 'EEE', 1), " +
                        "('student1', '" + userPw + "', 'John Doe', 'student', 'CSE', 1), " +
                        "('student2', '" + userPw + "', 'Jane Smith', 'student', 'BBA', 2)");

                stmt.execute("INSERT INTO courses (course_id, course_name, credits, teacher_id, semester, target_semester) VALUES " +
                        "('CSE101', 'Structured Programming', 3, 'teacher1', 'Spring 2026', 1), " +
                        "('CSE102', 'Calculus & Linear Algebra', 3, 'teacher1', 'Spring 2026', 1), " +
                        "('CSE201', 'Data Structures & Algorithms', 3, 'teacher1', 'Spring 2026', 2), " +
                        "('CSE301', 'Database Systems', 3, 'teacher2', 'Spring 2026', 3), " +
                        "('CSE401', 'Software Engineering', 3, 'teacher2', 'Spring 2026', 4)");

                stmt.execute("INSERT INTO news_tickers (content, target_role, is_active) VALUES " +
                        "('📢 Welcome to EduPortal Student Management System!', 'all', 1), " +
                        "('📅 Spring 2026 Course Registration is officially OPEN.', 'student', 1), " +
                        "('📝 Teachers please submit midterm grades before the deadline.', 'teacher', 1)");

                // Seed welcome briefing messages for initial users
                String welcomeSub = "🎓 Welcome to EduPortal! Important Account Setup Guidelines";
                String welcomeBody = "Welcome to EduPortal!\\n\\n" +
                        "Here is a quick guide to help you navigate and set up your portal account:\\n\\n" +
                        "1. 📧 EMAIL VERIFICATION: Please visit My Profile settings to link and verify your email address. Email verification is required before sending direct messages to staff or students.\\n\\n" +
                        "2. 📅 SYSTEM DEADLINES: All course registrations and grade submissions are governed by strict system deadlines set by management. Always check your portal dashboard for deadline dates. If a deadline has passed, contact management for individual permission.\\n\\n" +
                        "3. 📚 PORTAL FEATURES: You can take courses, view grade reports, manage account settings, and communicate via internal messaging.\\n\\n" +
                        "Best regards,\\n" +
                        "EduPortal System Administration";

                stmt.execute("INSERT INTO messages (sender_id, receiver_id, subject, body, is_read) VALUES " +
                        "('admin1', 'student1', '" + welcomeSub + "', '" + welcomeBody + "', 0), " +
                        "('admin1', 'student2', '" + welcomeSub + "', '" + welcomeBody + "', 0), " +
                        "('admin1', 'teacher1', '" + welcomeSub + "', '" + welcomeBody + "', 0), " +
                        "('admin1', 'teacher2', '" + welcomeSub + "', '" + welcomeBody + "', 0)");
            }

            // Seed Initial System Deadlines if empty (3 fixed types only)
            ResultSet rsDead = stmt.executeQuery("SELECT COUNT(*) AS cnt FROM system_deadlines");
            if (rsDead.next() && rsDead.getInt("cnt") == 0) {
                stmt.execute("INSERT INTO system_deadlines (deadline_key, title, start_date, end_date, is_active) VALUES " +
                        "('student_enroll', 'Student Course Registration', '2026-01-01', '2026-08-31', 1), " +
                        "('teacher_enroll', 'Teacher Course Registration', '2026-01-01', '2026-08-31', 1), " +
                        "('teacher_marks', 'Teacher Result Submission', '2026-01-01', '2026-09-30', 1)");
            }

            System.out.println("[DatabaseInitializer] SQLite Database Initialized & Schema Migrated.");

        } catch (Exception e) {
            System.err.println("[DatabaseInitializer] Error initializing database: " + e.getMessage());
        }
    }
}
