package com.eduportal.ui;

import com.eduportal.dao.CourseDAO;
import com.eduportal.dao.DeadlineDAO;
import com.eduportal.dao.EnrollmentDAO;
import com.eduportal.dao.NewsTickerDAO;
import com.eduportal.dao.UserDAO;
import com.eduportal.models.Course;
import com.eduportal.models.CourseApplication;
import com.eduportal.models.Deadline;
import com.eduportal.models.Enrollment;
import com.eduportal.models.NewsTicker;
import com.eduportal.models.User;
import com.eduportal.utils.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.collections.transformation.FilteredList;
import javafx.scene.layout.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class AdminDashboardView extends VBox {

    private final MainLayout mainLayout;
    private final String activeTab;

    private final UserDAO userDAO = new UserDAO();
    private final CourseDAO courseDAO = new CourseDAO();
    private final EnrollmentDAO enrollmentDAO = new EnrollmentDAO();
    private final DeadlineDAO deadlineDAO = new DeadlineDAO();
    private final NewsTickerDAO tickerDAO = new NewsTickerDAO();

    public AdminDashboardView(MainLayout mainLayout, String activeTab) {
        this.mainLayout = mainLayout;
        this.activeTab = activeTab;
        initUI();
    }

    private void initUI() {
        this.setSpacing(20);
        this.setPadding(new Insets(10));

        Label title = new Label("Admin Control Center");
        title.getStyleClass().add("page-title");

        Label subtitle = new Label("System management: user directory, course catalog, deadlines, and news ticker announcements.");
        subtitle.getStyleClass().add("page-subtitle");

        VBox header = new VBox(5, title, subtitle);
        this.getChildren().add(header);

        if ("manage_users".equalsIgnoreCase(activeTab)) {
            this.getChildren().add(createManageUsersSection());
        } else if ("courses".equalsIgnoreCase(activeTab)) {
            this.getChildren().add(createCoursesManagementSection());
        } else if ("results".equalsIgnoreCase(activeTab)) {
            this.getChildren().add(createResultsApprovalSection());
        } else if ("attendance".equalsIgnoreCase(activeTab)) {
            this.getChildren().add(new AttendanceView());
        } else if ("deadlines".equalsIgnoreCase(activeTab)) {
            this.getChildren().add(createDeadlinesManagementSection());
        } else if ("tickers".equalsIgnoreCase(activeTab)) {
            this.getChildren().add(createNewsTickerManagementSection());
        } else {
            // Default Dashboard: User Directory (Read-Only User List)
            this.getChildren().add(createDashboardUserDirectorySection());
        }
    }

    private VBox createDashboardUserDirectorySection() {
        VBox box = new VBox(15);
        box.getStyleClass().add("glass-card");

        Label label = new Label("👥 System User Directory Overview");
        label.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: -fx-base-primary;");

        // User Search Bar
        TextField searchField = new TextField();
        searchField.setPromptText("🔍 Search user by Name or User ID...");
        searchField.setPrefWidth(350);

        TableView<User> table = new TableView<>();
        table.setPrefHeight(320);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<User, String> idCol = new TableColumn<>("User ID");
        idCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getId()));

        TableColumn<User, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getName()));

        TableColumn<User, String> phoneCol = new TableColumn<>("Phone Number");
        phoneCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getPhone() != null ? d.getValue().getPhone() : "—"));

        TableColumn<User, String> roleCol = new TableColumn<>("Role");
        roleCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getRole().toUpperCase()));

        TableColumn<User, String> deptCol = new TableColumn<>("Department");
        deptCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDepartment() != null ? d.getValue().getDepartment() : "—"));

        TableColumn<User, String> semCol = new TableColumn<>("Current Semester");
        semCol.setCellValueFactory(d -> new SimpleStringProperty("student".equals(d.getValue().getRole()) ? "Semester " + d.getValue().getCurrentSemester() : "N/A"));

        TableColumn<User, String> emailCol = new TableColumn<>("Email Address");
        emailCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getEmail() != null ? d.getValue().getEmail() : "—"));

        TableColumn<User, String> verifiedCol = new TableColumn<>("Verification Status");
        verifiedCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().isEmailVerified() ? "✓ Verified" : "✗ Unverified"));

        table.getColumns().addAll(idCol, nameCol, phoneCol, roleCol, deptCol, semCol, emailCol, verifiedCol);

        Runnable loadData = () -> {
            String q = searchField.getText().trim();
            if (q.isEmpty()) {
                table.getItems().setAll(userDAO.getAllUsers());
            } else {
                table.getItems().setAll(userDAO.searchUsers(q));
            }
        };

        searchField.textProperty().addListener((obs, o, n) -> loadData.run());
        loadData.run();

        box.getChildren().addAll(label, searchField, table);
        return box;
    }

    private VBox createManageUsersSection() {
        VBox box = new VBox(15);
        box.getStyleClass().add("glass-card");

        Label label = new Label("👥 User Account Management & Semester Controls");
        label.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: -fx-base-primary;");

        TableView<User> table = new TableView<>();
        table.setPrefHeight(250);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<User, String> idCol = new TableColumn<>("User ID");
        idCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getId()));

        TableColumn<User, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getName()));

        TableColumn<User, String> roleCol = new TableColumn<>("Role");
        roleCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getRole().toUpperCase()));

        TableColumn<User, String> deptCol = new TableColumn<>("Department");
        deptCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDepartment() != null ? d.getValue().getDepartment() : "—"));

        TableColumn<User, String> semCol = new TableColumn<>("Semester Level");
        semCol.setCellValueFactory(d -> new SimpleStringProperty("student".equals(d.getValue().getRole()) ? "Semester " + d.getValue().getCurrentSemester() : "N/A"));

        TableColumn<User, String> emailCol = new TableColumn<>("Email");
        emailCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getEmail() != null ? d.getValue().getEmail() : "—"));

        table.getColumns().addAll(idCol, nameCol, roleCol, deptCol, semCol, emailCol);
        table.getItems().addAll(userDAO.getAllUsers());

        // Management Actions Bar
        HBox actions = new HBox(10);
        ComboBox<String> changeSemCombo = new ComboBox<>();
        for (int i = 1; i <= 8; i++) changeSemCombo.getItems().add("Semester " + i);
        changeSemCombo.setPromptText("Select Semester...");

        Button setSemBtn = new Button("Set Student Semester");
        setSemBtn.getStyleClass().add("btn-primary");

        Button resetPwBtn = new Button("Reset Password");
        resetPwBtn.getStyleClass().add("btn-ghost");

        Button deleteBtn = new Button("Delete User");
        deleteBtn.getStyleClass().add("btn-danger");

        Label statusMsg = new Label();

        setSemBtn.setOnAction(e -> {
            User selected = table.getSelectionModel().getSelectedItem();
            if (selected == null || !"student".equals(selected.getRole())) {
                statusMsg.setText("Please select a student to change semester.");
                statusMsg.setStyle("-fx-text-fill: #F97316;");
                return;
            }
            int newSem = changeSemCombo.getSelectionModel().getSelectedIndex() + 1;
            if (newSem > 0) {
                userDAO.updateStudentSemester(selected.getId(), newSem);
                table.getItems().setAll(userDAO.getAllUsers());
                statusMsg.setText("Updated " + selected.getName() + " to Semester " + newSem);
                statusMsg.setStyle("-fx-text-fill: #FDBA74;");
            }
        });

        resetPwBtn.setOnAction(e -> {
            User selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) return;
            userDAO.updatePassword(selected.getId(), "password123");
            statusMsg.setText("Password for " + selected.getId() + " reset to 'password123'.");
            statusMsg.setStyle("-fx-text-fill: #FDBA74;");
        });

        deleteBtn.setOnAction(e -> {
            User selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) return;
            userDAO.deleteUser(selected.getId());
            table.getItems().setAll(userDAO.getAllUsers());
            statusMsg.setText("User " + selected.getId() + " deleted.");
            statusMsg.setStyle("-fx-text-fill: #F97316;");
        });

        actions.getChildren().addAll(changeSemCombo, setSemBtn, resetPwBtn, deleteBtn);

        // Department Change Section
        HBox deptActions = new HBox(10);
        ComboBox<String> deptCombo = new ComboBox<>();
        deptCombo.getItems().addAll("CSE", "EEE", "BBA", "English", "Law", "Pharmacy", "CIVIL", "Architecture");
        deptCombo.setPromptText("Select Department...");

        Button changeDeptBtn = new Button("Change Department");
        changeDeptBtn.getStyleClass().add("btn-primary");

        changeDeptBtn.setOnAction(e -> {
            User selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                statusMsg.setText("Please select a user to change department.");
                statusMsg.setStyle("-fx-text-fill: #F97316;");
                return;
            }
            String dept = deptCombo.getValue();
            if (dept == null || dept.isEmpty()) {
                statusMsg.setText("Please select a department.");
                statusMsg.setStyle("-fx-text-fill: #F97316;");
                return;
            }
            userDAO.updateDepartment(selected.getId(), dept);
            table.getItems().setAll(userDAO.getAllUsers());
            statusMsg.setText("Department for " + selected.getName() + " changed to " + dept);
            statusMsg.setStyle("-fx-text-fill: #FDBA74;");
        });

        deptActions.getChildren().addAll(deptCombo, changeDeptBtn);

        box.getChildren().addAll(label, statusMsg, table, actions, deptActions);
        return box;
    }

    private VBox createCoursesManagementSection() {
        VBox box = new VBox(15);
        box.getStyleClass().add("glass-card");

        Label label = new Label("📖 Course Catalog Management");
        label.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: -fx-base-primary;");

        TableView<Course> table = new TableView<>();
        table.setPrefHeight(220);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Course, String> cIdCol = new TableColumn<>("Course ID");
        cIdCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCourseId()));

        TableColumn<Course, String> cNameCol = new TableColumn<>("Course Name");
        cNameCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCourseName()));

        TableColumn<Course, String> targetSemCol = new TableColumn<>("Target Semester");
        targetSemCol.setCellValueFactory(d -> new SimpleStringProperty("Semester " + d.getValue().getTargetSemester()));

        TableColumn<Course, String> credCol = new TableColumn<>("Credits");
        credCol.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getCredits())));

        TableColumn<Course, String> teacherCol = new TableColumn<>("Assigned Teacher");
        teacherCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTeacherName() != null ? d.getValue().getTeacherName() : "— Unassigned —"));

        table.getColumns().addAll(cIdCol, cNameCol, targetSemCol, credCol, teacherCol);
        table.getItems().addAll(courseDAO.getAllCourses());

        // Edit Course Form
        VBox formBox = new VBox(10);
        formBox.setPadding(new Insets(10, 0, 0, 0));

        HBox editForm = new HBox(10);
        TextField cIdField = new TextField(); cIdField.setPromptText("Course ID");
        TextField cNameField = new TextField(); cNameField.setPromptText("Course Name");
        TextField credField = new TextField(); credField.setPromptText("Credits"); credField.setPrefWidth(65);

        ComboBox<String> targetSemCombo = new ComboBox<>();
        for (int i = 1; i <= 8; i++) targetSemCombo.getItems().add("Semester " + i);
        targetSemCombo.setValue("Semester 1");

        // Teacher ComboBox with Name AND Department formatting
        ComboBox<User> teacherCombo = new ComboBox<>();
        teacherCombo.setPromptText("Select Teacher...");
        teacherCombo.getItems().addAll(userDAO.getUsersByRole("teacher"));
        teacherCombo.setCellFactory(p -> new ListCell<>() {
            @Override
            protected void updateItem(User item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText(null);
                else setText(item.getName() + " (" + (item.getDepartment() != null ? item.getDepartment() : "No Dept") + ")");
            }
        });
        teacherCombo.setButtonCell(teacherCombo.getCellFactory().call(null));

        Button addBtn = new Button("Upload New Course");
        addBtn.getStyleClass().add("btn-primary");

        Button updateBtn = new Button("Update Selected Course");
        updateBtn.getStyleClass().add("btn-success");

        Button deleteBtn = new Button("Delete Course");
        deleteBtn.getStyleClass().add("btn-danger");

        Button unassignTeacherBtn = new Button("Unassign Teacher");
        unassignTeacherBtn.getStyleClass().add("btn-ghost");

        Label statusMsg = new Label();

        table.getSelectionModel().selectedItemProperty().addListener((obs, o, selected) -> {
            if (selected != null) {
                cIdField.setText(selected.getCourseId());
                cNameField.setText(selected.getCourseName());
                credField.setText(String.valueOf(selected.getCredits()));
                targetSemCombo.setValue("Semester " + selected.getTargetSemester());
            }
        });

        addBtn.setOnAction(e -> {
            String cid = cIdField.getText().trim();
            String cname = cNameField.getText().trim();
            int cred = 3;
            try { cred = Integer.parseInt(credField.getText().trim()); } catch (Exception ignored) {}
            int targetSem = targetSemCombo.getSelectionModel().getSelectedIndex() + 1;
            User selectedTeacher = teacherCombo.getValue();

            if (!cid.isEmpty() && !cname.isEmpty()) {
                Course c = new Course(cid, cname, cred, selectedTeacher != null ? selectedTeacher.getId() : null, "Spring 2026", targetSem);
                courseDAO.addCourse(c);
                table.getItems().setAll(courseDAO.getAllCourses());
                cIdField.clear(); cNameField.clear(); credField.clear();
                statusMsg.setText("Course uploaded successfully!");
                statusMsg.setStyle("-fx-text-fill: #FDBA74;");
            }
        });

        updateBtn.setOnAction(e -> {
            Course selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                statusMsg.setText("Select a course from table to edit.");
                statusMsg.setStyle("-fx-text-fill: #F97316;");
                return;
            }
            String cname = cNameField.getText().trim();
            int cred = selected.getCredits();
            try { cred = Integer.parseInt(credField.getText().trim()); } catch (Exception ignored) {}
            int targetSem = targetSemCombo.getSelectionModel().getSelectedIndex() + 1;
            User selectedTeacher = teacherCombo.getValue();
            String teacherId = selectedTeacher != null ? selectedTeacher.getId() : selected.getTeacherId();

            boolean ok = courseDAO.updateCourse(selected.getCourseId(), cname, cred, teacherId, targetSem);
            if (ok) {
                table.getItems().setAll(courseDAO.getAllCourses());
                statusMsg.setText("Course updated successfully!");
                statusMsg.setStyle("-fx-text-fill: #FDBA74;");
            }
        });

        deleteBtn.setOnAction(e -> {
            Course selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) return;
            courseDAO.deleteCourse(selected.getCourseId());
            table.getItems().setAll(courseDAO.getAllCourses());
            statusMsg.setText("Course deleted.");
            statusMsg.setStyle("-fx-text-fill: #F97316;");
        });

        unassignTeacherBtn.setOnAction(e -> {
            Course selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                statusMsg.setText("Select a course to unassign its teacher.");
                statusMsg.setStyle("-fx-text-fill: #F97316;");
                return;
            }
            boolean ok = courseDAO.updateCourse(selected.getCourseId(), selected.getCourseName(),
                    selected.getCredits(), null, selected.getTargetSemester());
            if (ok) {
                table.getItems().setAll(courseDAO.getAllCourses());
                teacherCombo.setValue(null);
                statusMsg.setText("Teacher unassigned. The course is now available for applications.");
                statusMsg.setStyle("-fx-text-fill: #FDBA74;");
            } else {
                statusMsg.setText("Could not unassign the teacher.");
                statusMsg.setStyle("-fx-text-fill: #F97316;");
            }
        });

        editForm.getChildren().addAll(cIdField, cNameField, credField, targetSemCombo, teacherCombo);
        HBox btnBox = new HBox(10, addBtn, updateBtn, deleteBtn, unassignTeacherBtn, statusMsg);

        formBox.getChildren().addAll(editForm, btnBox);
        box.getChildren().addAll(label, statusMsg, table, formBox, createCourseApplicationsSection());
        return box;
    }

    private VBox createCourseApplicationsSection() {
        VBox section = new VBox(10);
        section.getChildren().add(new Label("Teacher course applications"));
        TableView<CourseApplication> table = new TableView<>();
        table.setPrefHeight(180);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<CourseApplication, String> courseColumn = new TableColumn<>("Course");
        courseColumn.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getCourseId() + " - " + d.getValue().getCourseName()));
        TableColumn<CourseApplication, String> teacherColumn = new TableColumn<>("Teacher");
        teacherColumn.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getTeacherName() + " (" + d.getValue().getTeacherId() + ")"));
        table.getColumns().addAll(courseColumn, teacherColumn);
        Runnable reload = () -> table.getItems().setAll(courseDAO.getPendingApplications());
        reload.run();

        Button approve = new Button("Approve and Assign");
        approve.getStyleClass().add("btn-primary");
        Button reject = new Button("Reject");
        reject.getStyleClass().add("btn-danger");
        Label status = new Label();
        approve.setOnAction(e -> {
            CourseApplication selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                status.setText("Select an application to approve.");
                return;
            }
            if (courseDAO.approveApplication(selected.getId())) {
                status.setText("Course assigned to " + selected.getTeacherName() + ".");
                table.getItems().setAll(courseDAO.getPendingApplications());
            } else {
                status.setText("This course may already be assigned or the request is no longer pending.");
            }
            table.getItems().setAll(courseDAO.getPendingApplications());
        });
        reject.setOnAction(e -> {
            CourseApplication selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                status.setText("Select an application to reject.");
                return;
            }
            status.setText(courseDAO.rejectApplication(selected.getId())
                    ? "Application rejected." : "Could not reject the application.");
            reload.run();
        });
        section.getChildren().addAll(status, table, new HBox(10, approve, reject));
        return section;
    }

    private VBox createResultsApprovalSection() {
        VBox section = new VBox(12);
        section.getStyleClass().add("glass-card");
        Label heading = new Label("Submitted results (pending, published, or under review)");

        TableView<Enrollment> table = new TableView<>();
        table.setPrefHeight(360);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        TextField studentSearch = new TextField();
        studentSearch.setPromptText("Search by student ID");
        studentSearch.setPrefWidth(260);
        Label selectionHint = new Label("Select one or more student results (Ctrl-click or Shift-click for multiple).");
        selectionHint.getStyleClass().add("text-muted");
        TableColumn<Enrollment, String> studentColumn = new TableColumn<>("Student");
        studentColumn.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getStudentName() + " (" + d.getValue().getStudentId() + ")"));
        TableColumn<Enrollment, String> courseColumn = new TableColumn<>("Course");
        courseColumn.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getCourseId() + " - " + d.getValue().getCourseName()));
        TableColumn<Enrollment, String> teacherColumn = new TableColumn<>("Teacher");
        teacherColumn.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getTeacherName() != null ? d.getValue().getTeacherName() : "Unassigned"));
        TableColumn<Enrollment, String> attendanceColumn = markColumn("Attendance (5%)", Enrollment::getAttendance);
        TableColumn<Enrollment, String> quiz1Column = markColumn("Quiz 1 (10%)", Enrollment::getQ1);
        TableColumn<Enrollment, String> quiz2Column = markColumn("Quiz 2 (10%)", Enrollment::getQ2);
        TableColumn<Enrollment, String> assignmentColumn = markColumn("Assignment (5%)", Enrollment::getAssignment);
        TableColumn<Enrollment, String> midtermColumn = markColumn("Midterm (30%)", Enrollment::getMid);
        TableColumn<Enrollment, String> finalColumn = markColumn("Final Exam (40%)", Enrollment::getFinalExam);
        TableColumn<Enrollment, String> marksColumn = new TableColumn<>("Total");
        marksColumn.setCellValueFactory(d -> new SimpleStringProperty(String.format("%.1f", d.getValue().getTotalMarks())));
        TableColumn<Enrollment, String> statusColumn = new TableColumn<>("Status");
        statusColumn.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getPublicationStatus()));
        table.getColumns().addAll(studentColumn, courseColumn, teacherColumn, attendanceColumn, quiz1Column,
                quiz2Column, assignmentColumn, midtermColumn, finalColumn, marksColumn, statusColumn);

        ObservableList<Enrollment> allResults = FXCollections.observableArrayList();
        FilteredList<Enrollment> filteredResults = new FilteredList<>(allResults);
        studentSearch.textProperty().addListener((obs, previous, query) -> {
            String search = query == null ? "" : query.trim().toLowerCase();
            filteredResults.setPredicate(enrollment -> search.isEmpty()
                    || enrollment.getStudentId().toLowerCase().contains(search));
        });
        table.setItems(filteredResults);
        Runnable reload = () -> allResults.setAll(enrollmentDAO.getResultsForAdmin());
        reload.run();
        Button approve = new Button("Approve Selected Results");
        approve.getStyleClass().add("btn-primary");
        Button requestReview = new Button("Request Review for Selected");
        requestReview.getStyleClass().add("btn-ghost");
        Label status = new Label();
        approve.setOnAction(e -> {
            List<Enrollment> selectedResults = new ArrayList<>(table.getSelectionModel().getSelectedItems());
            if (selectedResults.isEmpty()) {
                status.setText("Select one or more results to approve.");
                return;
            }
            User admin = SessionManager.getCurrentUser();
            if (admin == null) {
                status.setText("Could not identify the administrator approving these results.");
                return;
            }
            int approvedCount = 0;
            for (Enrollment enrollment : selectedResults) {
                if (enrollmentDAO.setResultStatus(enrollment.getId(), "approved", admin.getId())) approvedCount++;
            }
            status.setText(approvedCount + " of " + selectedResults.size() + " selected result(s) approved.");
            reload.run();
        });
        requestReview.setOnAction(e -> {
            List<Enrollment> selectedResults = new ArrayList<>(table.getSelectionModel().getSelectedItems());
            if (selectedResults.isEmpty()) {
                status.setText("Select one or more student results to request review.");
                return;
            }
            User admin = SessionManager.getCurrentUser();
            int requestedCount = 0;
            if (admin != null) {
                for (Enrollment enrollment : selectedResults) {
                    if (enrollmentDAO.requestTeacherReview(enrollment.getId(), admin.getId())) requestedCount++;
                }
            }
            status.setText(requestedCount + " of " + selectedResults.size()
                    + " selected teacher review request(s) sent.");
            reload.run();
        });
        section.getChildren().addAll(heading, status, studentSearch, selectionHint, table,
                new HBox(10, approve, requestReview));
        return section;
    }

    private TableColumn<Enrollment, String> markColumn(String title,
                                                         java.util.function.Function<Enrollment, Double> value) {
        TableColumn<Enrollment, String> column = new TableColumn<>(title);
        column.setCellValueFactory(d -> new SimpleStringProperty(String.format("%.1f",
                value.apply(d.getValue()) == null ? 0.0 : value.apply(d.getValue()))));
        return column;
    }

    private VBox createDeadlinesManagementSection() {
        VBox container = new VBox(20);

        // Clean expired permissions on load
        deadlineDAO.cleanExpiredPermissions();

        // =========================================================================
        // BLOCK 1: System Deadline Management (Stacked Block 1)
        // =========================================================================
        VBox sysCard = new VBox(15);
        sysCard.getStyleClass().add("glass-card");

        Label label = new Label("📅 System Deadline Management");
        label.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: -fx-base-primary;");

        Label desc = new Label("These are fixed system deadlines. You can only modify the start and end dates. Deadlines cannot be added or removed.");
        desc.getStyleClass().add("text-muted");
        desc.setWrapText(true);

        // Fixed Deadlines Table
        TableView<Deadline> table = new TableView<>();
        table.setPrefHeight(180);
        table.setMinHeight(180);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Deadline, String> titleCol = new TableColumn<>("Deadline Type");
        titleCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTitle()));

        TableColumn<Deadline, String> startCol = new TableColumn<>("Start Date");
        startCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStartDate() != null ? d.getValue().getStartDate() : "—"));

        TableColumn<Deadline, String> endCol = new TableColumn<>("End Date");
        endCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getEndDate() != null ? d.getValue().getEndDate() : "—"));

        TableColumn<Deadline, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(d -> {
            String endDate = d.getValue().getEndDate();
            boolean active = true;
            if (endDate != null && !endDate.isEmpty()) {
                try {
                    active = !LocalDate.now().isAfter(LocalDate.parse(endDate));
                } catch (Exception ignored) {}
            }
            return new SimpleStringProperty(active ? "✓ OPEN" : "🔒 CLOSED");
        });

        table.getColumns().addAll(titleCol, startCol, endCol, statusCol);
        table.getItems().addAll(deadlineDAO.getAllSystemDeadlines());

        // Date Edit Form
        HBox dateEditBox = new HBox(12);
        DatePicker startDatePicker = new DatePicker();
        startDatePicker.setPromptText("Start Date");

        DatePicker endDatePicker = new DatePicker();
        endDatePicker.setPromptText("End Date");

        Button updateDatesBtn = new Button("Update Dates");
        updateDatesBtn.getStyleClass().add("btn-primary");

        Label dateStatusMsg = new Label();

        table.getSelectionModel().selectedItemProperty().addListener((obs, o, selected) -> {
            if (selected != null) {
                try { startDatePicker.setValue(LocalDate.parse(selected.getStartDate())); } catch (Exception ignored) { startDatePicker.setValue(null); }
                try { endDatePicker.setValue(LocalDate.parse(selected.getEndDate())); } catch (Exception ignored) { endDatePicker.setValue(null); }
            }
        });

        updateDatesBtn.setOnAction(e -> {
            Deadline selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                dateStatusMsg.setText("Please select a deadline from the table above.");
                dateStatusMsg.setStyle("-fx-text-fill: #F97316;");
                return;
            }
            LocalDate s = startDatePicker.getValue();
            LocalDate endD = endDatePicker.getValue();
            String startStr = s != null ? s.toString() : selected.getStartDate();
            String endStr = endD != null ? endD.toString() : selected.getEndDate();

            boolean ok = deadlineDAO.updateDeadlineDates(selected.getDeadlineKey(), startStr, endStr);
            if (ok) {
                table.getItems().setAll(deadlineDAO.getAllSystemDeadlines());
                dateStatusMsg.setText("Dates updated for: " + selected.getTitle());
                dateStatusMsg.setStyle("-fx-text-fill: #FDBA74;");
            }
        });

        dateEditBox.getChildren().addAll(startDatePicker, endDatePicker, updateDatesBtn);
        sysCard.getChildren().addAll(label, desc, dateStatusMsg, table, dateEditBox);

        // =========================================================================
        // BLOCK 2: Individual Deadline Permissions (Stacked Block 2)
        // =========================================================================
        VBox permCard = new VBox(15);
        permCard.getStyleClass().add("glass-card");

        Label permLabel = new Label("🔑 Individual Deadline Permissions");
        permLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: -fx-base-primary;");

        Label permDesc = new Label("Grant a specific student or teacher permission to bypass a closed deadline. " +
                "Students can ONLY receive permission for Student Course Registration. " +
                "Teachers can ONLY receive permission for Teacher Registration or Result Submission.");
        permDesc.getStyleClass().add("text-muted");
        permDesc.setWrapText(true);

        HBox grantForm = new HBox(12);

        TextField userIdField = new TextField();
        userIdField.setPromptText("Enter User ID");
        userIdField.setPrefWidth(140);

        ComboBox<String> deadlineKeyCombo = new ComboBox<>();
        deadlineKeyCombo.setPromptText("Select Deadline Type...");
        deadlineKeyCombo.getItems().addAll("student_enroll", "teacher_enroll", "teacher_marks");

        // Custom cell factory for deadline combo
        deadlineKeyCombo.setCellFactory(p -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText(null);
                else setText(getDeadlineFriendlyName(item));
            }
        });
        deadlineKeyCombo.setButtonCell(deadlineKeyCombo.getCellFactory().call(null));

        // Auto-restrict deadline options based on entered User ID
        userIdField.textProperty().addListener((obs, oldVal, newVal) -> {
            String uid = newVal != null ? newVal.trim() : "";
            if (uid.isEmpty()) {
                deadlineKeyCombo.getItems().setAll("student_enroll", "teacher_enroll", "teacher_marks");
                return;
            }
            User u = userDAO.getUserById(uid);
            if (u != null) {
                if ("student".equalsIgnoreCase(u.getRole())) {
                    deadlineKeyCombo.getItems().setAll("student_enroll");
                    deadlineKeyCombo.setValue("student_enroll");
                } else if ("teacher".equalsIgnoreCase(u.getRole())) {
                    deadlineKeyCombo.getItems().setAll("teacher_enroll", "teacher_marks");
                    deadlineKeyCombo.setValue("teacher_enroll");
                } else {
                    deadlineKeyCombo.getItems().setAll("student_enroll", "teacher_enroll", "teacher_marks");
                }
            } else {
                deadlineKeyCombo.getItems().setAll("student_enroll", "teacher_enroll", "teacher_marks");
            }
        });

        DatePicker permExpiryPicker = new DatePicker();
        permExpiryPicker.setPromptText("Permission Expiry Date");

        Button grantBtn = new Button("Grant Permission");
        grantBtn.getStyleClass().add("btn-primary");

        Label permStatusMsg = new Label();
        permStatusMsg.setWrapText(true);

        // Active Permissions Table
        TableView<String[]> permTable = new TableView<>();
        permTable.setPrefHeight(200);
        permTable.setMinHeight(200);
        permTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<String[], String> pUserIdCol = new TableColumn<>("User ID");
        pUserIdCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue()[1]));

        TableColumn<String[], String> pUserNameCol = new TableColumn<>("User Name");
        pUserNameCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue()[2]));

        TableColumn<String[], String> pDeadlineCol = new TableColumn<>("Permission Type");
        pDeadlineCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue()[4]));

        TableColumn<String[], String> pExpiryCol = new TableColumn<>("Expires On");
        pExpiryCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue()[5]));

        TableColumn<String[], String> pGrantedCol = new TableColumn<>("Granted At");
        pGrantedCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue()[6]));

        permTable.getColumns().addAll(pUserIdCol, pUserNameCol, pDeadlineCol, pExpiryCol, pGrantedCol);
        permTable.getItems().addAll(deadlineDAO.getAllPermissions());

        grantBtn.setOnAction(e -> {
            String uid = userIdField.getText().trim();
            String dKey = deadlineKeyCombo.getValue();
            LocalDate expiry = permExpiryPicker.getValue();

            if (uid.isEmpty() || dKey == null || expiry == null) {
                permStatusMsg.setText("Please fill in User ID, Deadline Type, and Expiry Date.");
                permStatusMsg.setStyle("-fx-text-fill: #F97316;");
                return;
            }

            // Check user exists
            User user = userDAO.getUserById(uid);
            if (user == null) {
                permStatusMsg.setText("User ID '" + uid + "' not found.");
                permStatusMsg.setStyle("-fx-text-fill: #F97316;");
                return;
            }

            // Role-based permission restriction validation
            if ("student".equalsIgnoreCase(user.getRole())) {
                if (!"student_enroll".equals(dKey)) {
                    permStatusMsg.setText("Students can ONLY receive permission for Student Course Registration.");
                    permStatusMsg.setStyle("-fx-text-fill: #F97316; -fx-font-weight: bold;");
                    return;
                }
            } else if ("teacher".equalsIgnoreCase(user.getRole())) {
                if ("student_enroll".equals(dKey)) {
                    permStatusMsg.setText("Teachers CANNOT receive permission for Student Course Registration.");
                    permStatusMsg.setStyle("-fx-text-fill: #F97316; -fx-font-weight: bold;");
                    return;
                }
            }

            boolean ok = deadlineDAO.grantPermission(uid, dKey, expiry.toString());
            if (ok) {
                permStatusMsg.setText("✅ Permission granted to " + user.getName() + " (" + uid + ") for " + getDeadlineFriendlyName(dKey) + " until " + expiry.toString());
                permStatusMsg.setStyle("-fx-text-fill: #FDBA74; -fx-font-weight: bold;");
                userIdField.clear();
                permTable.getItems().setAll(deadlineDAO.getAllPermissions());
            } else {
                permStatusMsg.setText("Failed to grant permission.");
                permStatusMsg.setStyle("-fx-text-fill: #F97316;");
            }
        });

        grantForm.getChildren().addAll(userIdField, deadlineKeyCombo, permExpiryPicker, grantBtn);

        Label activePermLabel = new Label("📋 Active Granted Permissions");
        activePermLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: -fx-text-main;");

        Button revokeBtn = new Button("Revoke Selected Permission");
        revokeBtn.getStyleClass().add("btn-danger");

        revokeBtn.setOnAction(e -> {
            String[] selected = permTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                permStatusMsg.setText("Select a permission from the table below to revoke.");
                permStatusMsg.setStyle("-fx-text-fill: #F97316;");
                return;
            }
            int permId = Integer.parseInt(selected[0]);
            deadlineDAO.revokePermissionById(permId);
            permTable.getItems().setAll(deadlineDAO.getAllPermissions());
            permStatusMsg.setText("Permission revoked for " + selected[2] + " (" + selected[1] + ").");
            permStatusMsg.setStyle("-fx-text-fill: #F97316;");
        });

        permCard.getChildren().addAll(permLabel, permDesc, permStatusMsg, grantForm,
                activePermLabel, permTable, revokeBtn);

        container.getChildren().addAll(sysCard, permCard);
        return container;
    }

    private String getDeadlineFriendlyName(String key) {
        if (key == null) return "";
        switch (key) {
            case "student_enroll": return "Student Course Registration";
            case "teacher_enroll": return "Teacher Course Registration";
            case "teacher_marks": return "Teacher Result Submission";
            default: return key;
        }
    }

    private VBox createNewsTickerManagementSection() {
        VBox box = new VBox(15);
        box.getStyleClass().add("glass-card");

        Label label = new Label("📢 News Ticker Dedicated Management (Instant Sync)");
        label.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: -fx-base-primary;");

        TableView<NewsTicker> table = new TableView<>();
        table.setPrefHeight(220);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<NewsTicker, String> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getId())));

        TableColumn<NewsTicker, String> contentCol = new TableColumn<>("News Ticker Content");
        contentCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getContent()));

        TableColumn<NewsTicker, String> targetCol = new TableColumn<>("Target Audience");
        targetCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTargetRole().toUpperCase()));

        TableColumn<NewsTicker, String> statusCol = new TableColumn<>("Visibility");
        statusCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().isActive() ? "✓ VISIBLE" : "🙈 HIDDEN"));

        table.getColumns().addAll(idCol, contentCol, targetCol, statusCol);
        table.getItems().addAll(tickerDAO.getAllTickers());

        // Add / Edit Form
        HBox form = new HBox(10);
        TextField contentField = new TextField();
        contentField.setPromptText("Enter News Ticker Announcement...");
        HBox.setHgrow(contentField, Priority.ALWAYS);

        ComboBox<String> audienceCombo = new ComboBox<>();
        audienceCombo.getItems().addAll("All Users", "Students Only", "Teachers Only");
        audienceCombo.setValue("All Users");

        Button addBtn = new Button("Create Ticker");
        addBtn.getStyleClass().add("btn-primary");

        Button editBtn = new Button("Update Selected");
        editBtn.getStyleClass().add("btn-success");

        Button toggleBtn = new Button("Hide / Show");
        toggleBtn.getStyleClass().add("btn-ghost");

        Button deleteBtn = new Button("Delete Ticker");
        deleteBtn.getStyleClass().add("btn-danger");

        Label statusMsg = new Label();

        // Populate form on selection
        table.getSelectionModel().selectedItemProperty().addListener((obs, o, selected) -> {
            if (selected != null) {
                contentField.setText(selected.getContent());
                String role = selected.getTargetRole();
                if ("student".equals(role)) audienceCombo.setValue("Students Only");
                else if ("teacher".equals(role)) audienceCombo.setValue("Teachers Only");
                else audienceCombo.setValue("All Users");
            }
        });

        addBtn.setOnAction(e -> {
            String text = contentField.getText().trim();
            if (!text.isEmpty()) {
                String role = audienceCombo.getValue().contains("Students") ? "student" : (audienceCombo.getValue().contains("Teachers") ? "teacher" : "all");
                tickerDAO.addTicker(text, role);
                table.getItems().setAll(tickerDAO.getAllTickers());
                contentField.clear();
                MainLayout.triggerTickerSync();
                statusMsg.setText("News Ticker created & synced!");
                statusMsg.setStyle("-fx-text-fill: #FDBA74;");
            }
        });

        editBtn.setOnAction(e -> {
            NewsTicker selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                statusMsg.setText("Select a ticker to update.");
                statusMsg.setStyle("-fx-text-fill: #F97316;");
                return;
            }
            String text = contentField.getText().trim();
            if (!text.isEmpty()) {
                String role = audienceCombo.getValue().contains("Students") ? "student" : (audienceCombo.getValue().contains("Teachers") ? "teacher" : "all");
                tickerDAO.updateTicker(selected.getId(), text, role);
                table.getItems().setAll(tickerDAO.getAllTickers());
                MainLayout.triggerTickerSync();
                statusMsg.setText("Ticker updated & synced!");
                statusMsg.setStyle("-fx-text-fill: #FDBA74;");
            }
        });

        toggleBtn.setOnAction(e -> {
            NewsTicker selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) return;
            tickerDAO.toggleTicker(selected.getId());
            table.getItems().setAll(tickerDAO.getAllTickers());
            MainLayout.triggerTickerSync();
        });

        deleteBtn.setOnAction(e -> {
            NewsTicker selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) return;
            tickerDAO.deleteTicker(selected.getId());
            table.getItems().setAll(tickerDAO.getAllTickers());
            MainLayout.triggerTickerSync();
            statusMsg.setText("Ticker deleted & synced.");
            statusMsg.setStyle("-fx-text-fill: #F97316;");
        });

        form.getChildren().addAll(contentField, audienceCombo, addBtn, editBtn, toggleBtn, deleteBtn);
        box.getChildren().addAll(label, statusMsg, table, form);
        return box;
    }
}
