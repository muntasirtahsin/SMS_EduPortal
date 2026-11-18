package com.eduportal.ui;

import com.eduportal.dao.CourseDAO;
import com.eduportal.dao.DeadlineDAO;
import com.eduportal.dao.EnrollmentDAO;
import com.eduportal.models.Course;
import com.eduportal.models.Enrollment;
import com.eduportal.models.User;
import com.eduportal.utils.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;

public class TeacherDashboardView extends VBox {

    private final String activeTab;
    private final int reviewEnrollmentId;
    private final CourseDAO courseDAO = new CourseDAO();
    private final EnrollmentDAO enrollmentDAO = new EnrollmentDAO();
    private final DeadlineDAO deadlineDAO = new DeadlineDAO();

    public TeacherDashboardView(String activeTab) {
        this(activeTab, 0);
    }

    public TeacherDashboardView(String activeTab, int reviewEnrollmentId) {
        this.activeTab = activeTab;
        this.reviewEnrollmentId = reviewEnrollmentId;
        initUI();
    }

    private void initUI() {
        this.setSpacing(20);
        this.setPadding(new Insets(10));

        User teacher = SessionManager.getCurrentUser();
        if (teacher == null) return;

        Label title = new Label("Teacher Academic Portal");
        title.getStyleClass().add("page-title");

        Label subtitle = new Label("Welcome, Dr. " + teacher.getName() + "! Manage assigned courses, enrollments, and student grades.");
        subtitle.getStyleClass().add("page-subtitle");

        VBox header = new VBox(5, title, subtitle);
        this.getChildren().add(header);

        if ("take_course".equalsIgnoreCase(activeTab)) {
            this.getChildren().add(createTakeCourseSection(teacher));
        } else if ("enter_marks".equalsIgnoreCase(activeTab)) {
            this.getChildren().add(createEnterMarksSection(teacher));
        } else if ("attendance".equalsIgnoreCase(activeTab)) {
            this.getChildren().add(new AttendanceView());
        } else {
            // Default Dashboard: Read-only Assigned Courses Overview
            this.getChildren().add(createAssignedCoursesDashboardSection(teacher));
        }
    }

    private VBox createAssignedCoursesDashboardSection(User teacher) {
        VBox box = new VBox(15);
        box.getStyleClass().add("glass-card");

        Label label = new Label("📚 My Assigned Courses Overview");
        label.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: -fx-base-primary;");

        TableView<Course> table = new TableView<>();
        table.setPrefHeight(300);

        TableColumn<Course, String> cIdCol = new TableColumn<>("Course ID");
        cIdCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCourseId()));

        TableColumn<Course, String> cNameCol = new TableColumn<>("Course Name");
        cNameCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCourseName()));

        TableColumn<Course, String> studentCountCol = new TableColumn<>("Total Enrolled Students");
        studentCountCol.setCellValueFactory(d -> {
            List<Enrollment> list = enrollmentDAO.getEnrollmentsByCourse(d.getValue().getCourseId());
            return new SimpleStringProperty(list.size() + " Students");
        });

        TableColumn<Course, String> credCol = new TableColumn<>("Course Credits");
        credCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCredits() + " Credits"));

        TableColumn<Course, String> semCol = new TableColumn<>("Target Semester");
        semCol.setCellValueFactory(d -> new SimpleStringProperty("Semester " + d.getValue().getTargetSemester()));

        table.getColumns().addAll(cIdCol, cNameCol, studentCountCol, credCol, semCol);
        table.getItems().addAll(courseDAO.getCoursesByTeacher(teacher.getId()));

        box.getChildren().addAll(label, table);
        return box;
    }

    private VBox createTakeCourseSection(User teacher) {
        VBox box = new VBox(15);
        box.getStyleClass().add("glass-card");

        Label label = new Label("📚 Apply to Teach an Available Course");
        label.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: -fx-base-primary;");

        // Deadline check for teacher course registration (with user-specific permission bypass)
        boolean isTakeActive = deadlineDAO.isDeadlineActiveForUser("teacher_enroll", teacher.getId());
        if (!isTakeActive) {
            Label warnLabel = new Label("⚠️ Course registration is closed. Contact management for permission.");
            warnLabel.setStyle("-fx-text-fill: #F97316; -fx-font-weight: bold;");
            warnLabel.setWrapText(true);
            box.getChildren().addAll(label, warnLabel);
            return box;
        }

        Label desc = new Label("Apply for an unassigned course. An administrator will review your request and assign the course if approved.");
        desc.getStyleClass().add("text-muted");
        desc.setWrapText(true);

        ComboBox<String> semesterCombo = new ComboBox<>();
        semesterCombo.getItems().addAll("Semester 1", "Semester 2", "Semester 3", "Semester 4",
                "Semester 5", "Semester 6", "Semester 7", "Semester 8");
        semesterCombo.setPromptText("1. Select semester");
        semesterCombo.setPrefWidth(200);

        ComboBox<Course> courseCombo = new ComboBox<>();
        courseCombo.setPromptText("2. Select an available course");
        courseCombo.setMaxWidth(450);
        courseCombo.setPrefWidth(360);
        courseCombo.setDisable(true);
        Label availability = new Label("Choose a semester to see available courses.");
        availability.getStyleClass().add("text-muted");

        semesterCombo.setOnAction(e -> {
            courseCombo.getItems().clear();
            courseCombo.setValue(null);
            int semester = semesterCombo.getSelectionModel().getSelectedIndex() + 1;
            List<Course> available = courseDAO.getAvailableCoursesForTeachers(semester, teacher.getId());
            courseCombo.getItems().setAll(available);
            courseCombo.setDisable(available.isEmpty());
            availability.setText(available.isEmpty()
                    ? "No unassigned courses are available for this semester."
                    : available.size() + " available course(s).");
        });

        Button applyBtn = new Button("Submit Application");
        applyBtn.getStyleClass().add("btn-primary");

        Label statusMsg = new Label();

        applyBtn.setOnAction(e -> {
            Course selected = courseCombo.getValue();
            if (selected == null) {
                statusMsg.setText("Please select an available course.");
                statusMsg.setStyle("-fx-text-fill: #F97316;");
                return;
            }

            boolean ok = courseDAO.applyForCourse(selected.getCourseId(), teacher.getId());
            if (ok) {
                statusMsg.setText("Application submitted. An administrator must approve the assignment.");
                statusMsg.setStyle("-fx-text-fill: #FDBA74; -fx-font-weight: bold;");
                courseCombo.getItems().remove(selected);
                courseCombo.setValue(null);
            } else {
                statusMsg.setText("Failed to submit application.");
                statusMsg.setStyle("-fx-text-fill: #F97316;");
            }
        });

        HBox form = new HBox(12, semesterCombo, courseCombo, applyBtn);
        form.setAlignment(Pos.CENTER_LEFT);

        box.getChildren().addAll(label, statusMsg, desc, form, availability);
        return box;
    }

    private VBox createEnterMarksSection(User teacher) {
        VBox courseBox = new VBox(15);
        courseBox.getStyleClass().add("glass-card");

        Label selectLabel = new Label("📝 Enter Marks");
        selectLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: -fx-base-primary;");

        boolean isMarksSubmissionActive = deadlineDAO.isDeadlineActiveForUser("teacher_marks", teacher.getId());
        Label deadlineNotice = new Label();
        if (!isMarksSubmissionActive) {
            deadlineNotice.setText("⚠️ Result submission is closed. You can view enrolled students, but cannot edit marks.");
            deadlineNotice.setStyle("-fx-text-fill: #F97316; -fx-font-weight: bold;");
            deadlineNotice.setWrapText(true);
        }

        VBox semesterStep = new VBox(8);
        Label semesterStepLabel = new Label("1. Select semester");
        semesterStepLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        ComboBox<String> semesterCombo = new ComboBox<>();
        semesterCombo.getItems().addAll("Semester 1", "Semester 2", "Semester 3", "Semester 4",
                "Semester 5", "Semester 6", "Semester 7", "Semester 8");
        semesterCombo.setPromptText("Choose a semester");
        semesterCombo.setMaxWidth(Double.MAX_VALUE);
        semesterStep.getChildren().addAll(semesterStepLabel, semesterCombo);

        VBox courseStep = new VBox(8);
        Label courseStepLabel = new Label("2. Select course");
        courseStepLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        ComboBox<Course> courseCombo = new ComboBox<>();
        courseCombo.setPromptText("Choose an assigned course");
        courseCombo.setMaxWidth(Double.MAX_VALUE);
        courseCombo.setDisable(true);
        Label courseHint = new Label("Select a semester to see your assigned courses.");
        courseHint.getStyleClass().add("text-muted");
        courseStep.getChildren().addAll(courseStepLabel, courseCombo, courseHint);
        courseStep.setVisible(false);
        courseStep.setManaged(false);
        List<Course> assignedCourses = courseDAO.getCoursesByTeacher(teacher.getId());
        Enrollment reviewTarget = reviewEnrollmentId > 0
                ? enrollmentDAO.getEnrollmentForTeacherReview(reviewEnrollmentId, teacher.getId()) : null;
        Label rosterHint = new Label("Choose a semester, then a course to load its students.");
        rosterHint.getStyleClass().add("text-muted");

        TableView<Enrollment> studentTable = new TableView<>();
        studentTable.setPrefHeight(260);

        TableColumn<Enrollment, String> sIdCol = new TableColumn<>("Student ID");
        sIdCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStudentId()));

        TableColumn<Enrollment, String> sNameCol = new TableColumn<>("Student Name");
        sNameCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStudentName()));

        TableColumn<Enrollment, String> resultStatusCol = new TableColumn<>("Result Status");
        resultStatusCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().isMidAbsent() || d.getValue().isFinalAbsent()
                        ? "Incomplete" : d.getValue().getPublicationStatus()));

        TableColumn<Enrollment, String> attCol = new TableColumn<>("Attendance (5%)");
        attCol.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getAttendance())));

        TableColumn<Enrollment, String> q1Col = new TableColumn<>("Quiz 1 (10%)");
        q1Col.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getQ1())));

        TableColumn<Enrollment, String> q2Col = new TableColumn<>("Quiz 2 (10%)");
        q2Col.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getQ2())));

        TableColumn<Enrollment, String> assCol = new TableColumn<>("Assignment (5%)");
        assCol.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getAssignment())));

        TableColumn<Enrollment, String> midCol = new TableColumn<>("Midterm (30%)");
        midCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().isMidAbsent()
                ? "Absent" : String.valueOf(d.getValue().getMid())));

        TableColumn<Enrollment, String> finCol = new TableColumn<>("Final (40%)");
        finCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().isFinalAbsent()
                ? "Absent" : String.valueOf(d.getValue().getFinalExam())));

        TableColumn<Enrollment, String> totCol = new TableColumn<>("Total (100%)");
        totCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().isMidAbsent() || d.getValue().isFinalAbsent()
                ? "Incomplete" : String.format("%.1f", d.getValue().getTotalMarks())));

        studentTable.getColumns().addAll(sIdCol, sNameCol, resultStatusCol, attCol, q1Col, q2Col, assCol, midCol, finCol, totCol);

        VBox rosterStep = new VBox(10);
        Label rosterStepLabel = new Label("3. Registered students");
        rosterStepLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        rosterStep.getChildren().addAll(rosterStepLabel, rosterHint, studentTable);
        rosterStep.setVisible(false);
        rosterStep.setManaged(false);

        semesterCombo.valueProperty().addListener((obs, previousSemester, selectedSemesterName) -> {
            courseCombo.getSelectionModel().clearSelection();
            studentTable.getItems().clear();
            studentTable.getSelectionModel().clearSelection();
            rosterStep.setVisible(false);
            rosterStep.setManaged(false);
            courseStep.setVisible(selectedSemesterName != null);
            courseStep.setManaged(selectedSemesterName != null);
            if (selectedSemesterName == null) {
                courseCombo.getItems().clear();
                courseCombo.setDisable(true);
                courseHint.setText("Select a semester to see your assigned courses.");
                return;
            }
            int selectedSemester = semesterCombo.getItems().indexOf(selectedSemesterName) + 1;
            List<Course> semesterCourses = assignedCourses.stream()
                    .filter(course -> course.getTargetSemester() == selectedSemester)
                    .collect(java.util.stream.Collectors.toList());
            courseCombo.getItems().setAll(semesterCourses);
            courseCombo.setDisable(semesterCourses.isEmpty());
            courseHint.setText(semesterCourses.isEmpty()
                    ? "No courses are assigned to you for this semester."
                    : semesterCourses.size() + " assigned course(s) available.");
            rosterHint.setText(semesterCourses.isEmpty()
                    ? "No assigned courses for this semester."
                    : "Select a course to view all registered students.");
        });

        courseCombo.valueProperty().addListener((obs, previousCourse, selected) -> {
            studentTable.getItems().clear();
            studentTable.getSelectionModel().clearSelection();
            boolean showRoster = selected != null;
            rosterStep.setVisible(showRoster);
            rosterStep.setManaged(showRoster);
            if (showRoster) {
                List<Enrollment> roster = enrollmentDAO.getEnrollmentsByCourse(selected.getCourseId());
                studentTable.getItems().setAll(roster);
                rosterHint.setText(roster.isEmpty() ? "No students are registered in this course."
                        : roster.size() + " registered student(s). Select a student to enter marks.");
            }
        });

        // Edit Marks Form
        VBox editForm = new VBox(10);
        editForm.setPadding(new Insets(10, 0, 0, 0));

        TextField attField = new TextField(); attField.setPrefWidth(75);
        TextField q1Field = new TextField(); q1Field.setPrefWidth(75);
        TextField q2Field = new TextField(); q2Field.setPrefWidth(75);
        TextField assField = new TextField(); assField.setPrefWidth(75);
        TextField midField = new TextField(); midField.setPrefWidth(75);
        TextField finField = new TextField(); finField.setPrefWidth(75);
        HBox markFields = new HBox(10,
                createMarkField(attField, "Attendance (5%)"),
                createMarkField(q1Field, "Quiz 1 (10%)"),
                createMarkField(q2Field, "Quiz 2 (10%)"),
                createMarkField(assField, "Assignment (5%)"),
                createMarkField(midField, "Midterm (30%)"),
                createMarkField(finField, "Final Exam (40%)"));

        Button saveMarksBtn = new Button("Save and Submit Result");
        saveMarksBtn.getStyleClass().add("btn-primary");

        Button removeStudentBtn = new Button("❌ Remove Student from Course");
        removeStudentBtn.getStyleClass().add("btn-danger");
        removeStudentBtn.setDisable(true);
        saveMarksBtn.setDisable(true);

        Label statusMsg = new Label();

        studentTable.getSelectionModel().selectedItemProperty().addListener((obs, o, selectedStudent) -> {
            boolean selected = selectedStudent != null;
            saveMarksBtn.setDisable(!selected || !isMarksSubmissionActive);
            removeStudentBtn.setDisable(!selected || !isMarksSubmissionActive);
            if (!selected) {
                attField.setDisable(true);
                q1Field.setDisable(true);
                q2Field.setDisable(true);
                assField.setDisable(true);
                midField.setDisable(true);
                finField.setDisable(true);
                return;
            }
            if (selectedStudent != null) {
                attField.setText(String.valueOf(selectedStudent.getAttendance()));
                q1Field.setText(String.valueOf(selectedStudent.getQ1()));
                q2Field.setText(String.valueOf(selectedStudent.getQ2()));
                assField.setText(String.valueOf(selectedStudent.getAssignment()));
                midField.setText(selectedStudent.isMidAbsent() ? "Absent" : String.valueOf(selectedStudent.getMid()));
                finField.setText(selectedStudent.isFinalAbsent() ? "Absent" : String.valueOf(selectedStudent.getFinalExam()));
                boolean improvement = "improvement".equals(selectedStudent.getAttemptType());
                boolean incomplete = "incomplete".equals(selectedStudent.getAttemptType());
                attField.setDisable(!isMarksSubmissionActive || improvement || incomplete);
                q1Field.setDisable(!isMarksSubmissionActive || improvement || incomplete);
                q2Field.setDisable(!isMarksSubmissionActive || improvement || incomplete);
                assField.setDisable(!isMarksSubmissionActive || improvement || incomplete);
                midField.setDisable(!isMarksSubmissionActive
                        || incomplete && !selectedStudent.isMidAbsent());
                finField.setDisable(!isMarksSubmissionActive
                        || incomplete && !selectedStudent.isFinalAbsent());
                if (improvement) {
                    statusMsg.setText("Improvement: only midterm and final marks can be changed.");
                } else if (incomplete) {
                    statusMsg.setText("Incomplete: enter marks only for the absent exam(s), by Semester "
                            + selectedStudent.getIncompleteDueSemester() + ".");
                } else {
                    statusMsg.setText("");
                }
            }
        });

        saveMarksBtn.setOnAction(e -> {
            Enrollment selectedStudent = studentTable.getSelectionModel().getSelectedItem();
            if (selectedStudent == null) {
                statusMsg.setText("Please click and select a student from the table.");
                statusMsg.setStyle("-fx-text-fill: #F97316;");
                return;
            }

            try {
                boolean ok = enrollmentDAO.updateMarks(selectedStudent.getId(), attField.getText(), q1Field.getText(),
                        q2Field.getText(), assField.getText(), midField.getText(), finField.getText(), teacher.getId());
                if (ok) {
                    statusMsg.setText("incomplete".equals(selectedStudent.getAttemptType())
                            && (selectedStudent.isMidAbsent() || selectedStudent.isFinalAbsent())
                            ? "Incomplete exam marks saved." : "✅ Result submitted to the administrator for approval.");
                    statusMsg.setStyle("-fx-text-fill: #FDBA74; -fx-font-weight: bold;");
                    if (courseCombo.getValue() != null) {
                        studentTable.getItems().setAll(enrollmentDAO.getEnrollmentsByCourse(courseCombo.getValue().getCourseId()));
                    }
                } else {
                    statusMsg.setText("Could not save marks. Check the values and the student's attempt restrictions.");
                    statusMsg.setStyle("-fx-text-fill: #F97316; -fx-font-weight: bold;");
                }
            } catch (Exception ex) {
                statusMsg.setText("Error saving marks: Please enter valid numeric values.");
                statusMsg.setStyle("-fx-text-fill: #F97316;");
            }
        });

        removeStudentBtn.setOnAction(e -> {
            Enrollment selectedStudent = studentTable.getSelectionModel().getSelectedItem();
            Course selectedCourse = courseCombo.getValue();

            if (selectedStudent == null || selectedCourse == null) {
                statusMsg.setText("Please select a course and click a student from the table to remove.");
                statusMsg.setStyle("-fx-text-fill: #F97316;");
                return;
            }

            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Remove Student from Course");
            confirmAlert.setHeaderText("Remove " + selectedStudent.getStudentName() + " (" + selectedStudent.getStudentId() + ")?");
            confirmAlert.setContentText("Are you sure you want to remove this student from " + selectedCourse.getCourseId() + " (" + selectedCourse.getCourseName() + ")?\nThis action will unenroll the student from your course.");

            var choice = confirmAlert.showAndWait();
            if (choice.isPresent() && choice.get() == ButtonType.OK) {
                boolean removed = enrollmentDAO.deleteEnrollmentById(selectedStudent.getId());
                if (removed) {
                    statusMsg.setText("✅ Removed student " + selectedStudent.getStudentName() + " (" + selectedStudent.getStudentId() + ") from " + selectedCourse.getCourseId() + ".");
                    statusMsg.setStyle("-fx-text-fill: #FDBA74; -fx-font-weight: bold;");

                    // Refresh table & clear input fields
                    studentTable.getItems().setAll(enrollmentDAO.getEnrollmentsByCourse(selectedCourse.getCourseId()));
                    attField.clear(); q1Field.clear(); q2Field.clear(); assField.clear(); midField.clear(); finField.clear();

                } else {
                    statusMsg.setText("Failed to remove student from course.");
                    statusMsg.setStyle("-fx-text-fill: #F97316;");
                }
            }
        });

        attField.setPromptText("Attendance /5");
        q1Field.setPromptText("Quiz 1 /10");
        q2Field.setPromptText("Quiz 2 /10");
        assField.setPromptText("Assignment /5");
        midField.setPromptText("Mid /30");
        finField.setPromptText("Final /40");
        attField.setDisable(true);
        q1Field.setDisable(true);
        q2Field.setDisable(true);
        assField.setDisable(true);
        midField.setDisable(true);
        finField.setDisable(true);
        saveMarksBtn.setDisable(true);
        removeStudentBtn.setDisable(true);
        if (reviewEnrollmentId > 0) {
            if (reviewTarget == null) {
                statusMsg.setText("The requested result is no longer assigned to you or is unavailable.");
                statusMsg.setStyle("-fx-text-fill: #F97316;");
            } else {
                Course reviewCourse = assignedCourses.stream()
                        .filter(course -> course.getCourseId().equals(reviewTarget.getCourseId()))
                        .findFirst().orElse(null);
                if (reviewCourse != null) {
                    semesterCombo.setValue("Semester " + reviewCourse.getTargetSemester());
                    courseCombo.setValue(reviewCourse);
                    for (int i = 0; i < studentTable.getItems().size(); i++) {
                        if (studentTable.getItems().get(i).getId() == reviewEnrollmentId) {
                            studentTable.getSelectionModel().select(i);
                            studentTable.scrollTo(i);
                            break;
                        }
                    }
                } else {
                    statusMsg.setText("The course for this review request is no longer assigned to you.");
                    statusMsg.setStyle("-fx-text-fill: #F97316;");
                }
            }
        }
        editForm.getChildren().addAll(markFields, new HBox(10, saveMarksBtn, removeStudentBtn));
        VBox marksStep = new VBox(10, new Label("Student marks"), editForm);
        rosterStep.getChildren().add(marksStep);
        courseBox.getChildren().addAll(selectLabel, deadlineNotice, statusMsg, semesterStep, courseStep, rosterStep);
        return courseBox;
    }

    private VBox createMarkField(TextField field, String labelText) {
        Label label = new Label(labelText);
        label.getStyleClass().add("mark-field-label");
        label.setWrapText(true);
        label.setMaxWidth(85);
        VBox fieldBox = new VBox(4, field, label);
        fieldBox.setPrefWidth(85);
        return fieldBox;
    }

}
