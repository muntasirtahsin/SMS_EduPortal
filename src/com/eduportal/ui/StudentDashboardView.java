package com.eduportal.ui;

import com.eduportal.dao.CourseDAO;
import com.eduportal.dao.DeadlineDAO;
import com.eduportal.dao.EnrollmentDAO;
import com.eduportal.dao.UserDAO;
import com.eduportal.models.Course;
import com.eduportal.models.Enrollment;
import com.eduportal.models.User;
import com.eduportal.services.PDFReportService;
import com.eduportal.utils.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class StudentDashboardView extends VBox {

    private final String activeTab;
    private final CourseDAO courseDAO = new CourseDAO();
    private final EnrollmentDAO enrollmentDAO = new EnrollmentDAO();
    private final UserDAO userDAO = new UserDAO();
    private final DeadlineDAO deadlineDAO = new DeadlineDAO();

    public StudentDashboardView(String activeTab) {
        this.activeTab = activeTab;
        initUI();
    }

    private void initUI() {
        this.setSpacing(20);
        this.setPadding(new Insets(10));

        User currentUser = SessionManager.getCurrentUser();
        if (currentUser == null) return;
        enrollmentDAO.expireIncompleteEnrollments(currentUser.getId(), currentUser.getCurrentSemester());

        // Title Header
        Label title = new Label("Student Academic Portal");
        title.getStyleClass().add("page-title");

        Label subtitle = new Label("Welcome, " + currentUser.getName() + "! Current Status: Semester " + currentUser.getCurrentSemester() + " Student.");
        subtitle.getStyleClass().add("page-subtitle");

        VBox header = new VBox(5, title, subtitle);
        this.getChildren().add(header);

        if ("take_course".equalsIgnoreCase(activeTab)) {
            this.getChildren().add(createTakeCourseSection(currentUser));
        } else if ("grade_report".equalsIgnoreCase(activeTab)) {
            this.getChildren().add(createGradeReportSection(currentUser));
        } else if ("attendance".equalsIgnoreCase(activeTab)) {
            this.getChildren().add(new AttendanceView());
        } else {
            // Default Dashboard: My Courses Overview Focus
            this.getChildren().add(createMyCoursesOverviewSection(currentUser));
        }
    }

    private VBox createMyCoursesOverviewSection(User currentUser) {
        VBox box = new VBox(15);

        // Fetch Enrolled Courses
        List<Enrollment> enrollments = enrollmentDAO.getEnrollmentsByStudent(currentUser.getId(), "All");

        double totalGradePoints = 0.0;
        int totalCredits = 0;
        int totalWaiverCredits = 0;
        List<Enrollment> approvedEnrollments = enrollmentDAO.getApprovedEnrollmentsByStudent(currentUser.getId(), "All");

        for (Enrollment e : approvedEnrollments) {
            totalGradePoints += (e.getGradePoint() * e.getCredits());
            totalCredits += e.getCredits();
        }
        totalWaiverCredits = getTotalSemesterWaiverCredits(approvedEnrollments);

        double cgpa = totalCredits > 0 ? (totalGradePoints / totalCredits) : 0.0;

        // Stat Cards Header
        HBox statsGrid = new HBox(15);
        VBox gpaCard = createStatCard("Cumulative CGPA", String.format("%.2f", cgpa), "#F97316");
        VBox waiverCard = createStatCard("Total Waiver Earned", totalWaiverCredits + " Credits", "#FAFAFA");
        VBox courseCard = createStatCard("Enrolled Courses", enrollments.size() + " Courses", "#FDBA74");
        statsGrid.getChildren().addAll(gpaCard, waiverCard, courseCard);
        box.getChildren().add(statsGrid);

        // Enrolled Courses Main Card
        VBox coursesCard = new VBox(15);
        coursesCard.getStyleClass().add("glass-card");

        Label cardTitle = new Label("📚 My Enrolled Courses");
        cardTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: -fx-base-primary;");

        TableView<Enrollment> table = new TableView<>();
        table.setPrefHeight(320);

        TableColumn<Enrollment, String> cIdCol = new TableColumn<>("Course ID");
        cIdCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCourseId()));

        TableColumn<Enrollment, String> cNameCol = new TableColumn<>("Course Name");
        cNameCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCourseName()));

        TableColumn<Enrollment, String> teacherCol = new TableColumn<>("Teacher");
        teacherCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getTeacherName() != null ? d.getValue().getTeacherName() : "— Unassigned —"));

        TableColumn<Enrollment, String> semCol = new TableColumn<>("Semester");
        semCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getSemester()));

        TableColumn<Enrollment, String> attendanceCol = publishedMarkColumn("Attendance (5%)", Enrollment::getAttendance);
        TableColumn<Enrollment, String> quiz1Col = publishedMarkColumn("Quiz 1 (10%)", Enrollment::getQ1);
        TableColumn<Enrollment, String> quiz2Col = publishedMarkColumn("Quiz 2 (10%)", Enrollment::getQ2);
        TableColumn<Enrollment, String> assignmentCol = publishedMarkColumn("Assignment (5%)", Enrollment::getAssignment);
        TableColumn<Enrollment, String> midtermCol = publishedMarkColumn("Midterm (30%)", Enrollment::getMid);
        TableColumn<Enrollment, String> finalCol = publishedMarkColumn("Final Exam (40%)", Enrollment::getFinalExam);

        TableColumn<Enrollment, String> marksCol = new TableColumn<>("Total Marks");
        marksCol.setCellValueFactory(d -> new SimpleStringProperty("approved".equals(d.getValue().getPublicationStatus())
                ? String.format("%.1f", d.getValue().getTotalMarks()) : unpublishedResultMessage(d.getValue())));

        TableColumn<Enrollment, String> gradeCol = new TableColumn<>("Grade");
        gradeCol.setCellValueFactory(d -> new SimpleStringProperty("approved".equals(d.getValue().getPublicationStatus())
                ? d.getValue().getLetterGrade() + " (" + String.format("%.2f", d.getValue().getGradePoint()) + ")"
                : unpublishedResultMessage(d.getValue())));

        TableColumn<Enrollment, String> waiverCol = new TableColumn<>("Course Result");
        waiverCol.setCellValueFactory(d -> new SimpleStringProperty("approved".equals(d.getValue().getPublicationStatus())
                ? d.getValue().getResultStatus() : unpublishedResultMessage(d.getValue())));

        table.getColumns().addAll(cIdCol, cNameCol, teacherCol, semCol, attendanceCol, quiz1Col,
                quiz2Col, assignmentCol, midtermCol, finalCol, marksCol, gradeCol, waiverCol);
        table.getItems().addAll(enrollments);

        coursesCard.getChildren().addAll(cardTitle, table);
        box.getChildren().add(coursesCard);
        return box;
    }

    private VBox createTakeCourseSection(User user) {
        VBox box = new VBox(15);
        box.getStyleClass().add("glass-card");

        Label title = new Label("📚 Register for New Course (Semester Wise)");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: -fx-base-primary;");

        // Deadline check for course enrollment (with user-specific permission bypass)
        boolean isRegActive = deadlineDAO.isDeadlineActiveForUser("student_enroll", user.getId());
        if (!isRegActive) {
            Label warnLabel = new Label("⚠️ Course registration is closed. Contact management for permission.");
            warnLabel.setStyle("-fx-text-fill: #F97316; -fx-font-weight: bold;");
            warnLabel.setWrapText(true);
            box.getChildren().addAll(title, warnLabel);
            return box;
        }

        // Semester Switcher Dropdown
        HBox filterBar = new HBox(15);
        filterBar.setAlignment(Pos.CENTER_LEFT);

        Label semLabel = new Label("Select Target Semester:");
        semLabel.getStyleClass().add("text-main");

        ComboBox<String> semCombo = new ComboBox<>();
        for (int i = 1; i <= 8; i++) {
            semCombo.getItems().add("Semester " + i);
        }
        semCombo.setValue("Semester " + user.getCurrentSemester());

        Label termLabel = new Label("Term:");
        termLabel.getStyleClass().add("text-main");

        ComboBox<String> termCombo = new ComboBox<>();
        termCombo.getItems().addAll("Spring 2026 (Jan - Jun)", "Summer 2026 (Jul - Dec)");
        termCombo.setValue("Spring 2026 (Jan - Jun)");

        filterBar.getChildren().addAll(semLabel, semCombo, termLabel, termCombo);

        // Available Courses Table
        TableView<Course> courseTable = new TableView<>();
        courseTable.setPrefHeight(250);

        TableColumn<Course, String> cIdCol = new TableColumn<>("Course ID");
        cIdCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCourseId()));

        TableColumn<Course, String> cNameCol = new TableColumn<>("Course Name");
        cNameCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCourseName()));

        TableColumn<Course, String> credCol = new TableColumn<>("Credits");
        credCol.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getCredits())));

        TableColumn<Course, String> teacherCol = new TableColumn<>("Assigned Teacher");
        teacherCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTeacherName() != null ? d.getValue().getTeacherName() : "— Unassigned —"));

        courseTable.getColumns().addAll(cIdCol, cNameCol, credCol, teacherCol);

        Runnable loadCourses = () -> {
            int selectedSem = semCombo.getSelectionModel().getSelectedIndex() + 1;
            List<Course> courses = courseDAO.getCoursesByTargetSemester(selectedSem);
            courseTable.getItems().setAll(courses);
        };

        semCombo.setOnAction(e -> loadCourses.run());
        loadCourses.run();

        Button enrollBtn = new Button("Register Selected Course");
        enrollBtn.getStyleClass().add("btn-primary");

        ComboBox<String> attemptTypeCombo = new ComboBox<>();
        attemptTypeCombo.setPromptText("Select course attempt");
        attemptTypeCombo.setPrefWidth(260);
        attemptTypeCombo.setDisable(true);
        attemptTypeCombo.getStyleClass().add("attendance-filter");
        Label attemptHelp = new Label();
        attemptHelp.getStyleClass().add("text-muted");
        courseTable.getSelectionModel().selectedItemProperty().addListener((obs, previous, selected) -> {
            attemptTypeCombo.getItems().clear();
            attemptTypeCombo.setValue(null);
            attemptTypeCombo.setDisable(selected == null);
            if (selected == null) {
                attemptHelp.setText("Select a course row to see the available attempt type.");
                return;
            }
            Enrollment previousAttempt = enrollmentDAO.getLatestEnrollment(
                    user.getId(), selected.getCourseId(), user.getCurrentSemester());
            if (previousAttempt == null) {
                attemptTypeCombo.getItems().add("Regular");
                attemptHelp.setText("First enrollment for this course.");
                attemptTypeCombo.setValue("Regular");
            } else if ("incomplete".equals(previousAttempt.getAttemptType())
                    && (previousAttempt.isMidAbsent() || previousAttempt.isFinalAbsent())
                    && previousAttempt.getIncompleteDueSemester() >= user.getCurrentSemester()) {
                attemptTypeCombo.getItems().add("Incomplete");
                attemptHelp.setText("Complete the absent exam by Semester " + previousAttempt.getIncompleteDueSemester() + ".");
                attemptTypeCombo.setValue("Incomplete");
            } else if ("approved".equals(previousAttempt.getPublicationStatus())) {
                attemptTypeCombo.getItems().addAll("Retake", "Improvement");
                attemptHelp.setText("Retake resets all marks. Improvement preserves other marks; only mid and final can change.");
                attemptTypeCombo.setValue("Retake");
            } else if ("expired_incomplete".equals(previousAttempt.getPublicationStatus())) {
                attemptTypeCombo.getItems().add("Retake");
                attemptHelp.setText("The incomplete period expired; this course is now a retake.");
                attemptTypeCombo.setValue("Retake");
            } else {
                attemptHelp.setText("This course attempt is not complete yet.");
            }
        });

        Label statusLabel = new Label();

        enrollBtn.setOnAction(e -> {
            Course selected = courseTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                statusLabel.setText("Please click to select a course from the table above.");
                statusLabel.setStyle("-fx-text-fill: #F97316;");
                return;
            }

            String selectedAttempt = attemptTypeCombo.getValue();
            if (selectedAttempt == null) {
                statusLabel.setText("This course already has an active attempt. Finish it before registering again.");
                statusLabel.setStyle("-fx-text-fill: #F97316; -fx-font-weight: bold;");
                return;
            }

            String term = termCombo.getValue().contains("Spring") ? "Spring 2026" : "Summer 2026";
            boolean ok = enrollmentDAO.enrollCourse(user.getId(), selected.getCourseId(), term,
                    selectedAttempt.toLowerCase(), user.getCurrentSemester());
            if (ok) {
                statusLabel.setText("✅ Registered for " + selectedAttempt.toLowerCase() + " of "
                        + selected.getCourseId() + " (" + selected.getCourseName() + ").");
                statusLabel.setStyle("-fx-text-fill: #FDBA74; -fx-font-weight: bold;");
                courseTable.getSelectionModel().clearSelection();
            } else {
                statusLabel.setText("Could not register this course attempt. Refresh the page and check eligibility.");
                statusLabel.setStyle("-fx-text-fill: #F97316;");
            }
        });

        box.getChildren().addAll(title, statusLabel, filterBar, courseTable, attemptTypeCombo, attemptHelp, enrollBtn);
        return box;
    }

    private VBox createGradeReportSection(User user) {
        VBox box = new VBox(15);
        box.getStyleClass().add("glass-card");

        Label title = new Label("📑 Grade Report & Academic Transcript");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: -fx-base-primary;");

        HBox filterBar = new HBox(15);
        filterBar.setAlignment(Pos.CENTER_LEFT);

        Label scopeLabel = new Label("Result View Scope:");
        scopeLabel.getStyleClass().add("text-main");

        ComboBox<String> scopeCombo = new ComboBox<>();
        scopeCombo.getItems().add("All semesters");
        scopeCombo.getItems().addAll(enrollmentDAO.getSemestersForStudent(user.getId()));
        scopeCombo.setValue("All semesters");

        Button downloadPdfBtn = new Button("📥 Download PDF Grade Report");
        downloadPdfBtn.getStyleClass().add("btn-success");

        filterBar.getChildren().addAll(scopeLabel, scopeCombo, downloadPdfBtn);

        TableView<Enrollment> table = new TableView<>();
        table.setPrefHeight(300);

        TableColumn<Enrollment, String> cIdCol = new TableColumn<>("Course ID");
        cIdCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCourseId()));

        TableColumn<Enrollment, String> cNameCol = new TableColumn<>("Course Name");
        cNameCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCourseName()));

        TableColumn<Enrollment, String> semCol = new TableColumn<>("Semester");
        semCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getSemester()));

        TableColumn<Enrollment, String> attendanceCol = publishedMarkColumn("Attendance (5%)", Enrollment::getAttendance);
        TableColumn<Enrollment, String> quiz1Col = publishedMarkColumn("Quiz 1 (10%)", Enrollment::getQ1);
        TableColumn<Enrollment, String> quiz2Col = publishedMarkColumn("Quiz 2 (10%)", Enrollment::getQ2);
        TableColumn<Enrollment, String> assignmentCol = publishedMarkColumn("Assignment (5%)", Enrollment::getAssignment);
        TableColumn<Enrollment, String> midtermCol = publishedMarkColumn("Midterm (30%)", Enrollment::getMid);
        TableColumn<Enrollment, String> finalCol = publishedMarkColumn("Final Exam (40%)", Enrollment::getFinalExam);

        TableColumn<Enrollment, String> marksCol = new TableColumn<>("Total Marks");
        marksCol.setCellValueFactory(d -> new SimpleStringProperty(resultValue(d.getValue(),
                String.format("%.1f", d.getValue().getTotalMarks()))));

        TableColumn<Enrollment, String> gradeCol = new TableColumn<>("Grade");
        gradeCol.setCellValueFactory(d -> new SimpleStringProperty(resultValue(d.getValue(),
                d.getValue().getLetterGrade() + " (" + String.format("%.2f", d.getValue().getGradePoint()) + ")")));

        TableColumn<Enrollment, String> waiverCol = new TableColumn<>("Course Result");
        waiverCol.setCellValueFactory(d -> new SimpleStringProperty(resultValue(d.getValue(), d.getValue().getResultStatus())));

        TableColumn<Enrollment, String> statusCol = new TableColumn<>("Publication Status");
        statusCol.setCellValueFactory(d -> new SimpleStringProperty(publicationStatus(d.getValue())));

        table.getColumns().addAll(cIdCol, cNameCol, semCol, attendanceCol, quiz1Col, quiz2Col,
                assignmentCol, midtermCol, finalCol, marksCol, gradeCol, waiverCol, statusCol);

        Label statusMsg = new Label();
        Label waiverSummary = new Label();
        waiverSummary.getStyleClass().add("text-muted");
        waiverSummary.setWrapText(true);

        Runnable loadReport = () -> {
            String selectedSemester = scopeCombo.getValue();
            String term = "All semesters".equals(selectedSemester) ? "All" : selectedSemester;
            List<Enrollment> enrollments = enrollmentDAO.getEnrollmentsByStudent(user.getId(), term);
            table.getItems().setAll(enrollments);
            List<Enrollment> approved = enrollments.stream()
                    .filter(enrollment -> "approved".equals(enrollment.getPublicationStatus()))
                    .collect(Collectors.toList());
            waiverSummary.setText(getSemesterWaiverSummary(approved, selectedSemester));
        };

        scopeCombo.setOnAction(e -> loadReport.run());
        loadReport.run();

        downloadPdfBtn.setOnAction(e -> {
            List<Enrollment> list = new ArrayList<>();
            for (Enrollment enrollment : table.getItems()) {
                if ("approved".equals(enrollment.getPublicationStatus())) list.add(enrollment);
            }
            if (list.isEmpty()) {
                statusMsg.setText("No approved course results available to generate a report for this semester.");
                statusMsg.setStyle("-fx-text-fill: #F97316;");
                return;
            }

            double totalPts = 0; int totalCreds = 0;
            for (Enrollment en : list) {
                totalPts += (en.getGradePoint() * en.getCredits());
                totalCreds += en.getCredits();
            }
            double cgpa = totalCreds > 0 ? (totalPts / totalCreds) : 0.0;

            File pdf = PDFReportService.generateGradeReportPDF(user, list, scopeCombo.getValue(), cgpa);
            if (pdf != null && pdf.exists()) {
                statusMsg.setText("Grade Report saved successfully at: " + pdf.getAbsolutePath());
                statusMsg.setStyle("-fx-text-fill: #FDBA74;");
            } else {
                statusMsg.setText("Failed to generate PDF report.");
                statusMsg.setStyle("-fx-text-fill: #F97316;");
            }
        });

        box.getChildren().addAll(title, statusMsg, filterBar, waiverSummary, table);
        return box;
    }

    private int getTotalSemesterWaiverCredits(List<Enrollment> approvedEnrollments) {
        return approvedEnrollments.stream().map(Enrollment::getSemester).distinct()
                .mapToInt(semester -> semesterWaiverCredits(approvedEnrollments.stream()
                        .filter(enrollment -> semester.equals(enrollment.getSemester()))
                        .collect(Collectors.toList())))
                .sum();
    }

    private String getSemesterWaiverSummary(List<Enrollment> approvedEnrollments, String selectedSemester) {
        if (!"All semesters".equals(selectedSemester)) {
            double average = semesterAverage(approvedEnrollments);
            int credits = semesterWaiverCredits(approvedEnrollments);
            return approvedEnrollments.isEmpty() ? "No approved grades for this semester."
                    : String.format("Semester average: %.1f%% — %d credit waiver earned.", average, credits);
        }
        if (approvedEnrollments.isEmpty()) return "No approved grades available for semester waivers.";
        List<String> summaries = approvedEnrollments.stream().map(Enrollment::getSemester).distinct()
                .map(semester -> {
                    List<Enrollment> semesterResults = approvedEnrollments.stream()
                            .filter(enrollment -> semester.equals(enrollment.getSemester()))
                            .collect(Collectors.toList());
                    return semester + ": " + semesterWaiverCredits(semesterResults) + " credits";
                }).collect(Collectors.toList());
        return "Semester waiver credits: " + getTotalSemesterWaiverCredits(approvedEnrollments)
                + " total (" + String.join("; ", summaries) + ").";
    }

    private int semesterWaiverCredits(List<Enrollment> enrollments) {
        double average = semesterAverage(enrollments);
        if (average >= 95) return 6;
        if (average >= 85) return 3;
        return 0;
    }

    private double semesterAverage(List<Enrollment> enrollments) {
        if (enrollments.isEmpty()) return 0;
        return enrollments.stream().mapToDouble(Enrollment::getTotalMarks).sum() / enrollments.size();
    }

    private String resultValue(Enrollment enrollment, String approvedValue) {
        if ("approved".equals(enrollment.getPublicationStatus())) return approvedValue;
        if ("incomplete".equals(enrollment.getPublicationStatus())) return "Incomplete";
        if ("review_requested".equals(enrollment.getPublicationStatus())) return "";
        return "—";
    }

    private TableColumn<Enrollment, String> publishedMarkColumn(
            String title, java.util.function.Function<Enrollment, Double> markValue) {
        TableColumn<Enrollment, String> column = new TableColumn<>(title);
        column.setCellValueFactory(d -> {
            Enrollment enrollment = d.getValue();
            if (!"approved".equals(enrollment.getPublicationStatus())) {
                return new SimpleStringProperty(unpublishedResultMessage(enrollment));
            }
            Double mark = markValue.apply(enrollment);
            return new SimpleStringProperty(String.format("%.1f", mark == null ? 0.0 : mark));
        });
        return column;
    }

    private String publicationStatus(Enrollment enrollment) {
        if ("review_requested".equals(enrollment.getPublicationStatus())) return "";
        if ("incomplete".equals(enrollment.getPublicationStatus())) {
            if (enrollment.isMidAbsent() && enrollment.isFinalAbsent()) return "Incomplete (Mid & Final absent)";
            if (enrollment.isMidAbsent()) return "Incomplete (Mid absent)";
            if (enrollment.isFinalAbsent()) return "Incomplete (Final absent)";
        }
        if ("expired_incomplete".equals(enrollment.getPublicationStatus())) return "Incomplete expired — retake";
        return enrollment.getPublicationStatus() == null ? "Not submitted" : enrollment.getPublicationStatus();
    }

    private VBox createStatCard(String title, String value, String colorHex) {
        VBox card = new VBox(8);
        card.getStyleClass().add("glass-card");
        HBox.setHgrow(card, Priority.ALWAYS);
        card.setPadding(new Insets(15, 20, 15, 20));

        Label tLabel = new Label(title);
        tLabel.getStyleClass().add("text-muted");
        tLabel.setStyle("-fx-font-size: 12px;");

        Label vLabel = new Label(value);
        vLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: " + colorHex + ";");

        card.getChildren().addAll(tLabel, vLabel);
        return card;
    }

    private String unpublishedResultMessage(Enrollment enrollment) {
        if ("incomplete".equals(enrollment.getPublicationStatus())) return publicationStatus(enrollment);
        if ("review_requested".equals(enrollment.getPublicationStatus())) return "";
        if ("pending".equals(enrollment.getPublicationStatus())) return "Pending approval";
        return "Not submitted";
    }
}
