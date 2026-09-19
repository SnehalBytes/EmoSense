package com.checkin.screens;

import com.checkin.Theme;
import com.checkin.model.AnalysisResult;
import com.checkin.model.CheckInRecord;
import com.checkin.model.User;
import com.checkin.services.CheckInPersistenceService;
import com.checkin.services.HistoryService;
import com.checkin.services.InsightsService;
import com.checkin.services.UserSession;
import com.checkin.utils.Navigation;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Dashboard screen: The central home screen of EmoSense.
 * Displays the primary Check-In action, dynamic Latest Check-In card,
 * quick action navigation, compact Insights preview, and enforces strict user isolation.
 */
public class DashboardScreen extends BorderPane {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMM dd, yyyy  hh:mm a");

    private final Navigation navigation;
    private final HistoryService historyService;
    private final InsightsService insightsService;

    private final VBox mainContentArea = new VBox(24);

    public DashboardScreen(Navigation navigation) {
        this(navigation, new HistoryService(), new InsightsService());
    }

    public DashboardScreen(Navigation navigation, HistoryService historyService, InsightsService insightsService) {
        this.navigation = navigation;
        this.historyService = historyService != null ? historyService : new HistoryService();
        this.insightsService = insightsService != null ? insightsService : new InsightsService(this.historyService);

        getStyleClass().add("screen-root");

        setTop(buildNavbar());
        setCenter(buildScrollContainer());

        loadDashboardData();
    }

    private HBox buildNavbar() {
        HBox navbar = new HBox(20);
        navbar.setAlignment(Pos.CENTER_LEFT);
        navbar.setPadding(new Insets(18, 36, 18, 36));
        navbar.setStyle(
                "-fx-background-color: " + Theme.COLOR_CARD_BG + ";" +
                "-fx-border-color: " + Theme.COLOR_CARD_BORDER + ";" +
                "-fx-border-width: 0 0 1 0;"
        );

        Label logo = new Label("EMOSENSE");
        logo.setStyle(
                "-fx-font-size: 20px;" +
                "-fx-font-weight: 800;" +
                "-fx-text-fill: #f1f3f9;" +
                "-fx-letter-spacing: 2px;"
        );

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        User currentUser = UserSession.getInstance().getCurrentUser();
        if (currentUser == null && navigation != null && navigation.getAuthService() != null) {
            currentUser = navigation.getAuthService().getCurrentUser();
        }

        String displayName = (currentUser != null && currentUser.getFullName() != null && !currentUser.getFullName().isBlank())
                ? currentUser.getFullName()
                : (currentUser != null && currentUser.getEmail() != null) ? currentUser.getEmail() : "Friend";

        Label userGreeting = new Label("Welcome back, " + displayName);
        userGreeting.setStyle(
                "-fx-font-size: 13px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: " + Theme.COLOR_TEXT_PRIMARY + ";"
        );

        Button profileBtn = new Button("Profile");
        Theme.applySecondaryButton(profileBtn);
        profileBtn.setOnAction(e -> {
            if (navigation != null) navigation.showProfile();
        });

        Button settingsBtn = new Button("Settings");
        Theme.applySecondaryButton(settingsBtn);
        settingsBtn.setOnAction(e -> {
            if (navigation != null) navigation.showSettings();
        });

        Button privacyBtn = new Button("Privacy");
        Theme.applySecondaryButton(privacyBtn);
        privacyBtn.setOnAction(e -> {
            if (navigation != null) navigation.showPrivacy();
        });

        Button aboutBtn = new Button("About");
        Theme.applySecondaryButton(aboutBtn);
        aboutBtn.setOnAction(e -> {
            if (navigation != null) navigation.showTechnology();
        });

        Button signOutBtn = new Button("Sign Out");
        Theme.applySecondaryButton(signOutBtn);
        signOutBtn.setOnAction(e -> {
            if (navigation != null) navigation.signOut();
        });

        navbar.getChildren().addAll(logo, spacer, userGreeting, profileBtn, settingsBtn, privacyBtn, aboutBtn, signOutBtn);
        return navbar;
    }

    private ScrollPane buildScrollContainer() {
        mainContentArea.setPadding(new Insets(28, 40, 36, 40));
        mainContentArea.setAlignment(Pos.TOP_CENTER);
        mainContentArea.setMaxWidth(960);

        VBox outer = new VBox(mainContentArea);
        outer.setAlignment(Pos.TOP_CENTER);

        ScrollPane scrollPane = new ScrollPane(outer);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("edge-to-edge");
        return scrollPane;
    }

    /**
     * Loads the latest user data and populates the dashboard dynamically.
     */
    public void loadDashboardData() {
        mainContentArea.getChildren().clear();

        // 1. Header welcome block
        VBox headerBlock = buildDashboardHeader();
        mainContentArea.getChildren().add(headerBlock);

        // 2. Primary Action Hero Card
        VBox heroCard = buildPrimaryActionCard();
        mainContentArea.getChildren().add(heroCard);

        // 3. Dynamic Latest Check-In Section
        String currentUserId = UserSession.getInstance().getCurrentUserId();
        if (currentUserId == null || currentUserId.isBlank()) {
            mainContentArea.getChildren().add(buildNoCheckInCard());
            mainContentArea.getChildren().add(buildQuickActionsGrid());
            return;
        }

        try {
            List<CheckInRecord> records = historyService.getUserHistory(currentUserId);
            if (records == null || records.isEmpty()) {
                mainContentArea.getChildren().add(buildNoCheckInCard());
            } else {
                CheckInRecord latest = records.get(0); // Sorted newest first
                mainContentArea.getChildren().add(buildLatestCheckInCard(latest));
            }

            // 4. Quick Actions Grid
            mainContentArea.getChildren().add(buildQuickActionsGrid());

            // 5. Compact Insights Preview
            InsightsService.UserInsights insights = insightsService.computeUserInsights(currentUserId);
            mainContentArea.getChildren().add(buildInsightsPreviewCard(insights));

        } catch (Exception e) {
            System.err.println("[DashboardScreen] Failed loading dashboard data: " + e.getMessage());
            mainContentArea.getChildren().add(buildErrorState());
            mainContentArea.getChildren().add(buildQuickActionsGrid());
        }
    }

    private VBox buildDashboardHeader() {
        VBox box = new VBox(4);
        box.setAlignment(Pos.CENTER_LEFT);

        User currentUser = UserSession.getInstance().getCurrentUser();
        String displayName = (currentUser != null && currentUser.getFullName() != null && !currentUser.getFullName().isBlank())
                ? currentUser.getFullName() : null;

        Label welcomeTitle = new Label(displayName != null ? "Welcome back, " + displayName : "Welcome back");
        welcomeTitle.setStyle("-fx-font-size: 24px; -fx-font-weight: 800; -fx-text-fill: #f1f3f9;");

        Label subtitle = new Label("Understand your emotional signals through reflection.");
        subtitle.setStyle("-fx-font-size: 13.5px; -fx-text-fill: " + Theme.COLOR_TEXT_SECONDARY + ";");

        box.getChildren().addAll(welcomeTitle, subtitle);
        return box;
    }

    private VBox buildPrimaryActionCard() {
        VBox card = new VBox(14);
        card.setPadding(new Insets(26, 28, 26, 28));
        card.setAlignment(Pos.CENTER_LEFT);
        Theme.applyCardStyle(card);
        card.setStyle(
                card.getStyle() +
                "-fx-border-color: " + Theme.COLOR_ACCENT + ";" +
                "-fx-border-width: 1.5;" +
                "-fx-background-color: linear-gradient(to right, #1b2038, #181d32);"
        );

        HBox topRow = new HBox(10);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label badge = new Label("PRIMARY ACTION");
        badge.setStyle(
                "-fx-background-color: rgba(99, 102, 241, 0.2);" +
                "-fx-text-fill: #a5b4fc;" +
                "-fx-font-size: 10px;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 3 8;" +
                "-fx-background-radius: 4;"
        );

        Label cardTitle = new Label("Start a New Check-In");
        cardTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #f1f3f9;");

        topRow.getChildren().addAll(badge, cardTitle);

        Label supportingText = new Label("Share a photo, your thoughts, or both.");
        supportingText.setStyle("-fx-font-size: 13.5px; -fx-text-fill: " + Theme.COLOR_TEXT_SECONDARY + ";");

        Button startBtn = new Button("Start Check-In  →");
        Theme.applyPrimaryButton(startBtn);
        startBtn.setStyle(
                startBtn.getStyle() +
                "-fx-font-size: 14px;" +
                "-fx-padding: 10 24;" +
                "-fx-font-weight: bold;"
        );
        startBtn.setOnAction(e -> {
            if (navigation != null) navigation.showCheckIn();
        });

        card.getChildren().addAll(topRow, supportingText, startBtn);
        return card;
    }

    private VBox buildLatestCheckInCard(CheckInRecord record) {
        VBox card = new VBox(14);
        card.setPadding(new Insets(22, 26, 22, 26));
        Theme.applyCardStyle(card);

        // Header row
        HBox headerRow = new HBox(10);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        Label sectionLabel = new Label("LATEST CHECK-IN");
        sectionLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #818cf8; -fx-letter-spacing: 0.5px;");

        String formattedDate = record.createdAt() != null ? record.createdAt().format(DATE_FMT) : "Recent";
        Label dateLabel = new Label("•  " + formattedDate);
        dateLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: " + Theme.COLOR_TEXT_SECONDARY + ";");

        String mode = CheckInPersistenceService.determineMode(record);
        Label modeBadge = new Label(mode);
        modeBadge.setStyle(
                "-fx-background-color: rgba(99, 102, 241, 0.15);" +
                "-fx-text-fill: #a5b4fc;" +
                "-fx-font-size: 10px;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 2 7;" +
                "-fx-background-radius: 4;"
        );

        Label modelBadge = new Label("REAL MODEL");
        modelBadge.setStyle(
                "-fx-background-color: rgba(34, 197, 94, 0.15);" +
                "-fx-text-fill: #4ade80;" +
                "-fx-font-size: 10px;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 2 7;" +
                "-fx-background-radius: 4;"
        );

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button viewResultBtn = new Button("View Result");
        Theme.applySecondaryButton(viewResultBtn);
        viewResultBtn.setStyle(
                viewResultBtn.getStyle() +
                "-fx-font-size: 12px;" +
                "-fx-font-weight: bold;" +
                "-fx-border-color: #635bff;" +
                "-fx-text-fill: #c7d2fe;" +
                "-fx-padding: 6 14;" +
                "-fx-background-radius: 6;"
        );
        viewResultBtn.setOnAction(e -> handleViewResult(record));

        headerRow.getChildren().addAll(sectionLabel, dateLabel, modeBadge, modelBadge, spacer, viewResultBtn);

        // Signals container
        VBox signalsBox = new VBox(8);

        // Combined Multimodal Insight
        if (record.hasPhoto() && record.hasThoughts() && record.combinedLabel() != null) {
            HBox combinedRow = new HBox(10);
            combinedRow.setAlignment(Pos.CENTER_LEFT);
            combinedRow.setStyle(
                    "-fx-background-color: rgba(99, 91, 255, 0.08);" +
                    "-fx-padding: 8 12;" +
                    "-fx-background-radius: 6;" +
                    "-fx-border-color: rgba(99, 91, 255, 0.25);" +
                    "-fx-border-radius: 6;"
            );

            Label tag = new Label("Multimodal Insight:");
            tag.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #a5b4fc;");

            Label val = new Label(record.combinedLabel());
            val.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #f1f3f9;");

            Region cSpacer = new Region();
            HBox.setHgrow(cSpacer, Priority.ALWAYS);

            Label conf = new Label(String.format("%.0f%% confidence", record.combinedConfidence()));
            conf.setStyle("-fx-font-size: 11.5px; -fx-text-fill: " + Theme.COLOR_TEXT_SECONDARY + ";");

            combinedRow.getChildren().addAll(tag, val, cSpacer, conf);
            signalsBox.getChildren().add(combinedRow);
        }

        // Modality detail columns
        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(6);

        // Facial Signal
        VBox facialBox = new VBox(4);
        if (record.hasPhoto() && record.facialLabel() != null && !record.facialLabel().isBlank()) {
            Label fTitle = new Label("Facial Expression:");
            fTitle.setStyle("-fx-font-size: 11px; -fx-text-fill: " + Theme.COLOR_TEXT_SECONDARY + ";");

            Label fVal = new Label(record.facialLabel() + (record.facialConfidence() > 0 ? String.format(" (%.0f%% confidence)", record.facialConfidence()) : ""));
            fVal.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #f1f3f9;");
            facialBox.getChildren().addAll(fTitle, fVal);
        } else {
            Label fNotice = new Label("Facial signal not provided.");
            fNotice.setStyle("-fx-font-size: 12px; -fx-font-style: italic; -fx-text-fill: #64748b;");
            facialBox.getChildren().add(fNotice);
        }

        // Textual Signal
        VBox textBox = new VBox(4);
        if (record.hasThoughts() && record.textLabel() != null && !record.textLabel().isBlank()) {
            Label tTitle = new Label("Textual Emotional Cue:");
            tTitle.setStyle("-fx-font-size: 11px; -fx-text-fill: " + Theme.COLOR_TEXT_SECONDARY + ";");

            Label tVal = new Label(record.textLabel() + (record.textConfidence() > 0 ? String.format(" (%.0f%% confidence)", record.textConfidence()) : ""));
            tVal.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #f1f3f9;");
            textBox.getChildren().addAll(tTitle, tVal);
        } else {
            Label tNotice = new Label("Text signal not provided.");
            tNotice.setStyle("-fx-font-size: 12px; -fx-font-style: italic; -fx-text-fill: #64748b;");
            textBox.getChildren().add(tNotice);
        }

        grid.add(facialBox, 0, 0);
        grid.add(textBox, 1, 0);
        signalsBox.getChildren().add(grid);

        // Thoughts excerpt snippet if available
        if (record.hasThoughts() && record.thoughtsText() != null && !record.thoughtsText().isBlank()) {
            Label snippet = new Label("“" + truncate(record.thoughtsText(), 110) + "”");
            snippet.setStyle("-fx-font-size: 12px; -fx-font-style: italic; -fx-text-fill: #94a3b8;");
            snippet.setWrapText(true);
            signalsBox.getChildren().add(snippet);
        }

        card.getChildren().addAll(headerRow, signalsBox);
        return card;
    }

    private VBox buildNoCheckInCard() {
        VBox card = new VBox(12);
        card.setPadding(new Insets(24, 28, 24, 28));
        card.setAlignment(Pos.CENTER);
        Theme.applyCardStyle(card);

        Label title = new Label("No check-ins yet");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #f1f3f9;");

        Label subtitle = new Label("Complete your first check-in to start discovering your emotional signals.");
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: " + Theme.COLOR_TEXT_SECONDARY + ";");
        subtitle.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        Button startFirstBtn = new Button("Start Your First Check-In");
        Theme.applyPrimaryButton(startFirstBtn);
        startFirstBtn.setStyle(
                startFirstBtn.getStyle() +
                "-fx-font-size: 13px;" +
                "-fx-padding: 8 20;" +
                "-fx-font-weight: bold;"
        );
        startFirstBtn.setOnAction(e -> {
            if (navigation != null) navigation.showCheckIn();
        });

        card.getChildren().addAll(title, subtitle, startFirstBtn);
        return card;
    }

    private VBox buildQuickActionsGrid() {
        VBox section = new VBox(12);
        section.setAlignment(Pos.TOP_LEFT);

        Label title = new Label("Quick Actions");
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #f1f3f9;");

        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(16);

        grid.add(buildActionCard("NEW CHECK-IN", "Reflect on how you're feeling", () -> {
            if (navigation != null) navigation.showCheckIn();
        }), 0, 0);

        grid.add(buildActionCard("HISTORY", "Review previous check-ins", () -> {
            if (navigation != null) navigation.showHistory();
        }), 1, 0);

        grid.add(buildActionCard("AI COMPANION", "Talk through what's on your mind", () -> {
            if (navigation != null) navigation.showCompanion();
        }), 0, 1);

        grid.add(buildActionCard("INSIGHTS", "Explore your emotional patterns", () -> {
            if (navigation != null) navigation.showInsights();
        }), 1, 1);

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(50);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(50);
        grid.getColumnConstraints().addAll(col1, col2);

        section.getChildren().addAll(title, grid);
        return section;
    }

    private VBox buildActionCard(String headerText, String descriptionText, Runnable onAction) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(18));
        card.setAlignment(Pos.TOP_LEFT);
        Theme.applyInteractiveCardStyle(card);

        Label header = new Label(headerText);
        header.setStyle("-fx-font-size: 13px; -fx-font-weight: 800; -fx-text-fill: #818cf8; -fx-letter-spacing: 0.5px;");

        Label desc = new Label(descriptionText);
        desc.setStyle("-fx-font-size: 12.5px; -fx-text-fill: " + Theme.COLOR_TEXT_SECONDARY + ";");
        desc.setWrapText(true);

        Button openBtn = new Button("Open  →");
        Theme.applySecondaryButton(openBtn);
        openBtn.setStyle(
                openBtn.getStyle() +
                "-fx-font-size: 11.5px;" +
                "-fx-padding: 5 12;"
        );
        openBtn.setOnAction(e -> onAction.run());

        card.getChildren().addAll(header, desc, openBtn);
        return card;
    }

    private VBox buildInsightsPreviewCard(InsightsService.UserInsights insights) {
        VBox card = new VBox(12);
        card.setPadding(new Insets(20, 24, 20, 24));
        Theme.applyCardStyle(card);

        HBox topRow = new HBox(10);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Your Insights");
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #f1f3f9;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button viewInsightsBtn = new Button("Explore Insights  →");
        Theme.applySecondaryButton(viewInsightsBtn);
        viewInsightsBtn.setStyle(
                viewInsightsBtn.getStyle() +
                "-fx-font-size: 12px;" +
                "-fx-padding: 5 12;"
        );
        viewInsightsBtn.setOnAction(e -> {
            if (navigation != null) navigation.showInsights();
        });

        topRow.getChildren().addAll(title, spacer, viewInsightsBtn);

        if (!insights.hasSufficientData()) {
            VBox noticeBox = new VBox(6);
            Label notice = new Label("More check-ins will help build your personal insights.");
            notice.setStyle("-fx-font-size: 13px; -fx-font-style: italic; -fx-text-fill: " + Theme.COLOR_TEXT_SECONDARY + ";");

            String checkInCountText = insights.totalCheckIns() == 1
                    ? "1 completed check-in recorded so far."
                    : (insights.totalCheckIns() == 2 ? "2 completed check-ins recorded so far." : "No check-ins completed yet.");
            Label countLbl = new Label(checkInCountText);
            countLbl.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #818cf8;");

            noticeBox.getChildren().addAll(notice, countLbl);
            card.getChildren().addAll(topRow, noticeBox);
        } else {
            HBox statsRow = new HBox(16);
            statsRow.setAlignment(Pos.CENTER);

            VBox stat1 = buildMiniStat("Total Check-Ins", String.valueOf(insights.totalCheckIns()));
            VBox stat2 = buildMiniStat("Top Facial Signal", insights.mostFrequentFacial() != null ? insights.mostFrequentFacial() : "N/A");
            VBox stat3 = buildMiniStat("Top Textual Cue", insights.mostFrequentText() != null ? insights.mostFrequentText() : "N/A");

            HBox.setHgrow(stat1, Priority.ALWAYS);
            HBox.setHgrow(stat2, Priority.ALWAYS);
            HBox.setHgrow(stat3, Priority.ALWAYS);

            statsRow.getChildren().addAll(stat1, stat2, stat3);
            card.getChildren().addAll(topRow, statsRow);
        }

        return card;
    }

    private VBox buildMiniStat(String label, String value) {
        VBox b = new VBox(4);
        b.setAlignment(Pos.CENTER_LEFT);
        b.setPadding(new Insets(10, 14, 10, 14));
        b.setStyle(
                "-fx-background-color: rgba(255, 255, 255, 0.03);" +
                "-fx-border-color: #242b45;" +
                "-fx-border-radius: 6;" +
                "-fx-background-radius: 6;"
        );

        Label l = new Label(label);
        l.setStyle("-fx-font-size: 11px; -fx-text-fill: " + Theme.COLOR_TEXT_SECONDARY + ";");

        Label v = new Label(value);
        v.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #f1f3f9;");

        b.getChildren().addAll(l, v);
        return b;
    }

    private VBox buildErrorState() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(24));
        box.setAlignment(Pos.CENTER);
        Theme.applyCardStyle(box);

        Label errorTitle = new Label("Unable to load your latest check-in.");
        errorTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #f87171;");

        Label errorDesc = new Label("A database error occurred while accessing your stored check-ins.");
        errorDesc.setStyle("-fx-font-size: 12.5px; -fx-text-fill: " + Theme.COLOR_TEXT_SECONDARY + ";");

        Button retryBtn = new Button("Retry");
        Theme.applySecondaryButton(retryBtn);
        retryBtn.setOnAction(e -> loadDashboardData());

        box.getChildren().addAll(errorTitle, errorDesc, retryBtn);
        return box;
    }

    private void handleViewResult(CheckInRecord record) {
        Optional<AnalysisResult> resultOpt = historyService.getStoredResult(record.id());
        AnalysisResult displayResult;
        if (resultOpt.isPresent()) {
            displayResult = resultOpt.get();
        } else {
            displayResult = new AnalysisResult(
                    record.facialLabel(),
                    record.facialConfidence(),
                    record.textLabel(),
                    record.textConfidence(),
                    record.combinedLabel(),
                    record.combinedConfidence()
            );
        }

        if (navigation != null) {
            navigation.showResults(displayResult);
        }
    }

    private String truncate(String text, int max) {
        if (text == null) return "";
        return text.length() <= max ? text : text.substring(0, max) + "...";
    }

    public HistoryService getHistoryService() {
        return historyService;
    }

    public InsightsService getInsightsService() {
        return insightsService;
    }

    public VBox getMainContentArea() {
        return mainContentArea;
    }
}
