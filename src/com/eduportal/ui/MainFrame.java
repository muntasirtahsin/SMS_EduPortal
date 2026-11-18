package com.eduportal.ui;

import com.eduportal.models.User;
import com.eduportal.utils.SessionManager;

import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {
    public MainFrame() {
        User user = SessionManager.getCurrentUser();
        setTitle("EduPortal - " + user.getName() + " (" + user.getRole() + ")");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JMenuBar menuBar = new JMenuBar();
        JMenu accountMenu = new JMenu("Account");
        JMenuItem logoutItem = new JMenuItem("Logout");
        logoutItem.addActionListener(e -> {
            SessionManager.logout();
            new LoginFrame().setVisible(true);
            dispose();
        });
        accountMenu.add(logoutItem);
        menuBar.add(accountMenu);
        setJMenuBar(menuBar);

        JPanel mainPanel = new JPanel(new BorderLayout());
        JLabel welcomeLabel = new JLabel("Welcome to the " + user.getRole() + " dashboard, " + user.getName(), SwingConstants.CENTER);
        welcomeLabel.setFont(new Font("Arial", Font.BOLD, 20));
        mainPanel.add(welcomeLabel, BorderLayout.CENTER);
        
        add(mainPanel);
    }
}
