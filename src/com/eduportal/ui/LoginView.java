package com.eduportal.ui;

import com.eduportal.dao.UserDAO;
import com.eduportal.models.User;
import com.eduportal.utils.AutoUpdater;
import com.eduportal.utils.SessionManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

public class LoginView extends StackPane {

    private final MainApp mainApp;
    private final UserDAO userDAO = new UserDAO();
    private Button loginBtn;
    private Hyperlink registerLink;
    private Button updateActionBtn;
    private Label updateStatusLabel;
    private AutoUpdater.UpdateStatus updateStatus;

    public LoginView(MainApp mainApp) {
        this.mainApp = mainApp;
        initUI();
    }

    private void initUI() {
        this.getStyleClass().add("background-pane");
        this.setPadding(new Insets(40));

        // Glass Card Container
        VBox card = new VBox(20);
        card.getStyleClass().add("glass-card");
        card.setMaxWidth(420);
        card.setMaxHeight(Region.USE_PREF_SIZE);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(35));

        // Brand Title
        Label logoLabel = new Label("🎓 EduPortal");
        logoLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #F97316;");

        Label subtitleLabel = new Label("Student Management System");
        subtitleLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #A1A1AA;");

        VBox header = new VBox(5, logoLabel, subtitleLabel);
        header.setAlignment(Pos.CENTER);

        // User ID Input (no prompt text)
        VBox idBox = new VBox(6);
        Label idLabel = new Label("User ID");
        idLabel.setStyle("-fx-text-fill: #A1A1AA; -fx-font-size: 12px; -fx-font-weight: bold;");
        TextField idField = new TextField();
        idField.setPrefHeight(40);
        idBox.getChildren().addAll(idLabel, idField);

        // Password Input (no prompt text)
        VBox pwBox = new VBox(6);
        Label pwLabel = new Label("Password");
        pwLabel.setStyle("-fx-text-fill: #A1A1AA; -fx-font-size: 12px; -fx-font-weight: bold;");
        PasswordField pwField = new PasswordField();
        pwField.setPrefHeight(40);
        pwBox.getChildren().addAll(pwLabel, pwField);

        // Error Message Label
        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #F97316; -fx-font-size: 12px;");
        errorLabel.setVisible(false);
        errorLabel.setWrapText(true);

        // Login Button
        loginBtn = new Button("Sign In");
        loginBtn.getStyleClass().add("btn-primary");
        loginBtn.setMaxWidth(Double.MAX_VALUE);
        loginBtn.setPrefHeight(42);
        loginBtn.setDisable(true);

        // Login action
        Runnable loginAction = () -> {
            String userId = idField.getText().trim();
            String password = pwField.getText().trim();

            if (!canSignIn()) return;
            if (userId.isEmpty() || password.isEmpty()) {
                errorLabel.setText("Please enter both User ID and Password.");
                errorLabel.setVisible(true);
                return;
            }

            // Check if user ID exists first
            boolean exists = userDAO.userExists(userId);
            if (!exists) {
                // User ID does not exist — but password could also be wrong
                // Check if both are wrong: since user doesn't exist, we say "User ID is wrong"
                errorLabel.setText("User ID is wrong.");
                errorLabel.setVisible(true);
                return;
            }

            // User ID exists, try to authenticate
            User user = userDAO.authenticate(userId, password);
            if (user != null) {
                errorLabel.setVisible(false);
                SessionManager.setCurrentUser(user);
                mainApp.showMainLayout();
            } else {
                errorLabel.setText("Password is wrong.");
                errorLabel.setVisible(true);
            }
        };

        loginBtn.setOnAction(e -> loginAction.run());

        // Enter key: User ID → focus Password, Password → Sign In
        idField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                pwField.requestFocus();
            }
        });

        pwField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                loginAction.run();
            }
        });

        // Register link
        registerLink = new Hyperlink("Don't have an account? Register here");
        registerLink.setStyle("-fx-text-fill: #F97316; -fx-font-size: 12px;");
        registerLink.setDisable(true);
        registerLink.setOnAction(e -> {
            this.getChildren().clear();
            this.getChildren().add(new RegisterView(mainApp));
        });

        card.getChildren().addAll(header, errorLabel, idBox, pwBox, loginBtn, registerLink);

        VBox updateCard = new VBox(10);
        updateCard.getStyleClass().add("glass-card");
        updateCard.setMaxWidth(420);
        updateCard.setPadding(new Insets(16, 20, 16, 20));
        updateStatusLabel = new Label("Checking for updates...");
        updateStatusLabel.setWrapText(true);
        updateActionBtn = new Button();
        updateActionBtn.setVisible(false);
        updateActionBtn.setManaged(false);
        updateCard.getChildren().addAll(updateStatusLabel, updateActionBtn);

        VBox content = new VBox(14, card, updateCard);
        content.setAlignment(Pos.CENTER);
        content.setMaxWidth(420);
        this.setAlignment(Pos.CENTER);
        this.getChildren().setAll(content);
    }

    public void setUpdateStatus(AutoUpdater.UpdateStatus status) {
        updateStatus = status;
        updateStatusLabel.setText(status.getMessage());
        updateActionBtn.setOnAction(null);

        boolean checking = status.getState() == AutoUpdater.State.CHECKING;
        boolean requiredUpdate = status.getState() == AutoUpdater.State.UPDATE_AVAILABLE
                || status.getState() == AutoUpdater.State.UPDATE_BLOCKED
                || status.getState() == AutoUpdater.State.DOWNLOADING
                || status.getState() == AutoUpdater.State.INSTALLING;
        loginBtn.setDisable(checking || requiredUpdate);
        registerLink.setDisable(checking || requiredUpdate);

        if (status.getState() == AutoUpdater.State.UPDATE_AVAILABLE) {
            updateActionBtn.setText("Update Now");
            updateActionBtn.setDisable(false);
            updateActionBtn.setOnAction(e -> {
                updateActionBtn.setDisable(true);
                AutoUpdater.installUpdateAsync(status,
                        java.nio.file.Path.of(System.getProperty("user.dir")), this::setUpdateStatus);
            });
            setUpdateActionVisible(true);
        } else if (status.getState() == AutoUpdater.State.UPDATE_BLOCKED
                || status.getState() == AutoUpdater.State.FAILED) {
            updateActionBtn.setText("Check Again");
            updateActionBtn.setDisable(false);
            updateActionBtn.setOnAction(e -> {
                updateActionBtn.setDisable(true);
                AutoUpdater.checkForUpdatesAsync(this::setUpdateStatus);
            });
            setUpdateActionVisible(true);
        } else if (status.getState() == AutoUpdater.State.DOWNLOADING
                || status.getState() == AutoUpdater.State.INSTALLING
                || status.getState() == AutoUpdater.State.CHECKING) {
            updateActionBtn.setText(status.getState() == AutoUpdater.State.CHECKING
                    ? "Checking..." : "Please wait...");
            updateActionBtn.setDisable(true);
            setUpdateActionVisible(status.getState() != AutoUpdater.State.CHECKING);
        } else {
            setUpdateActionVisible(false);
        }
    }

    private void setUpdateActionVisible(boolean visible) {
        updateActionBtn.setVisible(visible);
        updateActionBtn.setManaged(visible);
    }

    private boolean canSignIn() {
        return updateStatus != null && (updateStatus.getState() == AutoUpdater.State.CURRENT
                || updateStatus.getState() == AutoUpdater.State.FAILED);
    }
}
