package com.checkin.screens;

import com.checkin.Theme;
import com.checkin.model.CheckInRecord;
import com.checkin.model.User;
import com.checkin.services.HistoryService;
import com.checkin.services.UserSession;
import com.checkin.utils.Navigation;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Profile screen for EmoSense.
 * Displays authenticated user information, account metadata, reflection history
 * summary, and a secure sign-out action using the existing UserSession mechanism.
 */
public class ProfileScreen extends BorderPane {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMMM dd, yyyy  hh:mm a");

    private final Navigation navigation;
    private final HistoryService historyService;
    private final Runnable onBack;
    private final VBox contentArea = new VBox(20);

    public ProfileScreen(Navigation navigation) {
        this(navigation, null, null);
    }

    public ProfileScreen(Navigation navigation, Runnable onBack) {
        this(navigation, null, onBack);
    }

    public ProfileScreen(Navigation navigation, HistoryService historyService, Runnable onBack) {
        this.navigation = navigation;
        this.historyService = historyService != null ? historyService : new HistoryService();
        this.onBack = onBack != null ? onBack : (navigation != null ? navigation::showDashboard : () -> {});

        getStyleClass().add("screen-root");

        setTop(buildHeader());
        setCenter(buildScrollContainer());

        renderProfile();
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

        Button backBtn = new Button("← Back");
        Theme.applySecondaryButton(backBtn);
        backBtn.setOnAction(e -> onBack.run());

        VBox titleBox = new VBox(2);
        Label title = new Label("Account Profile");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: 800; -fx-text-fill: #f1f3f9;");

        Label subtitle = new Label("Your EmoSense identity and session information.");
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: " + Theme.COLOR_TEXT_SECONDARY + ";");
        titleBox.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button signOutBtn = new Button("Sign Out");
        Theme.applySecondaryButton(signOutBtn);
        signOutBtn.setStyle(
                signOutBtn.getStyle() +
                "-fx-border-color: rgba(248, 113, 113, 0.4);" +
                "-fx-text-fill: #fca5a5;"
        );
        signOutBtn.setOnAction(e -> handleSignOut());

        header.getChildren().addAll(backBtn, titleBox, spacer, signOutBtn);
        return header;
    }

    private ScrollPane buildScrollContainer() {
        contentArea.setPadding(new Insets(26, 36, 36, 36));
        contentArea.setAlignment(Pos.TOP_CENTER);
        contentArea.setMaxWidth(760);

        VBox outer = new VBox(contentArea);
        outer.setAlignment(Pos.TOP_CENTER);

        ScrollPane scrollPane = new ScrollPane(outer);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("edge-to-edge");
        return scrollPane;
    }

    private void renderProfile() {
        contentArea.getChildren().clear();

        User currentUser = UserSession.getInstance().getCurrentUser();
        if (currentUser == null && navigation != null && navigation.getAuthService() != null) {
            currentUser = navigation.getAuthService().getCurrentUser();
        }

        // 1. Profile Hero Card
        contentArea.getChildren().add(buildHeroCard(currentUser));

        // 2. Account Details Card
        contentArea.getChildren().add(buildAccountDetailsCard(currentUser));

        // 3. Reflection Activity Card
        contentArea.getChildren().add(buildActivityCard(currentUser));

        // 4. Session & Security Card
        contentArea.getChildren().add(buildSecurityCard());
    }

    private VBox buildHeroCard(User user) {
        VBox card = new VBox(16);
        card.setPadding(new Insets(24, 28, 24, 28));
        Theme.applyCardStyle(card);
        card.setStyle(
                card.getStyle() +
                "-fx-background-color: linear-gradient(to right, #1a1e34, #161a29);" +
                "-fx-border-color: rgba(99, 102, 241, 0.35);" +
                "-fx-border-width: 1.5;"
        );

        HBox topRow = new HBox(20);
        topRow.setAlignment(Pos.CENTER_LEFT);

        // Avatar with initials
        String initials = computeInitials(user);
        StackPane avatarPane = new StackPane();
        Circle bgCircle = new Circle(28);
        bgCircle.setStyle("-fx-fill: #26204c; -fx-stroke: #4338ca; -fx-stroke-width: 2;");
        Label initialsLabel = new Label(initials);
        initialsLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: 800; -fx-text-fill: #c7d2fe;");
        avatarPane.getChildren().addAll(bgCircle, initialsLabel);

        VBox userDetails = new VBox(4);
        userDetails.setAlignment(Pos.CENTER_LEFT);

        String fullName = (user != null && user.getFullName() != null && !user.getFullName().isBlank())
                ? user.getFullName() : "Active Member";
        Label nameLabel = new Label(fullName);
        nameLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: 800; -fx-text-fill: #f1f3f9;");

        String email = (user != null && user.getEmail() != null && !user.getEmail().isBlank())
                ? user.getEmail() : "No email associated";
        Label emailLabel = new Label(email);
        emailLabel.setStyle("-fx-font-size: 13.5px; -fx-text-fill: " + Theme.COLOR_TEXT_SECONDARY + ";");

        HBox badgeRow = new HBox(8);
        badgeRow.setAlignment(Pos.CENTER_LEFT);
        badgeRow.setPadding(new Insets(4, 0, 0, 0));

        Label sessionBadge = new Label(user != null ? "● Active Session" : "● Guest Session");
        sessionBadge.setStyle(
                "-fx-background-color: rgba(16, 185, 129, 0.15);" +
                "-fx-text-fill: #34d399;" +
                "-fx-font-size: 11px;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 3 9;" +
                "-fx-background-radius: 12;" +
                "-fx-border-color: rgba(16, 185, 129, 0.4);" +
                "-fx-border-radius: 12;"
        );

        Label envBadge = new Label("EmoSense Desktop");
        envBadge.setStyle(
                "-fx-background-color: rgba(99, 91, 255, 0.15);" +
                "-fx-text-fill: #a5b4fc;" +
                "-fx-font-size: 11px;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 3 9;" +
                "-fx-background-radius: 12;" +
                "-fx-border-color: rgba(99, 91, 255, 0.35);" +
                "-fx-border-radius: 12;"
        );

        badgeRow.getChildren().addAll(sessionBadge, envBadge);
        userDetails.getChildren().addAll(nameLabel, emailLabel, badgeRow);

        topRow.getChildren().addAll(avatarPane, userDetails);
        card.getChildren().add(topRow);
        return card;
    }

    private VBox buildAccountDetailsCard(User user) {
        VBox card = new VBox(14);
        card.setPadding(new Insets(22, 26, 22, 26));
        Theme.applyCardStyle(card);

        Label sectionTitle = new Label("ACCOUNT INFORMATION");
        sectionTitle.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: #818cf8; -fx-letter-spacing: 0.5px;");

        GridPane grid = new GridPane();
        grid.setHgap(24);
        grid.setVgap(14);

        int row = 0;
        addDetailRow(grid, row++, "Full Name", (user != null && user.getFullName() != null && !user.getFullName().isBlank()) ? user.getFullName() : "Not provided");
        addDetailRow(grid, row++, "Email Address", (user != null && user.getEmail() != null && !user.getEmail().isBlank()) ? user.getEmail() : "Not provided");
        addDetailRow(grid, row++, "Account Created", (user != null && user.getCreatedAt() != null) ? user.getCreatedAt().format(DATE_FMT) : "Not available");
        addDetailRow(grid, row++, "Account ID", (user != null && user.getId() != null) ? user.getId() : "Session-only");

        card.getChildren().addAll(sectionTitle, grid);
        return card;
    }

    private void addDetailRow(GridPane grid, int row, String label, String value) {
        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size: 12.5px; -fx-text-fill: " + Theme.COLOR_TEXT_SECONDARY + ";");
        lbl.setPrefWidth(160);

        Label val = new Label(value);
        val.setStyle("-fx-font-size: 13.5px; -fx-font-weight: bold; -fx-text-fill: #f1f3f9;");
        val.setWrapText(true);

        grid.add(lbl, 0, row);
        grid.add(val, 1, row);
    }

    private VBox buildActivityCard(User user) {
        VBox card = new VBox(14);
        card.setPadding(new Insets(22, 26, 22, 26));
        Theme.applyCardStyle(card);

        HBox titleRow = new HBox(10);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label sectionTitle = new Label("REFLECTION ACTIVITY");
        sectionTitle.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: #818cf8; -fx-letter-spacing: 0.5px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button historyBtn = new Button("View Check-In History  →");
        Theme.applySecondaryButton(historyBtn);
        historyBtn.setStyle(
                historyBtn.getStyle() +
                "-fx-font-size: 11.5px;" +
                "-fx-padding: 5 12;"
        );
        historyBtn.setOnAction(e -> {
            if (navigation != null) navigation.showHistory();
        });

        titleRow.getChildren().addAll(sectionTitle, spacer, historyBtn);

        int checkInCount = 0;
        String currentUserId = user != null ? user.getId() : UserSession.getInstance().getCurrentUserId();
        if (currentUserId != null && !currentUserId.isBlank()) {
            try {
                List<CheckInRecord> history = historyService.getUserHistory(currentUserId);
                if (history != null) {
                    checkInCount = history.size();
                }
            } catch (Exception ignored) {}
        }

        HBox statsRow = new HBox(16);
        statsRow.setAlignment(Pos.CENTER);

        VBox countBox = buildStatBox("Total Check-Ins", String.valueOf(checkInCount));
        VBox statusBox = buildStatBox("Reflection Status", checkInCount > 0 ? "Active Contributor" : "New User");
        VBox isolationBox = buildStatBox("Data Privacy", "Strict User Isolation");

        HBox.setHgrow(countBox, Priority.ALWAYS);
        HBox.setHgrow(statusBox, Priority.ALWAYS);
        HBox.setHgrow(isolationBox, Priority.ALWAYS);

        statsRow.getChildren().addAll(countBox, statusBox, isolationBox);

        card.getChildren().addAll(titleRow, statsRow);
        return card;
    }

    private VBox buildStatBox(String label, String value) {
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
        v.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #f1f3f9;");

        b.getChildren().addAll(l, v);
        return b;
    }

    private VBox buildSecurityCard() {
        VBox card = new VBox(14);
        card.setPadding(new Insets(22, 26, 22, 26));
        Theme.applyCardStyle(card);

        Label sectionTitle = new Label("SESSION & SECURITY");
        sectionTitle.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: #818cf8; -fx-letter-spacing: 0.5px;");

        Label desc = new Label(
                "Your active session is stored securely in local memory. " +
                "Signing out clears your user session immediately and prevents your check-ins and insights from remaining visible."
        );
        desc.setStyle("-fx-font-size: 12.5px; -fx-text-fill: " + Theme.COLOR_TEXT_SECONDARY + ";");
        desc.setWrapText(true);

        Button logoutBtn = new Button("Sign Out of EmoSense");
        Theme.applySecondaryButton(logoutBtn);
        logoutBtn.setStyle(
                logoutBtn.getStyle() +
                "-fx-background-color: " + Theme.COLOR_DANGER_BG + ";" +
                "-fx-text-fill: " + Theme.COLOR_DANGER_TEXT + ";" +
                "-fx-border-color: rgba(248, 113, 113, 0.35);" +
                "-fx-border-radius: 8;" +
                "-fx-padding: 9 18;"
        );
        logoutBtn.setOnAction(e -> handleSignOut());

        card.getChildren().addAll(sectionTitle, desc, logoutBtn);
        return card;
    }

    private void handleSignOut() {
        if (navigation != null) {
            navigation.signOut();
        } else {
            UserSession.getInstance().logout();
        }
    }

    private String computeInitials(User user) {
        if (user == null) return "ES";
        String name = user.getFullName();
        if (name == null || name.isBlank()) {
            String email = user.getEmail();
            return (email != null && !email.isBlank()) ? email.substring(0, Math.min(2, email.length())).toUpperCase() : "ES";
        }
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2) {
            return ("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase();
        }
        return name.substring(0, Math.min(2, name.length())).toUpperCase();
    }

    public VBox getContentArea() {
        return contentArea;
    }
}
