package com.eduportal.ui;

import com.eduportal.dao.MessageDAO;
import com.eduportal.dao.UserDAO;
import com.eduportal.models.Message;
import com.eduportal.models.User;
import com.eduportal.utils.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessagesView extends VBox {

    private final MainLayout mainLayout;
    private final MessageDAO messageDAO = new MessageDAO();
    private final UserDAO userDAO = new UserDAO();

    private BorderPane contentPane;
    private Button inboxTabBtn;
    private Button sentTabBtn;
    private Button composeTabBtn;

    public MessagesView(MainLayout mainLayout) {
        this.mainLayout = mainLayout;
        initUI();
    }

    private void initUI() {
        this.setSpacing(20);
        this.setPadding(new Insets(10));
        VBox.setVgrow(this, Priority.ALWAYS);

        User currentUser = SessionManager.getCurrentUser();
        if (currentUser == null) return;

        Label title = new Label("Messages & Communication Portal");
        title.getStyleClass().add("page-title");

        Label subtitle = new Label("Send direct messages to students, teachers, and system administrators.");
        subtitle.getStyleClass().add("page-subtitle");

        VBox header = new VBox(5, title, subtitle);
        this.getChildren().add(header);

        // Web Portal Segmented Tab Bar
        HBox segmentedBar = new HBox(8);
        segmentedBar.getStyleClass().add("segmented-bar");

        inboxTabBtn = new Button("📥 Inbox");
        inboxTabBtn.getStyleClass().addAll("segmented-btn", "segmented-btn-active");

        sentTabBtn = new Button("📤 Sent Messages");
        sentTabBtn.getStyleClass().add("segmented-btn");

        composeTabBtn = new Button("✏ Compose Message");
        composeTabBtn.getStyleClass().add("segmented-btn");

        segmentedBar.getChildren().addAll(inboxTabBtn, sentTabBtn, composeTabBtn);
        this.getChildren().add(segmentedBar);

        contentPane = new BorderPane();
        VBox.setVgrow(contentPane, Priority.ALWAYS);
        this.getChildren().add(contentPane);

        inboxTabBtn.setOnAction(e -> {
            setActiveTab(inboxTabBtn);
            contentPane.setCenter(createInboxView(SessionManager.getCurrentUser()));
        });

        sentTabBtn.setOnAction(e -> {
            setActiveTab(sentTabBtn);
            contentPane.setCenter(createSentView(SessionManager.getCurrentUser()));
        });

        composeTabBtn.setOnAction(e -> {
            setActiveTab(composeTabBtn);
            contentPane.setCenter(createComposeView(SessionManager.getCurrentUser()));
        });

        // Default to Inbox
        contentPane.setCenter(createInboxView(currentUser));
    }

    private void setActiveTab(Button activeBtn) {
        inboxTabBtn.getStyleClass().remove("segmented-btn-active");
        sentTabBtn.getStyleClass().remove("segmented-btn-active");
        composeTabBtn.getStyleClass().remove("segmented-btn-active");
        activeBtn.getStyleClass().add("segmented-btn-active");
    }

    private BorderPane createInboxView(User user) {
        BorderPane splitPane = new BorderPane();
        splitPane.getStyleClass().add("glass-card");
        splitPane.setPadding(new Insets(15));
        VBox.setVgrow(splitPane, Priority.ALWAYS);

        List<Message> inboxList = messageDAO.getInboxMessages(user.getId());

        TableView<Message> table = new TableView<>();
        table.setPrefWidth(420);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Message, String> senderCol = new TableColumn<>("From");
        senderCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getSenderName()));

        TableColumn<Message, String> subCol = new TableColumn<>("Subject");
        subCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getSubject()));

        TableColumn<Message, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCreatedAt()));

        TableColumn<Message, String> readCol = new TableColumn<>("Status");
        readCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().isRead() ? "Read" : "● New"));

        table.getColumns().addAll(senderCol, subCol, dateCol, readCol);
        table.getItems().addAll(inboxList);

        // Right Detail Reader View — theme-matching colors
        VBox readerBox = new VBox(15);
        readerBox.setPadding(new Insets(15));
        readerBox.setStyle("-fx-background-color: -fx-card-bg; -fx-background-radius: 12px; -fx-border-color: -fx-border-color-glass; -fx-border-radius: 12px;");

        Label rSubject = new Label("Select a message from inbox to view details");
        rSubject.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: -fx-base-primary;");

        Label rFrom = new Label();
        rFrom.setStyle("-fx-text-fill: -fx-text-muted; -fx-font-size: 12px;");

        TextArea rBody = new TextArea();
        rBody.setEditable(false);
        rBody.setWrapText(true);
        rBody.setPrefHeight(300);
        rBody.setStyle("-fx-control-inner-background: -fx-input-bg; -fx-text-fill: #FAFAFA; -fx-font-size: 14px;");

        Button openResultEditor = new Button("Open Result Editor");
        openResultEditor.getStyleClass().add("btn-primary");
        openResultEditor.setVisible(false);
        openResultEditor.setManaged(false);
        readerBox.getChildren().addAll(rSubject, rFrom, rBody, openResultEditor);

        table.getSelectionModel().selectedItemProperty().addListener((obs, oldM, selected) -> {
            if (selected != null) {
                rSubject.setText(selected.getSubject());
                rFrom.setText("From: " + selected.getSenderName() + " (" + selected.getSenderId() + ") • Date: " + selected.getCreatedAt());
                String body = selected.getBody() == null ? "" : selected.getBody();
                Matcher reviewMatcher = Pattern.compile("\\[\\[RESULT_REVIEW:(\\d+)\\]\\]").matcher(body);
                Integer reviewEnrollmentId = null;
                if (reviewMatcher.find() && selected.getSubject() != null
                        && selected.getSubject().startsWith("Result review requested: ")) {
                    reviewEnrollmentId = Integer.parseInt(reviewMatcher.group(1));
                }
                rBody.setText(body.replaceAll("\\n?\\n?\\[\\[RESULT_REVIEW:\\d+\\]\\]", "").trim());
                Integer targetEnrollmentId = reviewEnrollmentId;
                boolean canOpenResult = targetEnrollmentId != null
                        && "teacher".equalsIgnoreCase(user.getRole());
                openResultEditor.setVisible(canOpenResult);
                openResultEditor.setManaged(canOpenResult);
                openResultEditor.setOnAction(e -> {
                    if (targetEnrollmentId != null) {
                        mainLayout.navigateTo(() -> new TeacherDashboardView("enter_marks", targetEnrollmentId));
                    }
                });

                // Mark as read when viewing
                if (!selected.isRead()) {
                    messageDAO.markAsRead(selected.getId());
                    selected.setIsRead(1);
                    table.refresh();
                    mainLayout.refreshMessageCount();
                }
            } else {
                openResultEditor.setVisible(false);
                openResultEditor.setManaged(false);
            }
        });

        splitPane.setLeft(table);
        splitPane.setCenter(readerBox);
        BorderPane.setMargin(readerBox, new Insets(0, 0, 0, 15));
        return splitPane;
    }

    private BorderPane createSentView(User user) {
        BorderPane splitPane = new BorderPane();
        splitPane.getStyleClass().add("glass-card");
        splitPane.setPadding(new Insets(15));
        VBox.setVgrow(splitPane, Priority.ALWAYS);

        List<Message> sentList = messageDAO.getSentMessages(user.getId());

        TableView<Message> table = new TableView<>();
        table.setPrefWidth(420);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Message, String> recCol = new TableColumn<>("To");
        recCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getReceiverName()));

        TableColumn<Message, String> subCol = new TableColumn<>("Subject");
        subCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getSubject()));

        TableColumn<Message, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCreatedAt()));

        table.getColumns().addAll(recCol, subCol, dateCol);
        table.getItems().addAll(sentList);

        // Right Detail Reader View — theme-matching colors
        VBox readerBox = new VBox(15);
        readerBox.setPadding(new Insets(15));
        readerBox.setStyle("-fx-background-color: -fx-card-bg; -fx-background-radius: 12px; -fx-border-color: -fx-border-color-glass; -fx-border-radius: 12px;");

        Label rSubject = new Label("Select a sent message to view details");
        rSubject.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: -fx-base-primary;");

        Label rTo = new Label();
        rTo.setStyle("-fx-text-fill: -fx-text-muted; -fx-font-size: 12px;");

        TextArea rBody = new TextArea();
        rBody.setEditable(false);
        rBody.setWrapText(true);
        rBody.setPrefHeight(300);
        rBody.setStyle("-fx-control-inner-background: -fx-input-bg; -fx-text-fill: #FAFAFA; -fx-font-size: 14px;");

        readerBox.getChildren().addAll(rSubject, rTo, rBody);

        table.getSelectionModel().selectedItemProperty().addListener((obs, oldM, selected) -> {
            if (selected != null) {
                rSubject.setText(selected.getSubject());
                rTo.setText("To: " + selected.getReceiverName() + " (" + selected.getReceiverId() + ") • Date: " + selected.getCreatedAt());
                rBody.setText(selected.getBody());
            }
        });

        splitPane.setLeft(table);
        splitPane.setCenter(readerBox);
        BorderPane.setMargin(readerBox, new Insets(0, 0, 0, 15));
        return splitPane;
    }

    private VBox createComposeView(User sender) {
        VBox box = new VBox(15);
        box.setPadding(new Insets(20));
        box.getStyleClass().add("glass-card");
        VBox.setVgrow(box, Priority.ALWAYS);

        // Re-verify current email verification status from DB
        User latestUser = userDAO.getUserById(sender.getId());
        if (latestUser != null) {
            sender.setEmailVerified(latestUser.getEmailVerified());
        }

        // Email Verification Guard Check
        if (!sender.isEmailVerified()) {
            VBox warningBox = new VBox(15);
            warningBox.setAlignment(Pos.CENTER);
            warningBox.setPadding(new Insets(35));
            warningBox.setStyle("-fx-background-color: rgba(249, 115, 22, 0.10); -fx-background-radius: 12px; -fx-border-color: #F97316; -fx-border-radius: 12px;");

            Label warnTitle = new Label("🔒 Email Verification Required");
            warnTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #F97316;");

            Label warnDesc = new Label("You cannot send messages to any user until you verify your account with your email address. Please go to Profile settings to link and verify your email.");
            warnDesc.getStyleClass().add("text-muted");
            warnDesc.setStyle("-fx-font-size: 13px; -fx-text-alignment: center;");
            warnDesc.setWrapText(true);

            Button profileBtn = new Button("👤 Go to Profile Settings");
            profileBtn.getStyleClass().add("btn-primary");
            profileBtn.setOnAction(e -> {
                mainLayout.navigateTo(() -> new ProfileView(mainLayout));
            });

            warningBox.getChildren().addAll(warnTitle, warnDesc, profileBtn);
            box.getChildren().add(warningBox);
            return box;
        }

        Label label = new Label("✏ Send Direct Message");
        label.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: -fx-base-primary;");

        Label desc = new Label("Compose and send an internal system message directly to any student, teacher, or administrator.");
        desc.getStyleClass().add("text-muted");

        ComboBox<User> userCombo = new ComboBox<>();
        userCombo.setPromptText("Select recipient...");
        userCombo.setMaxWidth(Double.MAX_VALUE);

        // Populate users excluding sender
        for (User u : userDAO.getAllUsers()) {
            if (!u.getId().equalsIgnoreCase(sender.getId())) {
                userCombo.getItems().add(u);
            }
        }

        userCombo.setCellFactory(p -> new ListCell<>() {
            @Override
            protected void updateItem(User item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText(null);
                else setText(item.getName() + " (" + item.getRole().toUpperCase() + " - ID: " + item.getId() + ")");
            }
        });
        userCombo.setButtonCell(userCombo.getCellFactory().call(null));

        TextField subjectField = new TextField();
        subjectField.setPromptText("Enter Message Subject...");
        subjectField.setStyle("-fx-control-inner-background: -fx-input-bg; -fx-text-fill: #FAFAFA; -fx-prompt-text-fill: #A1A1AA;");

        TextArea bodyArea = new TextArea();
        bodyArea.setPromptText("Write your message content here...");
        bodyArea.setPrefRowCount(8);
        bodyArea.setWrapText(true);
        bodyArea.setStyle("-fx-control-inner-background: -fx-input-bg; -fx-text-fill: #FAFAFA; -fx-prompt-text-fill: #A1A1AA; -fx-font-size: 14px;");

        Button sendBtn = new Button("📤 Send Message");
        sendBtn.getStyleClass().add("btn-primary");

        Label statusMsg = new Label();

        sendBtn.setOnAction(e -> {
            User recipient = userCombo.getValue();
            String sub = subjectField.getText().trim();
            String body = bodyArea.getText().trim();

            if (recipient == null || sub.isEmpty() || body.isEmpty()) {
                statusMsg.setText("Please select recipient, subject, and message body.");
                statusMsg.setStyle("-fx-text-fill: #F97316; -fx-font-weight: bold;");
                return;
            }

            Message m = new Message(sender.getId(), recipient.getId(), sub, body);
            boolean ok = messageDAO.sendMessage(m);
            if (ok) {
                statusMsg.setText("✅ Message sent successfully to " + recipient.getName() + "!");
                statusMsg.setStyle("-fx-text-fill: #FDBA74; -fx-font-weight: bold;");
                subjectField.clear();
                bodyArea.clear();
                userCombo.setValue(null);
            } else {
                statusMsg.setText("Failed to send message.");
                statusMsg.setStyle("-fx-text-fill: #F97316;");
            }
        });

        box.getChildren().addAll(label, statusMsg, desc, new Label("Recipient:"), userCombo, new Label("Subject:"), subjectField, new Label("Message:"), bodyArea, sendBtn);
        return box;
    }
}
