package com.eduportal.ui;

import com.eduportal.dao.UserDAO;
import com.eduportal.models.User;
import com.eduportal.utils.SessionManager;

import javax.swing.*;
import java.awt.*;

public class LoginFrame extends JFrame {
    private JTextField idField;
    private JPasswordField passwordField;
    private JButton loginButton;

    public LoginFrame() {
        setTitle("EduPortal Login");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);

        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("User ID:"), gbc);

        gbc.gridx = 1;
        idField = new JTextField(15);
        panel.add(idField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Password:"), gbc);

        gbc.gridx = 1;
        passwordField = new JPasswordField(15);
        panel.add(passwordField, gbc);

        gbc.gridx = 1; gbc.gridy = 2;
        loginButton = new JButton("Login");
        panel.add(loginButton, gbc);

        loginButton.addActionListener(e -> attemptLogin());

        add(panel);
    }

    private void attemptLogin() {
        String id = idField.getText();
        String password = new String(passwordField.getPassword());
        
        loginButton.setEnabled(false);
        loginButton.setText("Logging in...");
        
        // Run login in background thread to not freeze UI while python script runs
        SwingWorker<User, Void> worker = new SwingWorker<User, Void>() {
            @Override
            protected User doInBackground() throws Exception {
                UserDAO dao = new UserDAO();
                return dao.authenticate(id, password);
            }

            @Override
            protected void done() {
                try {
                    User user = get();
                    if (user != null) {
                        SessionManager.setCurrentUser(user);
                        JOptionPane.showMessageDialog(LoginFrame.this, "Login Successful! Role: " + user.getRole());
                        new MainFrame().setVisible(true);
                        dispose();
                    } else {
                        JOptionPane.showMessageDialog(LoginFrame.this, "Invalid ID or Password", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                } finally {
                    loginButton.setEnabled(true);
                    loginButton.setText("Login");
                }
            }
        };
        worker.execute();
    }
}
