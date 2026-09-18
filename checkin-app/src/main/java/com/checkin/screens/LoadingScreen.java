package com.checkin.screens;

import com.checkin.model.AnalysisEngine;
import com.checkin.model.AnalysisResult;
import com.checkin.model.CheckInData;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Loading screen shown while a check-in is analyzed. Walks through the steps
 * relevant to the actual inputs provided (photo-only, text-only, or the
 * full multimodal pipeline) and then hands the result back to the caller.
 */
public class LoadingScreen extends VBox {

    private final ProgressIndicator spinner = new ProgressIndicator();
    private final ProgressBar progressBar = new ProgressBar(0);
    private final VBox stepsBox = new VBox(10);

    public LoadingScreen(CheckInData data, Consumer<AnalysisResult> onComplete) {
        super(24);
        getStyleClass().add("screen-root");
        setAlignment(Pos.CENTER);
        setPadding(new Insets(40));

        Label title = new Label("Analyzing Your Check-In");
        title.getStyleClass().add("screen-title");

        spinner.setMaxSize(64, 64);
        progressBar.setPrefWidth(360);

        stepsBox.setAlignment(Pos.CENTER_LEFT);

        getChildren().addAll(title, spinner, progressBar, stepsBox);

        runPipeline(data, onComplete);
    }

    private void runPipeline(CheckInData data, Consumer<AnalysisResult> onComplete) {
        List<String> steps = buildStepList(data);

        Task<AnalysisResult> task = new Task<>() {
            @Override
            protected AnalysisResult call() throws Exception {
                for (int i = 0; i < steps.size(); i++) {
                    final int index = i;
                    Thread.sleep(550);
                    updateProgress(index + 1, steps.size());
                    Platform.runLater(() -> markStepDone(steps, index));
                }
                AnalysisResult result = new AnalysisEngine().analyze(data);

                // Transactional persistence to MySQL
                String userId = com.checkin.services.UserSession.getInstance().getCurrentUserId();
                if (userId != null) {
                    try {
                        new com.checkin.services.CheckInPersistenceService().persistCheckIn(data, result, userId);
                    } catch (Exception ex) {
                        System.err.println("[LoadingScreen] Note: Check-in persistence notification: " + ex.getMessage());
                    }
                }
                return result;
            }
        };

        progressBar.progressProperty().bind(task.progressProperty());

        // Pre-populate step labels (unchecked) before the task starts.
        for (String step : steps) {
            Label stepLabel = new Label("○ " + step);
            stepLabel.getStyleClass().add("loading-step");
            stepsBox.getChildren().add(stepLabel);
        }

        task.setOnSucceeded(e -> onComplete.accept(task.getValue()));
        task.setOnFailed(e -> onComplete.accept(new AnalysisResult(
                data.hasPhoto() ? "Unable to complete emotion analysis." : null, 0.0,
                data.hasThoughts() ? "Unable to complete emotion analysis." : null, 0.0,
                "Unable to complete emotion analysis.", 0.0
        )));

        Thread thread = new Thread(task, "analysis-pipeline");
        thread.setDaemon(true);
        thread.start();
    }

    private void markStepDone(List<String> steps, int index) {
        if (index < stepsBox.getChildren().size()) {
            Label label = (Label) stepsBox.getChildren().get(index);
            label.setText("✓ " + steps.get(index));
            label.getStyleClass().add("loading-step-done");
        }
    }

    private List<String> buildStepList(CheckInData data) {
        List<String> steps = new ArrayList<>();
        boolean hasPhoto = data.hasPhoto();
        boolean hasThoughts = data.hasThoughts();

        if (hasPhoto) {
            steps.add("Photo received");
        }
        if (hasThoughts) {
            steps.add("Thoughts received");
        }
        if (hasPhoto) {
            steps.add("Facial signal analyzed");
        }
        if (hasThoughts) {
            steps.add("Textual signal analyzed");
        }
        if (hasPhoto && hasThoughts) {
            steps.add("Signals combined");
        }
        steps.add("Generating insight...");
        return steps;
    }
}
