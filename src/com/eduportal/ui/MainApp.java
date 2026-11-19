package com.eduportal.ui;

import com.eduportal.Main;
import com.eduportal.utils.AutoUpdater;
import com.eduportal.utils.DatabaseInitializer;
import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

public class MainApp extends Application {

    private Stage primaryStage;
    private Scene mainScene;

    @Override
    public void start(Stage primaryStage) {
        try {
            this.primaryStage = primaryStage;
            this.primaryStage.setTitle("EduPortal Student Management System V"
                    + AutoUpdater.CURRENT_VERSION.replaceFirst("^[vV]", ""));
            URL icon = getClass().getResource("/eduportal.png");
            if (icon == null) {
                throw new IllegalStateException("Application icon resource /eduportal.png was not found.");
            }
            this.primaryStage.getIcons().add(new Image(icon.toExternalForm()));
            this.primaryStage.setResizable(true);

            // 1. Initialize Database Sche
            try {
                DatabaseInitializer.initializeDatabase();
            } catch (Exception e) {
                Main.logError("Database initialization warning", e);
            }

            // 2. Show Login View GUI
            showLoginView();

            // 4. Configure window dimensions & maximize state after showing stage
            try {
                Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
                primaryStage.setX(bounds.getMinX());
                primaryStage.setY(bounds.getMinY());
                primaryStage.setWidth(bounds.getWidth());
                primaryStage.setHeight(bounds.getHeight());
                primaryStage.setMinWidth(1024);
                primaryStage.setMinHeight(680);
                primaryStage.setMaximized(true);
            } catch (Exception ignored) {
            }

        } catch (Throwable t) {
            Main.logError("Error in MainApp.start", t);
        }
    }

    public void showLoginView() {
        LoginView loginView = new LoginView(this);
        if (mainScene == null) {
            mainScene = new Scene(loginView);
        } else {
            mainScene.setRoot(loginView);
        }
        applyTheme();
        primaryStage.setScene(mainScene);
        primaryStage.show();
        signalUpdatedStartupReady();
        AutoUpdater.checkForUpdatesAsync(loginView::setUpdateStatus);
    }

    private void signalUpdatedStartupReady() {
        String readyFile = System.getenv("EDUPORTAL_UPDATE_READY_FILE");
        if (readyFile == null || readyFile.isBlank()) return;
        try {
            Files.writeString(Path.of(readyFile), "ready");
        } catch (IOException e) {
            Main.logError("Could not confirm updated application startup", e);
        }
    }

    public void showMainLayout() {
        MainLayout layout = new MainLayout(this);
        if (mainScene == null) {
            mainScene = new Scene(layout);
        } else {
            mainScene.setRoot(layout);
        }
        applyTheme();
        primaryStage.setScene(mainScene);
        primaryStage.show();
    }

    public void applyTheme() {
        if (mainScene == null)
            return;

        mainScene.getStylesheets().clear();

        URL css = getClass().getResource("/style.css");

        if (css == null) {
            throw new RuntimeException("style.css not found inside JAR.");
        }

        mainScene.getStylesheets().add(css.toExternalForm());
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
