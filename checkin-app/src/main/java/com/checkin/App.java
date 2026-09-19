package com.checkin;

import com.checkin.services.AuthenticationService;
import com.checkin.services.DatasetInspectionService;
import com.checkin.utils.Navigation;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * Main application entry point for EmoSense.
 * Launches the primary stage, initializes core services and navigation,
 * presents the Sign In screen on startup, and asynchronously prints dataset
 * statistics to the console.
 */
public class App extends Application {

    private Navigation navigation;

    @Override
    public void start(Stage stage) {
        stage.setTitle("EmoSense");
        stage.setWidth(1280);
        stage.setHeight(800);
        stage.setMinWidth(1000);
        stage.setMinHeight(650);

        AuthenticationService authService = new AuthenticationService();
        this.navigation = new Navigation(stage, authService);

        // Print verified dataset statistics to console in background daemon thread
        Thread inspectionThread = new Thread(() -> {
            try {
                new DatasetInspectionService().printConsoleReport();
            } catch (Exception e) {
                System.err.println("Dataset inspection note: " + e.getMessage());
            }
        }, "dataset-inspection");
        inspectionThread.setDaemon(true);
        inspectionThread.start();

        // Application startup begins at Authentication (Sign In)
        navigation.showSignIn();

        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
