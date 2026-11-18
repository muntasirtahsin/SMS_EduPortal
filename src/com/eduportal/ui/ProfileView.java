package com.eduportal.ui;

import com.eduportal.dao.UserDAO;
import com.eduportal.models.User;
import com.eduportal.services.EmailService;
import com.eduportal.utils.SessionManager;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Random;

public class ProfileView extends VBox {

    private final MainLayout mainLayout;
    private final UserDAO userDAO = new UserDAO();
    private String generatedEmailOtp = null;
    private String generatedPassOtp = null;

    public ProfileView(MainLayout mainLayout) {
        this.mainLayout = mainLayout;
        initUI();
    }

    private void initUI() {
        this.setSpacing(20);
        this.setPadding(new Insets(10));

        User currentUser = SessionManager.getCurrentUser();
        if (currentUser == null) return;

        Label title = new Label("My Profile & Account Settings");
        title.getStyleClass().add("page-title");

        Label subtitle = new Label("Manage your personal details, email verification status, and account security.");
        subtitle.getStyleClass().add("page-subtitle");

        VBox header = new VBox(5, title, subtitle);
        this.getChildren().add(header);

        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(20);

        // Column 1: Personal Details & Password Change
        VBox leftCol = new VBox(20);
        leftCol.getChildren().add(createPersonalDetailsSection(currentUser));
        leftCol.getChildren().add(createPasswordChangeSection(currentUser));

        // Column 2: Email Management & System Mail (for Admin)
        VBox rightCol = new VBox(20);
        rightCol.getChildren().add(createEmailManagementSection(currentUser));
        if ("admin".equals(currentUser.getRole())) {
            rightCol.getChildren().add(createSystemMailSection());
        }

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(50);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(50);
        grid.getColumnConstraints().addAll(col1, col2);

        grid.add(leftCol, 0, 0);
        grid.add(rightCol, 1, 0);

        this.getChildren().add(grid);
    }

    private VBox createPersonalDetailsSection(User currentUser) {
        VBox card = new VBox(15);
        card.getStyleClass().add("glass-card");

        Label infoLabel = new Label("👤 Personal Details");
        infoLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: -fx-base-primary;");

        Label idLabel = new Label("User ID: " + currentUser.getId() + " (" + currentUser.getRole().toUpperCase() + ")");
        idLabel.getStyleClass().add("text-muted");

        TextField nameField = new TextField(currentUser.getName());
        nameField.setPromptText("Full Name");

        PhoneNumberField phoneField = new PhoneNumberField(currentUser.getPhone());

        Button saveInfoBtn = new Button("Save Profile Details");
        saveInfoBtn.getStyleClass().add("btn-primary");

        Label infoStatus = new Label();

        saveInfoBtn.setOnAction(e -> {
            if (!phoneField.isValid()) {
                infoStatus.setText("Enter a valid phone number for the selected country and calling code.");
                infoStatus.setStyle("-fx-text-fill: #F97316;");
                return;
            }
            String phone = phoneField.getInternationalNumber();
            if (userDAO.isPhoneNumberInUse(phone, currentUser.getId())) {
                infoStatus.setText("That phone number is already used by another account.");
                infoStatus.setStyle("-fx-text-fill: #F97316;");
                return;
            }
            boolean ok = userDAO.updateUserProfile(currentUser.getId(), nameField.getText().trim(), phone);
            if (ok) {
                currentUser.setName(nameField.getText().trim());
                currentUser.setPhone(phone);
                infoStatus.setText("Profile updated successfully!");
                infoStatus.setStyle("-fx-text-fill: #FDBA74;");
            } else {
                infoStatus.setText("Failed to update profile.");
                infoStatus.setStyle("-fx-text-fill: #F97316;");
            }
        });

        Label deptLabel = new Label("Department: " + (currentUser.getDepartment() != null ? currentUser.getDepartment() : "Not Set"));
        deptLabel.getStyleClass().add("text-muted");

        card.getChildren().addAll(infoLabel, infoStatus, idLabel, deptLabel, new Label("Full Name:"), nameField,
                new Label("Phone Number (select country code and omit local trunk prefix):"), phoneField, saveInfoBtn);
        return card;
    }

    private VBox createPasswordChangeSection(User currentUser) {
        VBox card = new VBox(12);
        card.getStyleClass().add("glass-card");

        Label label = new Label("🔒 Security & Password Change");
        label.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: -fx-base-primary;");

        Label desc = new Label(currentUser.isEmailVerified() ?
                "Since your email is verified, an OTP code will be sent to your email to authorize password changes." :
                "Your email is unverified. You can change your password directly.");
        desc.getStyleClass().add("text-muted");
        desc.setWrapText(true);

        PasswordField newPassField = new PasswordField();
        newPassField.setPromptText("Enter New Password");

        PasswordField confirmPassField = new PasswordField();
        confirmPassField.setPromptText("Confirm New Password");

        Label statusMsg = new Label();

        if (currentUser.isEmailVerified()) {
            Button requestOtpBtn = new Button("Request OTP to Change Password");
            requestOtpBtn.getStyleClass().add("btn-primary");

            TextField passOtpInput = new TextField();
            passOtpInput.setPromptText("Enter 6-Digit OTP");
            passOtpInput.setVisible(false);

            Button changePassBtn = new Button("Confirm OTP & Save Password");
            changePassBtn.getStyleClass().add("btn-success");
            changePassBtn.setVisible(false);

            requestOtpBtn.setOnAction(e -> {
                generatedPassOtp = String.format("%06d", new Random().nextInt(900000) + 100000);
                boolean sent = EmailService.sendOtpEmail(currentUser.getEmail(), generatedPassOtp);
                if (sent) {
                    statusMsg.setText("OTP sent to " + currentUser.getEmail() + "! Check your inbox.");
                    statusMsg.setStyle("-fx-text-fill: #FDBA74;");
                    passOtpInput.setVisible(true);
                    changePassBtn.setVisible(true);
                } else {
                    statusMsg.setText("Failed to send OTP email. Check system mail credentials.");
                    statusMsg.setStyle("-fx-text-fill: #F97316;");
                }
            });

            changePassBtn.setOnAction(e -> {
                String enteredOtp = passOtpInput.getText().trim();
                String newPass = newPassField.getText().trim();
                String confirmPass = confirmPassField.getText().trim();

                if (!enteredOtp.equals(generatedPassOtp)) {
                    statusMsg.setText("Incorrect OTP code. Try again.");
                    statusMsg.setStyle("-fx-text-fill: #F97316;");
                    return;
                }
                if (newPass.length() < 4 || !newPass.equals(confirmPass)) {
                    statusMsg.setText("Passwords must match and be at least 4 characters long.");
                    statusMsg.setStyle("-fx-text-fill: #F97316;");
                    return;
                }

                boolean ok = userDAO.updatePassword(currentUser.getId(), newPass);
                if (ok) {
                    statusMsg.setText("Password changed successfully!");
                    statusMsg.setStyle("-fx-text-fill: #FDBA74;");
                    newPassField.clear(); confirmPassField.clear(); passOtpInput.clear();
                }
            });

            card.getChildren().addAll(label, statusMsg, desc, requestOtpBtn, passOtpInput, newLabel("New Password:"), newPassField, newLabel("Confirm Password:"), confirmPassField, changePassBtn);

        } else {
            Button directSaveBtn = new Button("Save New Password");
            directSaveBtn.getStyleClass().add("btn-primary");

            directSaveBtn.setOnAction(e -> {
                String newPass = newPassField.getText().trim();
                String confirmPass = confirmPassField.getText().trim();

                if (newPass.length() < 4 || !newPass.equals(confirmPass)) {
                    statusMsg.setText("Passwords must match and be at least 4 characters long.");
                    statusMsg.setStyle("-fx-text-fill: #F97316;");
                    return;
                }

                boolean ok = userDAO.updatePassword(currentUser.getId(), newPass);
                if (ok) {
                    statusMsg.setText("Password changed successfully!");
                    statusMsg.setStyle("-fx-text-fill: #FDBA74;");
                    newPassField.clear(); confirmPassField.clear();
                } else {
                    statusMsg.setText("Failed to update password.");
                    statusMsg.setStyle("-fx-text-fill: #F97316;");
                }
            });

            card.getChildren().addAll(label, statusMsg, desc, newLabel("New Password:"), newPassField, newLabel("Confirm Password:"), confirmPassField, directSaveBtn);
        }

        return card;
    }

    private VBox createEmailManagementSection(User currentUser) {
        VBox card = new VBox(15);
        card.getStyleClass().add("glass-card");

        Label emailLabel = new Label("✉ Email Verification & Management");
        emailLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: -fx-base-primary;");

        if (currentUser.isEmailVerified()) {
            Label verifiedBadge = new Label("✓ Email Verified: " + currentUser.getEmail());
            verifiedBadge.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #FDBA74;");

            Label notice = new Label("Only 1 email address is allowed per account. You can remove this email to bind a new address.");
            notice.getStyleClass().add("text-muted");
            notice.setWrapText(true);

            Button removeEmailBtn = new Button("🗑 Remove Verified Email");
            removeEmailBtn.getStyleClass().add("btn-danger");

            removeEmailBtn.setOnAction(e -> {
                userDAO.removeEmail(currentUser.getId());
                currentUser.setEmail(null);
                currentUser.setEmailVerified(0);
                mainLayout.setView(new ProfileView(mainLayout));
            });

            card.getChildren().addAll(emailLabel, verifiedBadge, notice, removeEmailBtn);

        } else {
            Label statusDesc = new Label("Link an email address to enable OTP password security and send messages.");
            statusDesc.getStyleClass().add("text-muted");
            statusDesc.setWrapText(true);

            TextField emailInput = new TextField(currentUser.getEmail() != null ? currentUser.getEmail() : "");
            emailInput.setPromptText("Enter Email Address (e.g. user@gmail.com)");

            Button sendOtpBtn = new Button("Send OTP Verification Email");
            sendOtpBtn.getStyleClass().add("btn-primary");

            TextField otpInput = new TextField();
            otpInput.setPromptText("Enter 6-Digit OTP Code");
            otpInput.setVisible(false);

            Button verifyBtn = new Button("Verify OTP");
            verifyBtn.getStyleClass().add("btn-success");
            verifyBtn.setVisible(false);

            Label emailStatus = new Label();

            sendOtpBtn.setOnAction(e -> {
                String mail = emailInput.getText().trim();
                if (!mail.contains("@") || !mail.contains(".")) {
                    emailStatus.setText("Please enter a valid email address.");
                    emailStatus.setStyle("-fx-text-fill: #F97316;");
                    return;
                }

                generatedEmailOtp = String.format("%06d", new Random().nextInt(900000) + 100000);
                boolean sent = EmailService.sendOtpEmail(mail, generatedEmailOtp);
                if (sent) {
                    userDAO.updateEmailAndOtp(currentUser.getId(), mail, generatedEmailOtp);
                    emailStatus.setText("OTP sent to " + mail + "! Check your inbox.");
                    emailStatus.setStyle("-fx-text-fill: #FDBA74;");
                    otpInput.setVisible(true);
                    verifyBtn.setVisible(true);
                } else {
                    emailStatus.setText("Failed to send OTP email. Verify system mail credentials.");
                    emailStatus.setStyle("-fx-text-fill: #F97316;");
                }
            });

            verifyBtn.setOnAction(e -> {
                String entered = otpInput.getText().trim();
                if (entered.equals(generatedEmailOtp)) {
                    userDAO.verifyEmail(currentUser.getId());
                    currentUser.setEmailVerified(1);
                    currentUser.setEmail(emailInput.getText().trim());
                    emailStatus.setText("Email verified successfully!");
                    emailStatus.setStyle("-fx-text-fill: #FDBA74;");
                    mainLayout.setView(new ProfileView(mainLayout));
                } else {
                    emailStatus.setText("Incorrect OTP code. Please try again.");
                    emailStatus.setStyle("-fx-text-fill: #F97316;");
                }
            });

            card.getChildren().addAll(emailLabel, emailStatus, statusDesc, emailInput, sendOtpBtn, otpInput, verifyBtn);
        }

        return card;
    }

    private VBox createSystemMailSection() {
        VBox box = new VBox(12);
        box.getStyleClass().add("glass-card");

        Label label = new Label("⚙ System Sender Mail & App Key Credentials");
        label.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: -fx-base-primary;");

        Label desc = new Label("Configure the system Gmail sender address and 16-character App Password key. OTP verification emails are sent from this account.");
        desc.getStyleClass().add("text-muted");
        desc.setWrapText(true);

        boolean hasCustom = EmailService.hasCustomCredentials();
        String currentEmail = EmailService.getSystemSenderEmail();
        String currentPass = EmailService.getSystemAppPassword();

        Label badge = new Label(hasCustom ? "✓ Active Custom Mail: " + currentEmail : "ℹ System Default Mail: " + currentEmail);
        badge.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + (hasCustom ? "#FDBA74;" : "#F97316;"));

        TextField emailField = new TextField(hasCustom ? currentEmail : "");
        emailField.setPromptText("Sender Gmail Address (e.g. sender@gmail.com)");

        PasswordField keyField = new PasswordField();
        if (hasCustom) keyField.setText(currentPass);
        keyField.setPromptText("16-character Gmail App Password key");

        Button saveBtn = new Button(hasCustom ? "💾 Update Mail Credentials" : "💾 Save Mail Credentials");
        saveBtn.getStyleClass().add("btn-primary");

        Button removeBtn = new Button("🗑 Remove Mail Credentials");
        removeBtn.getStyleClass().add("btn-danger");
        removeBtn.setVisible(hasCustom);

        Label statusMsg = new Label();

        saveBtn.setOnAction(e -> {
            String mail = emailField.getText().trim();
            String pass = keyField.getText().replace(" ", "").trim();

            if (mail.isEmpty() || pass.isEmpty()) {
                statusMsg.setText("Please fill in both sender email and app password.");
                statusMsg.setStyle("-fx-text-fill: #F97316;");
                return;
            }

            try {
                File envFile = new File(".env");
                try (OutputStream os = new FileOutputStream(envFile)) {
                    String content = "# EduPortal Credentials\nGMAIL_USER=" + mail + "\nGMAIL_PASS=" + pass + "\n";
                    os.write(content.getBytes());
                }
                statusMsg.setText("✅ Mail credentials saved successfully!");
                statusMsg.setStyle("-fx-text-fill: #FDBA74; -fx-font-weight: bold;");
                mainLayout.setView(new ProfileView(mainLayout));
            } catch (Exception ex) {
                statusMsg.setText("Error saving credentials: " + ex.getMessage());
                statusMsg.setStyle("-fx-text-fill: #F97316;");
            }
        });

        removeBtn.setOnAction(e -> {
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Remove System Mail Credentials");
            confirmAlert.setHeaderText("Remove saved system sender credentials?");
            confirmAlert.setContentText("Are you sure you want to remove the current system sender mail and app password?\nSystem will fallback to default environment configuration.");

            var result = confirmAlert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                boolean ok = EmailService.removeMailCredentials();
                if (ok) {
                    statusMsg.setText("✅ Saved mail credentials removed successfully!");
                    statusMsg.setStyle("-fx-text-fill: #FDBA74; -fx-font-weight: bold;");
                    mainLayout.setView(new ProfileView(mainLayout));
                } else {
                    statusMsg.setText("Failed to remove credentials.");
                    statusMsg.setStyle("-fx-text-fill: #F97316;");
                }
            }
        });

        HBox btnBox = new HBox(10, saveBtn, removeBtn);
        box.getChildren().addAll(label, statusMsg, desc, badge, new Label("Sender Gmail Address:"), emailField, new Label("16-Character App Password:"), keyField, btnBox);
        return box;
    }

    private Label newLabel(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("text-main");
        return l;
    }
}
