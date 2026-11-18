package com.eduportal.utils;

import com.eduportal.models.User;

public class SessionManager {
    private static User currentUser;
    private static boolean darkMode = true;

    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static boolean isLoggedIn() {
        return currentUser != null;
    }

    public static void logout() {
        currentUser = null;
    }

    public static boolean isDarkMode() {
        return darkMode;
    }

    public static void setDarkMode(boolean mode) {
        darkMode = mode;
    }
}
