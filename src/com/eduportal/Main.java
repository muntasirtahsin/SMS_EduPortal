package com.eduportal;

import com.eduportal.ui.MainApp;

import javax.swing.JOptionPane;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class Main {

    public static void main(String[] args) {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            logError("Uncaught Exception in Thread [" + thread.getName() + "]", throwable);
        });

        try {
            System.out.println("=================================================");
            System.out.println("   EduPortal Student Management System (JavaFX)  ");
            System.out.println("=================================================");

            MainApp.main(args);
        } catch (Throwable t) {
            logError("Fatal Application Error on Startup", t);
        }
    }

    public static void logError(String title, Throwable t) {
        t.printStackTrace();
        try {
            StringWriter sw = new StringWriter();
            t.printStackTrace(new PrintWriter(sw));
            String errDetails = sw.toString();

            Files.writeString(
                Path.of("error.log"),
                "[" + java.time.LocalDateTime.now() + "] " + title + ":\n" + errDetails + "\n\n",
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
            );

            JOptionPane.showMessageDialog(
                null,
                title + ":\n" + t.toString() + "\n\nPlease check error.log for details.",
                "EduPortal Error",
                JOptionPane.ERROR_MESSAGE
            );
        } catch (Exception ignored) {}
    }
}
