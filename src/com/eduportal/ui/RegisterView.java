package com.eduportal.ui;

import com.eduportal.dao.UserDAO;
import com.eduportal.models.User;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;

public class RegisterView extends StackPane {

    private final MainApp mainApp;
    private final UserDAO userDAO = new UserDAO();

    public RegisterView(MainApp mainApp) {
        this.mainApp = mainApp;
        initUI();
    }

    private void initUI() {
        this.getStyleClass().add("background-pane");
        this.setPadding(new Insets(40));

        VBox card = new VBox(16);
        card.getStyleClass().add("glass-card");
        card.setMaxWidth(450);
        card.setMaxHeight(560);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(30));

        // Brand Title
        Label logoLabel = new Label("🎓 EduPortal");
        logoLabel.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #F97316;");

        Label subtitleLabel = new Label("Create a New Account");
        subtitleLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #A1A1AA;");

        VBox header = new VBox(5, logoLabel, subtitleLabel);
        header.setAlignment(Pos.CENTER);

        // Full Name
        VBox nameBox = new VBox(4);
        Label nameLabel = new Label("Full Name");
        nameLabel.setStyle("-fx-text-fill: #A1A1AA; -fx-font-size: 12px; -fx-font-weight: bold;");
        TextField nameField = new TextField();
        nameField.setPrefHeight(38);
        nameBox.getChildren().addAll(nameLabel, nameField);

        // Password
        VBox pwBox = new VBox(4);
        Label pwLabel = new Label("Set Password");
        pwLabel.setStyle("-fx-text-fill: #A1A1AA; -fx-font-size: 12px; -fx-font-weight: bold;");
        PasswordField pwField = new PasswordField();
        pwField.setPrefHeight(38);
        pwBox.getChildren().addAll(pwLabel, pwField);

        // Confirm Password
        VBox cpwBox = new VBox(4);
        Label cpwLabel = new Label("Confirm Password");
        cpwLabel.setStyle("-fx-text-fill: #A1A1AA; -fx-font-size: 12px; -fx-font-weight: bold;");
        PasswordField cpwField = new PasswordField();
        cpwField.setPrefHeight(38);
        cpwBox.getChildren().addAll(cpwLabel, cpwField);

        // Role Selector
        VBox roleBox = new VBox(4);
        Label roleLabel = new Label("Account Type");
        roleLabel.setStyle("-fx-text-fill: #A1A1AA; -fx-font-size: 12px; -fx-font-weight: bold;");
        ComboBox<String> roleCombo = new ComboBox<>();
        roleCombo.getItems().addAll("Student", "Teacher");
        roleCombo.setValue("Student");
        roleCombo.setMaxWidth(Double.MAX_VALUE);
        roleBox.getChildren().addAll(roleLabel, roleCombo);

        // Department Selector
        VBox deptBox = new VBox(4);
        Label deptLabel = new Label("Department");
        deptLabel.setStyle("-fx-text-fill: #A1A1AA; -fx-font-size: 12px; -fx-font-weight: bold;");
        ComboBox<String> deptCombo = new ComboBox<>();
        deptCombo.getItems().addAll("CSE", "EEE", "BBA", "English", "Law", "Pharmacy", "CIVIL", "Architecture");
        deptCombo.setValue("CSE");
        deptCombo.setMaxWidth(Double.MAX_VALUE);
        deptBox.getChildren().addAll(deptLabel, deptCombo);

        // Status / Error
        Label statusLabel = new Label();
        statusLabel.setWrapText(true);
        statusLabel.setStyle("-fx-font-size: 12px;");

        // Register Button
        Button registerBtn = new Button("Create Account");
        registerBtn.getStyleClass().add("btn-primary");
        registerBtn.setMaxWidth(Double.MAX_VALUE);
        registerBtn.setPrefHeight(42);

        registerBtn.setOnAction(e -> {
            String name = nameField.getText().trim();
            String pw = pwField.getText().trim();
            String cpw = cpwField.getText().trim();
            String role = roleCombo.getValue().toLowerCase();
            String dept = deptCombo.getValue();

            if (name.isEmpty() || pw.isEmpty() || cpw.isEmpty()) {
                statusLabel.setText("Please fill in all fields.");
                statusLabel.setStyle("-fx-text-fill: #F97316; -fx-font-size: 12px;");
                return;
            }

            if (pw.length() < 4) {
                statusLabel.setText("Password must be at least 4 characters.");
                statusLabel.setStyle("-fx-text-fill: #F97316; -fx-font-size: 12px;");
                return;
            }

            if (!pw.equals(cpw)) {
                statusLabel.setText("Passwords do not match.");
                statusLabel.setStyle("-fx-text-fill: #F97316; -fx-font-size: 12px;");
                return;
            }

            // Auto-generate ID
            String generatedId = userDAO.getNextId(role);

            User newUser = new User();
            newUser.setId(generatedId);
            newUser.setPassword(pw);
            newUser.setName(name);
            newUser.setRole(role);
            newUser.setDepartment(dept);
            newUser.setCurrentSemester(1);

            boolean ok = userDAO.createUser(newUser);
            if (ok) {
                statusLabel.setText("✅ Account created! Your User ID is: " + generatedId + ". Please remember this ID to sign in.");
                statusLabel.setStyle("-fx-text-fill: #FDBA74; -fx-font-size: 13px; -fx-font-weight: bold;");
                // Disable form
                nameField.setDisable(true);
                pwField.setDisable(true);
                cpwField.setDisable(true);
                roleCombo.setDisable(true);
                deptCombo.setDisable(true);
                registerBtn.setDisable(true);
            } else {
                statusLabel.setText("Registration failed. Please try again.");
                statusLabel.setStyle("-fx-text-fill: #F97316; -fx-font-size: 12px;");
            }
        });

        // Back to Login link
        Hyperlink backLink = new Hyperlink("Already have an account? Sign In");
        backLink.setStyle("-fx-text-fill: #F97316; -fx-font-size: 12px;");
        backLink.setOnAction(e -> mainApp.showLoginView());

        card.getChildren().addAll(header, statusLabel, nameBox, pwBox, cpwBox, roleBox, deptBox, registerBtn, backLink);
        this.getChildren().add(card);
    }
}
