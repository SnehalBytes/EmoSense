package com.checkin.screens;

import com.checkin.Theme;
import com.checkin.dao.AnalysisResultDAO;
import com.checkin.dao.CheckInDAO;
import com.checkin.model.AnalysisResult;
import com.checkin.model.AnalysisResultRecord;
import com.checkin.model.CheckInRecord;
import com.checkin.services.CheckInPersistenceService;
import com.checkin.services.HistoryService;
import com.checkin.services.UserSession;
import com.checkin.utils.Navigation;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
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
 * Check-In History screen for EmoSense.
 * Displays only the authenticated user's check-ins,
 * with options to view stored analysis results without re-running models.
 */
public class HistoryScreen extends BorderPane {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMM dd, yyyy  hh:mm a");

    private final Navigation navigation;
    private final HistoryService historyService;
    private final VBox contentArea = new VBox(16);

    public HistoryScreen(Navigation navigation) {
        this(navigation, new HistoryService());
    }

    public HistoryScreen(Navigation navigation, HistoryService historyService) {
        this.navigation = navigation;
        this.historyService = historyService != null ? historyService : new HistoryService();

        getStyleClass().add("screen-root");

        setTop(buildHeader());
        setCenter(buildCenterContainer());

        loadHistory();
    }

    public HistoryScreen(Navigation navigation, CheckInDAO checkInDAO, AnalysisResultDAO resultDAO) {
        this(navigation, new HistoryService(checkInDAO, resultDAO));
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
        Label title = new Label("Check-In History");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: 800; -fx-text-fill: #f1f3f9;");

        Label subtitle = new Label("Review your previous emotional check-ins.");
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: " + Theme.COLOR_TEXT_SECONDARY + ";");
        titleBox.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button newCheckInBtn = new Button("New Check-In");
        newCheckInBtn.getStyleClass().add("analyze-button");
        newCheckInBtn.setStyle(
                "-fx-background-color: " + Theme.COLOR_ACCENT + ";" +
                "-fx-text-fill: white;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 8 18;" +
                "-fx-background-radius: 8;" +
                "-fx-cursor: hand;"
        );
        newCheckInBtn.setOnAction(e -> {
            if (navigation != null) navigation.showCheckIn();
        });

        header.getChildren().addAll(backBtn, titleBox, spacer, newCheckInBtn);
        return header;
    }

    private ScrollPane buildCenterContainer() {
        contentArea.setPadding(new Insets(24, 36, 28, 36));
        contentArea.setAlignment(Pos.TOP_CENTER);
        contentArea.setMaxWidth(860);

        VBox outer = new VBox(contentArea);
        outer.setAlignment(Pos.TOP_CENTER);

        ScrollPane scrollPane = new ScrollPane(outer);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("edge-to-edge");
        return scrollPane;
    }

    public void loadHistory() {
        contentArea.getChildren().clear();

        // 1. Show Loading State
        VBox loadingBox = buildLoadingState();
        contentArea.getChildren().add(loadingBox);

        // Fetch records for the currently authenticated user
        String currentUserId = UserSession.getInstance().getCurrentUserId();
        if (currentUserId == null || currentUserId.isBlank()) {
            contentArea.getChildren().clear();
            contentArea.getChildren().add(buildEmptyState());
            return;
        }

        try {
            List<CheckInRecord> records = historyService.getUserHistory(currentUserId);
            contentArea.getChildren().clear();

            if (records == null || records.isEmpty()) {
                contentArea.getChildren().add(buildEmptyState());
            } else {
                HBox summaryRow = new HBox(12);
                summaryRow.setAlignment(Pos.CENTER_LEFT);
                Label countLabel = new Label("Showing " + records.size() + " recorded check-in" + (records.size() > 1 ? "s" : ""));
                countLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + Theme.COLOR_TEXT_SECONDARY + ";");

                Label userTag = new Label("Account: " + (UserSession.getInstance().getCurrentUser() != null ? UserSession.getInstance().getCurrentUser().getEmail() : currentUserId));
                userTag.setStyle("-fx-font-size: 12px; -fx-text-fill: #635bff;");

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                summaryRow.getChildren().addAll(countLabel, spacer, userTag);
                contentArea.getChildren().add(summaryRow);

                for (CheckInRecord record : records) {
                    contentArea.getChildren().add(buildRecordCard(record));
                }
            }
        } catch (Exception e) {
            System.err.println("[HistoryScreen] Failed to retrieve history: " + e.getMessage());
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

        Label loadingMsg = new Label("Loading your check-ins...");
        loadingMsg.setStyle("-fx-font-size: 14px; -fx-text-fill: " + Theme.COLOR_TEXT_PRIMARY + ";");

        box.getChildren().addAll(spinner, loadingMsg);
        return box;
    }

    private VBox buildErrorState() {
        VBox box = new VBox(14);
        box.setPadding(new Insets(40));
        box.setAlignment(Pos.CENTER);
        Theme.applyCardStyle(box);

        Label errorTitle = new Label("Unable to load check-in history.");
        errorTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #f87171;");

        Label errorMsg = new Label("A database error occurred while accessing your stored records. Please try again.");
        errorMsg.setStyle("-fx-font-size: 13px; -fx-text-fill: " + Theme.COLOR_TEXT_SECONDARY + ";");
        errorMsg.setWrapText(true);

        Button retryBtn = new Button("Retry");
        Theme.applySecondaryButton(retryBtn);
        retryBtn.setOnAction(e -> loadHistory());

        box.getChildren().addAll(errorTitle, errorMsg, retryBtn);
        return box;
    }

    private VBox buildEmptyState() {
        VBox box = new VBox(14);
        box.setPadding(new Insets(48, 32, 48, 32));
        box.setAlignment(Pos.CENTER);
        Theme.applyCardStyle(box);

        Label emptyTitle = new Label("No check-ins yet");
        emptyTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #f1f3f9;");

        Label emptyMsg = new Label("Your completed check-ins will appear here.");
        emptyMsg.setStyle("-fx-font-size: 13px; -fx-text-fill: " + Theme.COLOR_TEXT_SECONDARY + ";");
        emptyMsg.setWrapText(true);
        emptyMsg.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        Button startBtn = new Button("Start a Check-In");
        startBtn.getStyleClass().add("analyze-button");
        startBtn.setStyle(
                "-fx-background-color: " + Theme.COLOR_ACCENT + ";" +
                "-fx-text-fill: white;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 10 24;" +
                "-fx-background-radius: 8;" +
                "-fx-cursor: hand;"
        );
        startBtn.setOnAction(e -> {
            if (navigation != null) navigation.showCheckIn();
        });

        box.getChildren().addAll(emptyTitle, emptyMsg, startBtn);
        return box;
    }

    private VBox buildRecordCard(CheckInRecord record) {
        VBox card = new VBox(14);
        card.setPadding(new Insets(20, 24, 20, 24));
        Theme.applyCardStyle(card);

        // 1. Top row: Date/Time + Modality Badge + Model Badge + Spacer + View Result Button
        HBox topRow = new HBox(10);
        topRow.setAlignment(Pos.CENTER_LEFT);

        String formattedDate = record.createdAt() != null ? record.createdAt().format(DATE_FMT) : "Recent";
        Label dateLabel = new Label(formattedDate);
        dateLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #f1f3f9;");

        String mode = CheckInPersistenceService.determineMode(record);
        Label modeBadge = new Label(mode);
        modeBadge.getStyleClass().add("badge-single");
        modeBadge.setStyle(
                "-fx-background-color: rgba(99, 102, 241, 0.15);" +
                "-fx-text-fill: #a5b4fc;" +
                "-fx-padding: 3 8;" +
                "-fx-background-radius: 4;" +
                "-fx-font-size: 10px;" +
                "-fx-font-weight: bold;"
        );

        Label modelBadge = new Label("REAL MODEL");
        modelBadge.getStyleClass().add("badge-real");
        modelBadge.setStyle(
                "-fx-background-color: rgba(34, 197, 94, 0.15);" +
                "-fx-text-fill: #4ade80;" +
                "-fx-padding: 3 8;" +
                "-fx-background-radius: 4;" +
                "-fx-font-size: 10px;" +
                "-fx-font-weight: bold;"
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

        topRow.getChildren().addAll(dateLabel, modeBadge, modelBadge, spacer, viewResultBtn);

        // 2. Multimodal Combined Insight row (if multimodal)
        VBox signalsBox = new VBox(8);
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

            Label fusionTag = new Label("Multimodal Insight:");
            fusionTag.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #a5b4fc;");

            Label fusionVal = new Label(record.combinedLabel());
            fusionVal.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #f1f3f9;");

            Region cSpacer = new Region();
            HBox.setHgrow(cSpacer, Priority.ALWAYS);

            double cConf = record.combinedConfidence();
            ProgressBar cBar = new ProgressBar(cConf / 100.0);
            cBar.setPrefWidth(120);
            cBar.getStyleClass().add("prob-bar-top");

            Label cConfText = new Label(String.format("%.0f%% confidence", cConf));
            cConfText.setStyle("-fx-font-size: 11px; -fx-text-fill: " + Theme.COLOR_TEXT_SECONDARY + ";");

            combinedRow.getChildren().addAll(fusionTag, fusionVal, cSpacer, cBar, cConfText);
            signalsBox.getChildren().add(combinedRow);
        }

        // 3. Modality Signals Grid
        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(6);

        // Facial Signal column
        VBox facialBox = new VBox(4);
        if (record.hasPhoto() && record.facialLabel() != null && !record.facialLabel().isBlank()) {
            Label fTitle = new Label("Facial Expression:");
            fTitle.setStyle("-fx-font-size: 11px; -fx-text-fill: " + Theme.COLOR_TEXT_SECONDARY + ";");

            HBox fValRow = new HBox(8);
            fValRow.setAlignment(Pos.CENTER_LEFT);
            Label fVal = new Label(record.facialLabel());
            fVal.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + Theme.COLOR_TEXT_PRIMARY + ";");

            if (record.facialConfidence() > 0) {
                Label fConf = new Label(String.format("(%.0f%% confidence)", record.facialConfidence()));
                fConf.setStyle("-fx-font-size: 11px; -fx-text-fill: " + Theme.COLOR_TEXT_SECONDARY + ";");
                fValRow.getChildren().addAll(fVal, fConf);
            } else {
                fValRow.getChildren().add(fVal);
            }
            facialBox.getChildren().addAll(fTitle, fValRow);
        } else {
            Label fNotice = new Label("Facial signal not provided.");
            fNotice.setStyle("-fx-font-size: 12px; -fx-font-style: italic; -fx-text-fill: #64748b;");
            facialBox.getChildren().add(fNotice);
        }

        // Textual Signal column
        VBox textBox = new VBox(4);
        if (record.hasThoughts() && record.textLabel() != null && !record.textLabel().isBlank()) {
            Label tTitle = new Label("Textual Emotional Cue:");
            tTitle.setStyle("-fx-font-size: 11px; -fx-text-fill: " + Theme.COLOR_TEXT_SECONDARY + ";");

            HBox tValRow = new HBox(8);
            tValRow.setAlignment(Pos.CENTER_LEFT);
            Label tVal = new Label(record.textLabel());
            tVal.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + Theme.COLOR_TEXT_PRIMARY + ";");

            if (record.textConfidence() > 0) {
                Label tConf = new Label(String.format("(%.0f%% confidence)", record.textConfidence()));
                tConf.setStyle("-fx-font-size: 11px; -fx-text-fill: " + Theme.COLOR_TEXT_SECONDARY + ";");
                tValRow.getChildren().addAll(tVal, tConf);
            } else {
                tValRow.getChildren().add(tVal);
            }
            textBox.getChildren().addAll(tTitle, tValRow);
        } else {
            Label tNotice = new Label("Text signal not provided.");
            tNotice.setStyle("-fx-font-size: 12px; -fx-font-style: italic; -fx-text-fill: #64748b;");
            textBox.getChildren().add(tNotice);
        }

        grid.add(facialBox, 0, 0);
        grid.add(textBox, 1, 0);
        signalsBox.getChildren().add(grid);

        // 4. Thoughts excerpt snippet (if present)
        if (record.hasThoughts()) {
            Label snippet = new Label("“" + truncate(record.thoughtsText(), 120) + "”");
            snippet.setStyle("-fx-font-size: 12px; -fx-font-style: italic; -fx-text-fill: #94a3b8;");
            snippet.setWrapText(true);
            signalsBox.getChildren().add(snippet);
        }

        card.getChildren().addAll(topRow, signalsBox);
        return card;
    }

    private void handleViewResult(CheckInRecord record) {
        // Retrieve stored AnalysisResult from database without re-running models
        Optional<AnalysisResult> resultOpt = historyService.getStoredResult(record.id());

        AnalysisResult displayResult;
        if (resultOpt.isPresent()) {
            displayResult = resultOpt.get();
        } else {
            // Reconstruct directly from stored check-in record
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

    public VBox getContentArea() {
        return contentArea;
    }
}
