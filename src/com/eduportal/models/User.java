package com.eduportal.models;

public class User {
    private String id;
    private String password;
    private String name;
    private String role;
    private String email;
    private String phone;
    private int emailVerified;
    private String emailOtp;
    private int currentSemester = 1;
    private String department;
    private String createdAt;

    public User() {}

    public User(String id, String name, String role, String email, String phone, int emailVerified) {
        this.id = id;
        this.name = name;
        this.role = role;
        this.email = email;
        this.phone = phone;
        this.emailVerified = emailVerified;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public int getEmailVerified() { return emailVerified; }
    public boolean isEmailVerified() { return emailVerified == 1; }
    public void setEmailVerified(int emailVerified) { this.emailVerified = emailVerified; }

    public String getEmailOtp() { return emailOtp; }
    public void setEmailOtp(String emailOtp) { this.emailOtp = emailOtp; }

    public int getCurrentSemester() { return currentSemester; }
    public void setCurrentSemester(int currentSemester) { this.currentSemester = currentSemester; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
