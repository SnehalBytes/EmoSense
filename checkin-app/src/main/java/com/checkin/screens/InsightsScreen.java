package com.checkin.screens;

import com.checkin.Theme;
import com.checkin.model.AnalysisResult;
import com.checkin.model.CheckInRecord;
import com.checkin.services.CheckInPersistenceService;
import com.checkin.services.HistoryService;
import com.checkin.services.InsightsService;
import com.checkin.services.UserSession;
import com.checkin.utils.Navigation;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Insights screen displaying genuine reflection patterns and signal distributions
 * computed strictly from stored emotional check-ins for the authenticated user.
 * Explicitly non-clinical and strictly avoids fabricated statistics.
 */
public class InsightsScreen extends BorderPane {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMM dd, yyyy  hh:mm a");

    private final Navigation navigation;
    private final InsightsService insightsService;
    private final HistoryService historyService;

    private final VBox contentArea = new VBox(22);

    public InsightsScreen(Navigation navigation) {
        this(navigation, new InsightsService(), new HistoryService());
    }

    public InsightsScreen(Navigation navigation, InsightsService insightsService) {
        this(navigation, insightsService, insightsService != null ? insightsService.getHistoryService() : new HistoryService());
    }

    public InsightsScreen(Navigation navigation, InsightsService insightsService, HistoryService historyService) {
        this.navigation = navigation;
        this.insightsService = insightsService != null ? insightsService : new InsightsService();
        this.historyService = historyService != null ? historyService : new HistoryService();

        getStyleClass().add("screen-root");

        setTop(buildHeader());
        setCenter(buildScrollContainer());

        loadInsights();
    }

    private HBox buildHeader() {
        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(18, 36, 18, 36));
        header.setStyle(
                "-fx-background-color: " + Theme.COLOR_CARD_BG + ";" +
                "-fx-border-color: " + Theme.COLOR_CARD_BORDER + ";" +
                "-fx-border-width: 0 0 1 0;"
        );

        Button backBtn = new Button("← Dashboard");
        Theme.applySecondaryButton(backBtn);
        backBtn.setOnAction(e -> {
            if (navigation != null) navigation.showDashboard();
        });

        VBox titleBox = new VBox(2);
        Label title = new Label("Your Insights");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: 800; -fx-text-fill: #f1f3f9;");

        Label subtitle = new Label("Patterns from your saved emotional check-ins.");
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: " + Theme.COLOR_TEXT_SECONDARY + ";");
        titleBox.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button refreshBtn = new Button("Refresh");
        Theme.applySecondaryButton(refreshBtn);
        refreshBtn.setStyle(
                refreshBtn.getStyle() +
                "-fx-font-size: 12px;" +
                "-fx-padding: 6 14;"
        );
        refreshBtn.setOnAction(e -> loadInsights());

        header.getChildren().addAll(backBtn, titleBox, spacer, refreshBtn);
        return header;
    }

    private ScrollPane buildScrollContainer() {
        contentArea.setPadding(new Insets(26, 36, 36, 36));
        contentArea.setAlignment(Pos.TOP_CENTER);
        contentArea.setMaxWidth(900);

        VBox outer = new VBox(contentArea);
        outer.setAlignment(Pos.TOP_CENTER);

        ScrollPane scrollPane = new ScrollPane(outer);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("edge-to-edge");
        return scrollPane;
    }

    public void loadInsights() {
        contentArea.getChildren().clear();

        // 1. Loading State
        VBox loadingBox = buildLoadingState();
        contentArea.getChildren().add(loadingBox);

        String currentUserId = UserSession.getInstance().getCurrentUserId();
        if (currentUserId == null || currentUserId.isBlank()) {
            contentArea.getChildren().clear();
            contentArea.getChildren().add(buildEmptyState());
            return;
        }

        try {
            InsightsService.UserInsights insights = insightsService.computeUserInsights(currentUserId);
            contentArea.getChildren().clear();

            if (insights.totalCheckIns() == 0) {
                contentArea.getChildren().add(buildEmptyState());
                return;
            }

            // Section A: Check-In Activity KPI Summary
            contentArea.getChildren().add(buildKpiRow(insights));

            // Insufficient data notice if < 3 check-ins
            if (!insights.hasSufficientData()) {
                contentArea.getChildren().add(buildInsufficientDataCard(insights));
            } else {
                // Section B: Facial Signal Distribution Chart
                contentArea.getChildren().add(buildFacialDistributionCard(insights));

                // Section C: Textual Signal Distribution
                contentArea.getChildren().add(buildTextDistributionCard(insights));

                // Section E: Facial vs Textual Signals (Multimodal Alignment)
                contentArea.getChildren().add(buildAlignmentCard(insights));
            }

            // Section D: Recent Signals Chronological List
            contentArea.getChildren().add(buildRecentSignalsSection(insights));

            // Academic Non-Medical Disclaimer
            contentArea.getChildren().add(buildDisclaimerCard());

        } catch (Exception e) {
            System.err.println("[InsightsScreen] Error loading insights: " + e.getMessage());
            contentArea.getChildren().clear();
            contentArea.getChildren().add(buildErrorState());
        }
    }

    private VBox buildLoadingState() {
        VBox box = new VBox(14);
        box.setPadding(new Insets(40));
        box.setAlignment(Pos.CENTER);
        Theme.applyCardStyle(box);

        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setMaxSize(48, 48);

        Label loadingMsg = new Label("Loading your insights...");
        loadingMsg.setStyle("-fx-font-size: 14px; -fx-text-fill: " + Theme.COLOR_TEXT_PRIMARY + ";");

        box.getChildren().addAll(spinner, loadingMsg);
        return box;
    }

    private VBox buildErrorState() {
        VBox box = new VBox(14);
        box.setPadding(new Insets(40));
        box.setAlignment(Pos.CENTER);
        Theme.applyCardStyle(box);

        Label errorTitle = new Label("Unable to load your insights right now.");
        errorTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #f87171;");

        Label errorMsg = new Label("A database error occurred while calculating your insights. Please try again.");
        errorMsg.setStyle("-fx-font-size: 13px; -fx-text-fill: " + Theme.COLOR_TEXT_SECONDARY + ";");

        Button retryBtn = new Button("Retry");
        Theme.applySecondaryButton(retryBtn);
        retryBtn.setOnAction(e -> loadInsights());

        box.getChildren().addAll(errorTitle, errorMsg, retryBtn);
        return box;
    }

    private VBox buildEmptyState() {
        VBox box = new VBox(16);
        box.setPadding(new Insets(48, 36, 48, 36));
        box.setAlignment(Pos.CENTER);
        Theme.applyCardStyle(box);

        Label emptyTitle = new Label("No check-ins yet");
        emptyTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #f1f3f9;");

        Label emptyMsg = new Label("Complete your first check-in to start discovering your emotional patterns.");
        emptyMsg.setStyle("-fx-font-size: 13px; -fx-text-fill: " + Theme.COLOR_TEXT_SECONDARY + ";");
        emptyMsg.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        Button startBtn = new Button("Start Your First Check-In");
        Theme.applyPrimaryButton(startBtn);
        startBtn.setStyle(
                startBtn.getStyle() +
                "-fx-font-size: 13.5px;" +
                "-fx-padding: 10 24;" +
                "-fx-font-weight: bold;"
        );
        startBtn.setOnAction(e -> {
            if (navigation != null) navigation.showCheckIn();
        });

        box.getChildren().addAll(emptyTitle, emptyMsg, startBtn);
        return box;
    }

    private VBox buildInsufficientDataCard(InsightsService.UserInsights insights) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(20, 24, 20, 24));
        Theme.applyCardStyle(card);
        card.setStyle(
                card.getStyle() +
                "-fx-border-color: rgba(99, 102, 241, 0.35);" +
                "-fx-border-width: 1;"
        );

        Label title = new Label("More check-ins will help build your personal insights.");
        title.setStyle("-fx-font-size: 14.5px; -fx-font-weight: bold; -fx-text-fill: #a5b4fc;");

        String desc = String.format(
                "You have %d completed check-in%s recorded. A minimum of 3 check-ins is recommended " +
                "to observe meaningful distributions across facial and textual expressions. Continue reflecting to unlock full analytics.",
                insights.totalCheckIns(),
                insights.totalCheckIns() == 1 ? "" : "s"
        );
        Label descLabel = new Label(desc);
        descLabel.setStyle("-fx-font-size: 12.5px; -fx-text-fill: " + Theme.COLOR_TEXT_SECONDARY + ";");
        descLabel.setWrapText(true);

        Button newCheckInBtn = new Button("Start a New Check-In  →");
        Theme.applyPrimaryButton(newCheckInBtn);
        newCheckInBtn.setStyle(
                newCheckInBtn.getStyle() +
                "-fx-font-size: 12px;" +
                "-fx-padding: 6 16;"
        );
        newCheckInBtn.setOnAction(e -> {
            if (navigation != null) navigation.showCheckIn();
        });

        card.getChildren().addAll(title, descLabel, newCheckInBtn);
        return card;
    }

    private HBox buildKpiRow(InsightsService.UserInsights insights) {
        HBox row = new HBox(14);
        row.setAlignment(Pos.CENTER);

        VBox totalCard = createKpiCard("CHECK-IN ACTIVITY", String.valueOf(insights.totalCheckIns()), "Completed entries");
        VBox modeCard = createKpiCard("INPUT BREAKDOWN",
                insights.multimodalCount() + " Multi · " + insights.photoCount() + " Photo · " + insights.textCount() + " Text",
                "By modality type");
        VBox confCard = createKpiCard("AVG. SIGNAL CONFIDENCE",
                String.format("%.0f%%", insights.averageConfidence()),
                "Mean model confidence");

        HBox.setHgrow(totalCard, Priority.ALWAYS);
        HBox.setHgrow(modeCard, Priority.ALWAYS);
        HBox.setHgrow(confCard, Priority.ALWAYS);

        row.getChildren().addAll(totalCard, modeCard, confCard);
        return row;
    }

    private VBox createKpiCard(String header, String value, String subtitle) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(18, 20, 18, 20));
        Theme.applyCardStyle(card);
        card.setAlignment(Pos.CENTER_LEFT);

        Label h = new Label(header);
        h.setStyle("-fx-font-size: 10.5px; -fx-font-weight: 800; -fx-text-fill: #818cf8; -fx-letter-spacing: 0.5px;");

        Label v = new Label(value);
        v.setStyle("-fx-font-size: 19px; -fx-font-weight: 800; -fx-text-fill: #f1f3f9;");

        Label s = new Label(subtitle);
        s.setStyle("-fx-font-size: 11.5px; -fx-text-fill: " + Theme.COLOR_TEXT_SECONDARY + ";");

        card.getChildren().addAll(h, v, s);
        return card;
    }

    private VBox buildFacialDistributionCard(InsightsService.UserInsights insights) {
        VBox card = new VBox(14);
        card.setPadding(new Insets(22));
        Theme.applyCardStyle(card);
        card.setAlignment(Pos.TOP_LEFT);

        Label title = new Label("Facial Signal Distribution");
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #f1f3f9;");

        Label subtitle = new Label("Frequency across 7 FER2013 facial expression classes from your stored photo check-ins.");
        subtitle.setStyle("-fx-font-size: 12px; -fx-text-fill: " + Theme.COLOR_TEXT_SECONDARY + ";");

        card.getChildren().addAll(title, subtitle);

        boolean hasAnyFacial = insights.facialDistribution().values().stream().anyMatch(v -> v > 0);
        if (!hasAnyFacial) {
            Label emptyLbl = new Label("No facial signals recorded yet. Photo check-ins will populate this chart.");
            emptyLbl.setStyle("-fx-font-size: 12.5px; -fx-font-style: italic; -fx-text-fill: #64748b;");
            card.getChildren().add(emptyLbl);
            return card;
        }

        // Native JavaFX BarChart
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Facial Signal");
        xAxis.setTickLabelFill(Color.web("#94a3b8"));

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Count");
        yAxis.setTickUnit(1);
        yAxis.setMinorTickVisible(false);
        yAxis.setTickLabelFill(Color.web("#94a3b8"));

        BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
        barChart.setLegendVisible(false);
        barChart.setAnimated(false);
        barChart.setPrefHeight(260);
        barChart.setStyle("-fx-background-color: transparent;");

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (String c : InsightsService.FER2013_CLASSES) {
            int count = insights.facialDistribution().getOrDefault(c, 0);
            series.getData().add(new XYChart.Data<>(c, count));
        }

        barChart.getData().add(series);
        card.getChildren().add(barChart);

        return card;
    }

    private VBox buildTextDistributionCard(InsightsService.UserInsights insights) {
        VBox card = new VBox(14);
        card.setPadding(new Insets(22));
        Theme.applyCardStyle(card);
        card.setAlignment(Pos.TOP_LEFT);

        Label title = new Label("Textual Signal Distribution");
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #f1f3f9;");

        Label subtitle = new Label("Mapped emotional cues identified from your written thoughts.");
        subtitle.setStyle("-fx-font-size: 12px; -fx-text-fill: " + Theme.COLOR_TEXT_SECONDARY + ";");

        card.getChildren().addAll(title, subtitle);

        if (insights.textDistribution().isEmpty()) {
            Label emptyLbl = new Label("No textual emotional cues recorded yet. Written thoughts will populate this section.");
            emptyLbl.setStyle("-fx-font-size: 12.5px; -fx-font-style: italic; -fx-text-fill: #64748b;");
            card.getChildren().add(emptyLbl);
            return card;
        }

        int max = insights.textDistribution().values().stream().mapToInt(v -> v).max().orElse(1);

        for (Map.Entry<String, Integer> entry : insights.textDistribution().entrySet()) {
            VBox row = new VBox(4);
            HBox labels = new HBox();
            Label nameLbl = new Label(entry.getKey());
            nameLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: #f1f3f9;");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Label countLbl = new Label(entry.getValue() + " entry" + (entry.getValue() > 1 ? "ies" : "y"));
            countLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: " + Theme.COLOR_TEXT_SECONDARY + ";");

            labels.getChildren().addAll(nameLbl, spacer, countLbl);

            ProgressBar bar = new ProgressBar((double) entry.getValue() / max);
            bar.setMaxWidth(Double.MAX_VALUE);

            row.getChildren().addAll(labels, bar);
            card.getChildren().add(row);
        }

        return card;
    }

    private VBox buildAlignmentCard(InsightsService.UserInsights insights) {
        VBox card = new VBox(12);
        card.setPadding(new Insets(20, 22, 20, 22));
        Theme.applyCardStyle(card);
        card.setAlignment(Pos.TOP_LEFT);

        Label title = new Label("Facial vs Textual Modality Alignment");
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #f1f3f9;");

        Label subtitle = new Label("Comparison of facial signals and textual cues for check-ins with both inputs provided.");
        subtitle.setStyle("-fx-font-size: 12px; -fx-text-fill: " + Theme.COLOR_TEXT_SECONDARY + ";");

        card.getChildren().addAll(title, subtitle);

        int totalMulti = insights.multimodalAlignedCount() + insights.multimodalDifferCount();
        if (totalMulti == 0) {
            Label emptyLbl = new Label("No multimodal check-ins (photo + thoughts) recorded yet.");
            emptyLbl.setStyle("-fx-font-size: 12.5px; -fx-font-style: italic; -fx-text-fill: #64748b;");
            card.getChildren().add(emptyLbl);
            return card;
        }

        HBox statsBox = new HBox(16);
        statsBox.setAlignment(Pos.CENTER);

        VBox alignedBox = buildMiniStat("Aligned Signals", insights.multimodalAlignedCount() + " times");
        VBox differBox = buildMiniStat("Differing Signals", insights.multimodalDifferCount() + " times");
        VBox rateBox = buildMiniStat("Alignment Rate", String.format("%.0f%%", insights.alignmentRate()));

        HBox.setHgrow(alignedBox, Priority.ALWAYS);
        HBox.setHgrow(differBox, Priority.ALWAYS);
        HBox.setHgrow(rateBox, Priority.ALWAYS);

        statsBox.getChildren().addAll(alignedBox, differBox, rateBox);

        ProgressBar agreementBar = new ProgressBar(insights.alignmentRate() / 100.0);
        agreementBar.setMaxWidth(Double.MAX_VALUE);

        card.getChildren().addAll(statsBox, agreementBar);
        return card;
    }

    private VBox buildRecentSignalsSection(InsightsService.UserInsights insights) {
        VBox section = new VBox(12);
        section.setAlignment(Pos.TOP_LEFT);

        Label title = new Label("Recent Signals");
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #f1f3f9;");

        section.getChildren().add(title);

        List<CheckInRecord> recent = insights.recentRecords();
        int displayLimit = Math.min(recent.size(), 5);

        for (int i = 0; i < displayLimit; i++) {
            CheckInRecord r = recent.get(i);
            section.getChildren().add(buildRecentRecordRow(r));
        }

        return section;
    }

    private VBox buildRecentRecordRow(CheckInRecord record) {
        VBox row = new VBox(8);
        row.setPadding(new Insets(14, 18, 14, 18));
        Theme.applyCardStyle(row);

        HBox top = new HBox(10);
        top.setAlignment(Pos.CENTER_LEFT);

        String dateStr = record.createdAt() != null ? record.createdAt().format(DATE_FMT) : "Recent";
        Label dateLbl = new Label(dateStr);
        dateLbl.setStyle("-fx-font-size: 12.5px; -fx-font-weight: bold; -fx-text-fill: #f1f3f9;");

        String mode = CheckInPersistenceService.determineMode(record);
        Label modeBadge = new Label(mode);
        modeBadge.setStyle(
                "-fx-background-color: rgba(99, 102, 241, 0.15);" +
                "-fx-text-fill: #a5b4fc;" +
                "-fx-font-size: 10px;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 2 6;" +
                "-fx-background-radius: 4;"
        );

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button viewBtn = new Button("View Result");
        Theme.applySecondaryButton(viewBtn);
        viewBtn.setStyle(
                viewBtn.getStyle() +
                "-fx-font-size: 11.5px;" +
                "-fx-padding: 4 10;"
        );
        viewBtn.setOnAction(e -> handleViewResult(record));

        top.getChildren().addAll(dateLbl, modeBadge, spacer, viewBtn);

        HBox signalSummary = new HBox(14);
        signalSummary.setAlignment(Pos.CENTER_LEFT);

        if (record.hasPhoto() && record.facialLabel() != null) {
            Label f = new Label("Facial: " + record.facialLabel());
            f.setStyle("-fx-font-size: 12px; -fx-text-fill: " + Theme.COLOR_TEXT_PRIMARY + ";");
            signalSummary.getChildren().add(f);
        }
        if (record.hasThoughts() && record.textLabel() != null) {
            Label t = new Label("Text: " + record.textLabel());
            t.setStyle("-fx-font-size: 12px; -fx-text-fill: " + Theme.COLOR_TEXT_PRIMARY + ";");
            signalSummary.getChildren().add(t);
        }
        if (record.combinedLabel() != null) {
            Label c = new Label("Insight: " + record.combinedLabel());
            c.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #818cf8;");
            signalSummary.getChildren().add(c);
        }

        row.getChildren().addAll(top, signalSummary);
        return row;
    }

    private VBox buildMiniStat(String label, String value) {
        VBox b = new VBox(3);
        b.setAlignment(Pos.CENTER_LEFT);
        b.setPadding(new Insets(10, 12, 10, 12));
        b.setStyle(
                "-fx-background-color: rgba(255, 255, 255, 0.03);" +
                "-fx-border-color: #242b45;" +
                "-fx-border-radius: 6;" +
                "-fx-background-radius: 6;"
        );

        Label l = new Label(label);
        l.setStyle("-fx-font-size: 10.5px; -fx-text-fill: " + Theme.COLOR_TEXT_SECONDARY + ";");

        Label v = new Label(value);
        v.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #f1f3f9;");

        b.getChildren().addAll(l, v);
        return b;
    }

    private VBox buildDisclaimerCard() {
        VBox card = new VBox(6);
        card.setPadding(new Insets(14, 18, 14, 18));
        Theme.applyCardStyle(card);
        card.setStyle(card.getStyle() + "-fx-border-color: rgba(99, 102, 241, 0.25);");

        Label title = new Label("Academic Prototyping Disclaimer");
        title.setStyle("-fx-font-size: 11.5px; -fx-font-weight: bold; -fx-text-fill: " + Theme.COLOR_ACCENT + ";");

        Label body = new Label(
                "Your Insights reflects frequency counts across your self-directed check-ins. " +
                "This prototype is designed for personal emotional reflection and HCI experimentation. " +
                "It does not constitute psychological, psychiatric, or medical diagnosis."
        );
        body.setStyle("-fx-font-size: 11.5px; -fx-text-fill: " + Theme.COLOR_TEXT_SECONDARY + ";");
        body.setWrapText(true);

        card.getChildren().addAll(title, body);
        return card;
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

    public InsightsService getInsightsService() {
        return insightsService;
    }

    public HistoryService getHistoryService() {
        return historyService;
    }

    public VBox getContentArea() {
        return contentArea;
    }
}
