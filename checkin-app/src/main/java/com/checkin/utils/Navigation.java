package com.checkin.utils;

import com.checkin.model.AnalysisResult;
import com.checkin.model.CheckInData;
import com.checkin.screens.*;
import com.checkin.services.AuthenticationService;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Centralized navigation manager for EmoSense.
 * Handles transitions between screens on the primary stage.
 */
public class Navigation {

    private final Stage stage;
    private final AuthenticationService authService;

    public Navigation(Stage stage, AuthenticationService authService) {
        this.stage = stage;
        this.authService = authService;
    }

    public Stage getStage() {
        return stage;
    }

    public AuthenticationService getAuthService() {
        return authService;
    }

    public void setScene(Parent root) {
        if (stage == null) {
            return;
        }
        double width = stage.getWidth() > 0 ? stage.getWidth() : 1280;
        double height = stage.getHeight() > 0 ? stage.getHeight() : 800;
        Scene scene = new Scene(root, width, height);
        var cssUrl = getClass().getResource("/styles.css");
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }
        stage.setScene(scene);
    }

    public void showSignIn() {
        setScene(new SignInScreen(this));
    }

    public void showSignUp() {
        setScene(new SignUpScreen(this));
    }

    public void showDashboard() {
        setScene(new DashboardScreen(this));
    }

    public void showHistory() {
        setScene(new HistoryScreen(this));
    }

    public void showInsights() {
        setScene(new InsightsScreen(this));
    }

    public void showCheckIn() {
        setScene(new CheckInScreen(this));
    }

    public void showLoading(CheckInData data) {
        setScene(new LoadingScreen(data, this::showResults));
    }

    public void showCompanion() {
        showCompanion(null);
    }

    public void showCompanion(AnalysisResult contextResult) {
        setScene(new CompanionScreen(this, contextResult));
    }

    public void showResults(AnalysisResult result) {
        setScene(new ResultsScreen(
                result,
                this::showCheckIn,
                this::showHistory,
                () -> showCompanion(result),
                this::showDashboard
        ));
    }

    public void showSupportiveSuggestions(AnalysisResult result) {
        setScene(new SupportiveSuggestionsScreen(
                result,
                () -> showCompanion(result),
                this::showCheckIn,
                this::showDashboard
        ));
    }

    public void showPlaceholder(String title, String message) {
        javafx.scene.control.Label titleLabel = new javafx.scene.control.Label(title);
        titleLabel.getStyleClass().add("screen-title");

        javafx.scene.control.Label msgLabel = new javafx.scene.control.Label(message);
        msgLabel.getStyleClass().add("screen-subtitle");
        msgLabel.setWrapText(true);
        msgLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        javafx.scene.control.Button backBtn = new javafx.scene.control.Button("← Back to Dashboard");
        backBtn.getStyleClass().add("secondary-button");
        backBtn.setOnAction(e -> showDashboard());

        javafx.scene.layout.VBox box = new javafx.scene.layout.VBox(20, titleLabel, msgLabel, backBtn);
        box.setAlignment(javafx.geometry.Pos.CENTER);
        box.setPadding(new javafx.geometry.Insets(40));
        box.getStyleClass().add("screen-root");

        setScene(box);
    }

    public void showProfile() {
        showProfile(this::showDashboard);
    }

    public void showProfile(Runnable onBack) {
        setScene(new ProfileScreen(this, onBack != null ? onBack : this::showDashboard));
    }

    public void showSettings() {
        showSettings(this::showDashboard);
    }

    public void showSettings(Runnable onBack) {
        setScene(new SettingsScreen(this, onBack != null ? onBack : this::showDashboard));
    }

    public void showPrivacy() {
        showPrivacy(this::showDashboard);
    }

    public void showPrivacy(Runnable onBack) {
        setScene(new PrivacyScreen(this, onBack != null ? onBack : this::showDashboard));
    }

    public void showTechnology() {
        showTechnology(this::showDashboard);
    }

    public void showTechnology(Runnable onBack) {
        setScene(new TechnologyScreen(this, onBack != null ? onBack : this::showDashboard));
    }

    public void signOut() {
        authService.signOut();
        showSignIn();
    }
}
