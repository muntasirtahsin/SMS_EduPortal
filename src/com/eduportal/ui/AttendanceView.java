package com.eduportal.ui;

import com.eduportal.dao.AttendanceDAO;
import com.eduportal.dao.CourseDAO;
import com.eduportal.dao.EnrollmentDAO;
import com.eduportal.models.AttendanceRecord;
import com.eduportal.models.Course;
import com.eduportal.models.User;
import com.eduportal.utils.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class AttendanceView extends VBox {

    private static final List<String> STATUSES = List.of("Unmarked", "Present", "Absent", "Late", "Excused");

    private final AttendanceDAO attendanceDAO = new AttendanceDAO();
    private final CourseDAO courseDAO = new CourseDAO();
    private final EnrollmentDAO enrollmentDAO = new EnrollmentDAO();

    public AttendanceView() {
        setSpacing(16);
        setPadding(new Insets(10));

        User user = SessionManager.getCurrentUser();
        if (user == null) return;

        Label title = new Label("Attendance");
        title.getStyleClass().add("page-title");
        getChildren().add(title);

        if ("teacher".equalsIgnoreCase(user.getRole())) {
            getChildren().add(createTeacherCourseAttendance(user));
        } else if ("student".equalsIgnoreCase(user.getRole())) {
            getChildren().add(createStudentAttendance(user));
        } else if ("admin".equalsIgnoreCase(user.getRole())) {
            getChildren().add(createAdminAttendance());
        }
    }

    private VBox createTeacherCourseAttendance(User teacher) {
        VBox section = new VBox(12);
        section.getStyleClass().add("glass-card");
        section.getChildren().add(new Label("Manage attendance for assigned courses"));

        ComboBox<String> semesterPicker = semesterPicker("1. Select semester");
        ComboBox<Course> coursePicker = coursePicker("2. Select course");
        coursePicker.setDisable(true);
        DatePicker datePicker = new DatePicker(LocalDate.now());

        TableView<AttendanceRecord> table = new TableView<>();
        table.setPrefHeight(340);
        table.getColumns().addAll(courseColumn(), studentIdColumn(), studentColumn(), dateColumn(),
                editableStatusColumn(AttendanceRecord::getStatus, AttendanceRecord::setStatus));

        Label hint = new Label("Choose a semester and course, then select Present/Absent from the visible status menu.");
        hint.getStyleClass().add("text-muted");
        Label message = new Label();

        Runnable loadRoster = () -> {
            Course course = coursePicker.getValue();
            if (course != null && datePicker.getValue() != null) {
                table.getItems().setAll(attendanceDAO.getCourseAttendance(course.getCourseId(),
                        datePicker.getValue().toString()));
                hint.setText(table.getItems().size() + " enrolled student(s). Select a status in the Attendance column.");
            }
        };
        semesterPicker.setOnAction(e -> {
            int semester = semesterPicker.getSelectionModel().getSelectedIndex() + 1;
            List<Course> courses = courseDAO.getCoursesByTeacher(teacher.getId()).stream()
                    .filter(course -> course.getTargetSemester() == semester).collect(Collectors.toList());
            coursePicker.getItems().setAll(courses);
            coursePicker.setValue(null);
            coursePicker.setDisable(courses.isEmpty());
            table.getItems().clear();
            hint.setText(courses.isEmpty() ? "No assigned courses for this semester."
                    : "Choose a course to load its enrolled students.");
        });
        coursePicker.setOnAction(e -> loadRoster.run());
        datePicker.setOnAction(e -> loadRoster.run());

        Button save = new Button("Save Attendance");
        save.getStyleClass().add("btn-primary");
        save.setOnAction(e -> {
            Course course = coursePicker.getValue();
            if (course == null || datePicker.getValue() == null) {
                showMessage(message, "Select a course and attendance date.", false);
                return;
            }
            boolean success = true;
            for (AttendanceRecord record : table.getItems()) {
                if ("Unmarked".equals(record.getStatus())) {
                    success &= attendanceDAO.clearStudentAttendance(course.getCourseId(), record.getStudentId(),
                            datePicker.getValue().toString());
                } else {
                    success &= attendanceDAO.saveStudentAttendance(course.getCourseId(), record.getStudentId(),
                            datePicker.getValue().toString(), record.getStatus(), teacher.getId());
                }
            }
            showMessage(message, success ? "Attendance saved." : "Some attendance records could not be saved.", success);
        });

        section.getChildren().addAll(message, new HBox(10, semesterPicker, coursePicker, datePicker),
                hint, table, save);
        return section;
    }

    private VBox createStudentAttendance(User student) {
        VBox section = new VBox(12);
        section.getStyleClass().add("glass-card");
        section.getChildren().add(new Label("My attendance (view only)"));

        ComboBox<String> semesterPicker = semesterPicker("Select semester");
        semesterPicker.getItems().add(0, "All semesters");
        semesterPicker.setValue("All semesters");
        ComboBox<Course> coursePicker = coursePicker("All courses");
        DatePicker datePicker = new DatePicker();
        datePicker.setPromptText("Choose date (optional)");
        Label summary = new Label();
        TableView<AttendanceRecord> table = new TableView<>();
        table.setPrefHeight(360);
        table.getColumns().addAll(courseColumn(), dateColumn(), statusDisplayColumn());

        List<AttendanceRecord> allRecords = attendanceDAO.getStudentAttendance(student.getId());
        Runnable reloadCourses = () -> {
            int semesterIndex = semesterPicker.getSelectionModel().getSelectedIndex();
            List<Course> availableCourses = semesterIndex == 0 ? courseDAO.getAllCourses()
                    : courseDAO.getCoursesByTargetSemester(semesterIndex);
            List<Course> semesterCourses = availableCourses.stream()
                    .filter(course -> allRecords.stream().anyMatch(record -> record.getCourseId().equals(course.getCourseId())))
                    .collect(Collectors.toList());
            coursePicker.getItems().setAll(semesterCourses);
            coursePicker.setValue(null);
            coursePicker.setDisable(semesterCourses.isEmpty());
            reloadAttendance(allRecords, coursePicker, semesterPicker, datePicker, table, summary);
        };
        semesterPicker.setOnAction(e -> reloadCourses.run());
        coursePicker.setOnAction(e -> reloadAttendance(allRecords, coursePicker, semesterPicker, datePicker, table, summary));
        datePicker.setOnAction(e -> reloadAttendance(allRecords, coursePicker, semesterPicker, datePicker, table, summary));

        section.getChildren().addAll(new HBox(10, semesterPicker, coursePicker, datePicker),
                summary, table);
        reloadCourses.run();
        return section;
    }

    private VBox createAdminAttendance() {
        VBox section = new VBox(12);
        section.getStyleClass().add("glass-card");
        section.getChildren().add(new Label("Student attendance (view only)"));

        ComboBox<String> semesterPicker = semesterPicker("1. Select semester");
        ComboBox<Course> coursePicker = coursePicker("2. Select course");
        coursePicker.setDisable(true);
        TextField studentIdField = new TextField();
        studentIdField.setPromptText("Enter student ID");
        studentIdField.setPrefWidth(200);
        Button searchButton = new Button("Search Attendance");
        searchButton.getStyleClass().add("btn-primary");
        DatePicker datePicker = new DatePicker();
        datePicker.setPromptText("Choose date (optional)");
        Label summary = new Label("Select a semester and course, then search by student ID.");
        summary.getStyleClass().add("text-muted");
        TableView<AttendanceRecord> table = new TableView<>();
        table.setPrefHeight(360);
        table.getColumns().addAll(courseColumn(), studentIdColumn(), studentColumn(),
                dateColumn(), statusDisplayColumn());

        List<AttendanceRecord> allRecords = attendanceDAO.getAllStudentAttendance();
        final String[] searchedStudentId = {null};
        Runnable searchAttendance = () -> {
            String studentId = studentIdField.getText().trim();
            Course selectedCourse = coursePicker.getValue();
            int semester = semesterPicker.getItems().indexOf(semesterPicker.getValue()) + 1;
            if (semester < 1 || selectedCourse == null) {
                summary.setText("Select a semester and course before searching.");
                table.getItems().clear();
                return;
            }
            if (studentId.isEmpty()) {
                summary.setText("Enter a student ID to view attendance.");
                table.getItems().clear();
                return;
            }
            if (selectedCourse.getTargetSemester() != semester) {
                summary.setText("The selected course does not belong to this semester.");
                table.getItems().clear();
                return;
            }
            searchedStudentId[0] = studentId;
            List<AttendanceRecord> filtered = allRecords.stream()
                    .filter(record -> record.getCourseId().equals(selectedCourse.getCourseId()))
                    .filter(record -> record.getStudentId().equalsIgnoreCase(studentId))
                    .collect(Collectors.toList());
            if (datePicker.getValue() != null) {
                String date = datePicker.getValue().toString();
                filtered.removeIf(record -> !record.getAttendanceDate().equals(date));
            }
            if (filtered.isEmpty()) {
                summary.setText("No attendance records found for student " + studentId
                        + " in " + selectedCourse.getCourseId() + ".");
            } else {
                displayAttendance(filtered, table, summary);
            }
        };
        semesterPicker.valueProperty().addListener((obs, previous, selectedSemester) -> {
            int semester = semesterPicker.getItems().indexOf(selectedSemester) + 1;
            List<Course> courses = semester < 1 ? List.of() : courseDAO.getCoursesByTargetSemester(semester);
            coursePicker.getItems().setAll(courses);
            coursePicker.setValue(null);
            coursePicker.setDisable(courses.isEmpty());
            studentIdField.clear();
            searchedStudentId[0] = null;
            table.getItems().clear();
            summary.setText(courses.isEmpty()
                    ? "No courses are available for this semester."
                    : "Select a course, then search by student ID.");
        });
        coursePicker.valueProperty().addListener((obs, previous, selectedCourse) -> {
            studentIdField.clear();
            searchedStudentId[0] = null;
            table.getItems().clear();
            summary.setText(selectedCourse == null
                    ? "Select a semester and course, then search by student ID."
                    : "Enter a student ID to view attendance for " + selectedCourse.getCourseId() + ".");
        });
        searchButton.setOnAction(e -> searchAttendance.run());
        studentIdField.setOnAction(e -> searchAttendance.run());
        datePicker.setOnAction(e -> {
            if (searchedStudentId[0] != null) searchAttendance.run();
        });
        section.getChildren().addAll(new HBox(10, semesterPicker, coursePicker, studentIdField,
                        searchButton, datePicker),
                summary, table);
        return section;
    }

    private void reloadAttendance(List<AttendanceRecord> records, ComboBox<Course> coursePicker,
                                  DatePicker datePicker, TableView<AttendanceRecord> table, Label summary) {
        List<AttendanceRecord> filtered = new ArrayList<>(records);
        if (coursePicker.getValue() != null) {
            filtered.removeIf(record -> !record.getCourseId().equals(coursePicker.getValue().getCourseId()));
        }
        if (datePicker.getValue() != null) {
            filtered.removeIf(record -> !record.getAttendanceDate().equals(datePicker.getValue().toString()));
        }
        displayAttendance(filtered, table, summary);
    }

    private void reloadAttendance(List<AttendanceRecord> records, ComboBox<Course> coursePicker,
                                  ComboBox<String> semesterPicker, DatePicker datePicker,
                                  TableView<AttendanceRecord> table, Label summary) {
        List<AttendanceRecord> filtered = new ArrayList<>(records);
        int semester = semesterPicker.getSelectionModel().getSelectedIndex();
        if (semester > 0) {
            filtered.removeIf(record -> courseDAO.getCoursesByTargetSemester(semester).stream()
                    .noneMatch(course -> course.getCourseId().equals(record.getCourseId())));
        }
        if (coursePicker.getValue() != null) {
            filtered.removeIf(record -> !record.getCourseId().equals(coursePicker.getValue().getCourseId()));
        }
        if (datePicker.getValue() != null) {
            filtered.removeIf(record -> !record.getAttendanceDate().equals(datePicker.getValue().toString()));
        }
        displayAttendance(filtered, table, summary);
    }

    private void displayAttendance(List<AttendanceRecord> records, TableView<AttendanceRecord> table, Label summary) {
        table.getItems().setAll(records);
        long present = records.stream().filter(record -> "Present".equals(record.getStatus())).count();
        summary.setText("Showing " + records.size() + " attendance record(s); Present: " + present
                + ", Absent: " + records.stream().filter(record -> "Absent".equals(record.getStatus())).count());
    }

    private ComboBox<String> semesterPicker(String prompt) {
        ComboBox<String> picker = new ComboBox<>();
        picker.getItems().addAll("Semester 1", "Semester 2", "Semester 3", "Semester 4",
                "Semester 5", "Semester 6", "Semester 7", "Semester 8");
        picker.setPromptText(prompt);
        picker.setPrefWidth(190);
        picker.getStyleClass().add("attendance-filter");
        return picker;
    }

    private ComboBox<Course> coursePicker(String prompt) {
        ComboBox<Course> picker = new ComboBox<>();
        picker.setPromptText(prompt);
        picker.setPrefWidth(260);
        picker.getStyleClass().add("attendance-filter");
        return picker;
    }

    private TableColumn<AttendanceRecord, String> courseColumn() {
        TableColumn<AttendanceRecord, String> column = new TableColumn<>("Course");
        column.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getCourseId() + " - " + d.getValue().getCourseName()));
        return column;
    }

    private TableColumn<AttendanceRecord, String> studentIdColumn() {
        TableColumn<AttendanceRecord, String> column = new TableColumn<>("Student ID");
        column.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStudentId()));
        return column;
    }

    private TableColumn<AttendanceRecord, String> studentColumn() {
        TableColumn<AttendanceRecord, String> column = new TableColumn<>("Student");
        column.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStudentName()));
        return column;
    }

    private TableColumn<AttendanceRecord, String> dateColumn() {
        TableColumn<AttendanceRecord, String> column = new TableColumn<>("Date");
        column.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getAttendanceDate()));
        return column;
    }

    private TableColumn<AttendanceRecord, String> statusDisplayColumn() {
        TableColumn<AttendanceRecord, String> column = new TableColumn<>("Attendance");
        column.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatus()));
        return column;
    }

    private TableColumn<AttendanceRecord, String> editableStatusColumn(
            Function<AttendanceRecord, String> getter, BiConsumer<AttendanceRecord, String> setter) {
        TableColumn<AttendanceRecord, String> column = new TableColumn<>("Attendance (select status)");
        column.setCellValueFactory(d -> new SimpleStringProperty(getter.apply(d.getValue())));
        column.setCellFactory(ignored -> new TableCell<>() {
            private final ComboBox<String> statusPicker = new ComboBox<>(
                    javafx.collections.FXCollections.observableArrayList(STATUSES));

            {
                statusPicker.setMaxWidth(Double.MAX_VALUE);
                statusPicker.setPrefWidth(150);
                statusPicker.getStyleClass().add("attendance-status-picker");
                statusPicker.setOnAction(e -> {
                    AttendanceRecord record = getTableRow() == null ? null : getTableRow().getItem();
                    if (record != null && statusPicker.getValue() != null) {
                        setter.accept(record, statusPicker.getValue());
                    }
                });
            }

            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    statusPicker.setValue(value);
                    setGraphic(statusPicker);
                }
            }
        });
        return column;
    }

    private void showMessage(Label label, String message, boolean success) {
        label.setText(message);
        label.setStyle(success ? "-fx-text-fill: #FDBA74;" : "-fx-text-fill: #F97316;");
    }
}
