package com.eduportal.services;

import com.eduportal.models.Enrollment;
import com.eduportal.models.User;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

public class PDFReportService {

    public static File generateGradeReportPDF(User student, List<Enrollment> enrollments, String reportType, double cgpa) {
        try {
            File pdfDir = new File("reports");
            if (!pdfDir.exists()) pdfDir.mkdirs();

            String fileName = "GradeReport_" + student.getId() + "_" + System.currentTimeMillis() + ".html";
            File reportFile = new File(pdfDir, fileName);

            StringBuilder html = new StringBuilder();
            html.append("<!DOCTYPE html><html><head><meta charset='UTF-8'>");
            html.append("<title>Academic Grade Report - ").append(student.getName()).append("</title>");
            html.append("<style>");
            html.append("body { font-family: 'Segoe UI', Arial, sans-serif; margin: 40px; color: #FAFAFA; background: #18181B; }");
            html.append(".header { text-align: center; border-bottom: 3px solid #F97316; padding-bottom: 15px; margin-bottom: 30px; }");
            html.append(".header h1 { margin: 0; color: #FAFAFA; font-size: 28px; }");
            html.append(".header p { margin: 5px 0 0 0; color: #A1A1AA; font-size: 14px; }");
            html.append(".info-grid { display: flex; justify-content: space-between; margin-bottom: 30px; background: #27272A; padding: 18px; border-radius: 8px; border: 1px solid rgba(250, 250, 250, 0.12); }");
            html.append(".info-item { font-size: 14px; }");
            html.append(".info-item strong { color: #FAFAFA; }");
            html.append("table { width: 100%; border-collapse: collapse; margin-bottom: 30px; }");
            html.append("th { background: #27272A; color: #F97316; text-align: left; padding: 12px; font-size: 13px; }");
            html.append("td { padding: 12px; border-bottom: 1px solid rgba(250, 250, 250, 0.12); font-size: 13px; }");
            html.append("tr:nth-child(even) { background-color: #27272A; }");
            html.append(".summary { text-align: right; background: #27272A; padding: 15px 20px; border-radius: 8px; border-left: 4px solid #F97316; font-size: 16px; font-weight: bold; }");
            html.append(".footer { margin-top: 50px; text-align: center; font-size: 12px; color: #A1A1AA; border-top: 1px solid rgba(250, 250, 250, 0.12); padding-top: 15px; }");
            html.append("</style></head><body>");

            html.append("<div class='header'>");
            html.append("<h1>🎓 EDUPORTAL ACADEMIC SYSTEM</h1>");
            html.append("<p>Official Student Academic Performance & Grade Transcript</p>");
            html.append("</div>");

            html.append("<div class='info-grid'>");
            html.append("<div class='info-item'><strong>Student ID:</strong> ").append(student.getId()).append("<br><strong>Name:</strong> ").append(student.getName()).append("</div>");
            html.append("<div class='info-item'><strong>Current Semester:</strong> Semester ").append(student.getCurrentSemester()).append("<br><strong>Report Scope:</strong> ").append(reportType).append("</div>");
            html.append("</div>");

            html.append("<table>");
            html.append("<thead><tr><th>Course ID</th><th>Course Title</th><th>Credits</th><th>Total Marks</th><th>Letter Grade</th><th>Grade Point</th><th>Course Result</th></tr></thead>");
            html.append("<tbody>");

            for (Enrollment e : enrollments) {
                html.append("<tr>");
                html.append("<td><strong>").append(e.getCourseId()).append("</strong></td>");
                html.append("<td>").append(e.getCourseName()).append("</td>");
                html.append("<td>").append(e.getCredits()).append("</td>");
                html.append("<td>").append(String.format("%.1f", e.getTotalMarks())).append("</td>");
                html.append("<td><strong>").append(e.getLetterGrade()).append("</strong></td>");
                html.append("<td>").append(String.format("%.2f", e.getGradePoint())).append("</td>");
                html.append("<td>").append(e.getResultStatus()).append("</td>");
                html.append("</tr>");
            }

            html.append("</tbody></table>");

            html.append("<div class='summary'>");
            html.append("Cumulative Grade Point Average (CGPA): <span style='color: #F97316;'>").append(String.format("%.2f", cgpa)).append(" / 4.00</span>");
            html.append("</div>");
            if (!enrollments.isEmpty()) {
                List<String> semesterSummaries = enrollments.stream().map(Enrollment::getSemester).distinct()
                        .map(semester -> {
                            List<Enrollment> semesterResults = enrollments.stream()
                                    .filter(enrollment -> semester.equals(enrollment.getSemester()))
                                    .collect(Collectors.toList());
                            double average = semesterResults.stream().mapToDouble(Enrollment::getTotalMarks).sum()
                                    / semesterResults.size();
                            int credits = average >= 95 ? 6 : average >= 85 ? 3 : 0;
                            return semester + ": " + String.format("%.1f%%", average) + ", " + credits + " credits";
                        }).collect(Collectors.toList());
                int totalWaiverCredits = enrollments.stream().map(Enrollment::getSemester).distinct()
                        .mapToInt(semester -> {
                            List<Enrollment> semesterResults = enrollments.stream()
                                    .filter(enrollment -> semester.equals(enrollment.getSemester()))
                                    .collect(Collectors.toList());
                            double average = semesterResults.stream().mapToDouble(Enrollment::getTotalMarks).sum()
                                    / semesterResults.size();
                            return average >= 95 ? 6 : average >= 85 ? 3 : 0;
                        }).sum();
                html.append("<div class='summary'>Semester waiver credits earned: ").append(totalWaiverCredits)
                        .append(" total (").append(String.join("; ", semesterSummaries)).append(")</div>");
            }

            html.append("<div class='footer'>");
            html.append("Generated officially by EduPortal System on ").append(new java.util.Date().toString()).append(" • Valid without physical signature.");
            html.append("</div>");

            html.append("</body></html>");

            try (OutputStream os = new FileOutputStream(reportFile)) {
                os.write(html.toString().getBytes(StandardCharsets.UTF_8));
            }

            return reportFile;

        } catch (Exception e) {
            System.err.println("[PDFReportService] Error generating PDF report: " + e.getMessage());
            return null;
        }
    }
}
