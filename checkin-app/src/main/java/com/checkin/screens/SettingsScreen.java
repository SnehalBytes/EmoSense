package com.checkin.screens;

import com.checkin.Theme;
import com.checkin.model.User;
import com.checkin.services.UserSession;
import com.checkin.utils.Navigation;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;

/**
 * Settings screen for EmoSense.
 * Presents application appearance configuration, honest notification statuses,
 * direct navigation to Privacy and Technology documentation, and account management.
 * Strictly avoids non-functional switches or fabricated persistence.
 */
public class SettingsScreen extends BorderPane {

    private final Navigation navigation;
    private final Runnable onBack;
    private final VBox contentArea = new VBox(20);

    public SettingsScreen(Navigation navigation) {
        this(navigation, null);
    }

    public SettingsScreen(Navigation navigation, Runnable onBack) {
        this.navigation = navigation;
        this.onBack = onBack != null ? onBack : (navigation != null ? navigation::showDashboard : () -> {});

        getStyleClass().add("screen-root");

        setTop(buildHeader());
        setCenter(buildScrollContainer());

        renderSettings();
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
        Label title = new Label("Settings & Preferences");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: 800; -fx-text-fill: #f1f3f9;");

        Label subtitle = new Label("Configure your EmoSense desktop environment and view system statuses.");
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: " + Theme.COLOR_TEXT_SECONDARY + ";");
        titleBox.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button profileShortcutBtn = new Button("View Profile");
        Theme.applySecondaryButton(profileShortcutBtn);
        profileShortcutBtn.setOnAction(e -> {
            if (navigation != null) navigation.showProfile(this::showSelf);
        });

        header.getChildren().addAll(backBtn, titleBox, spacer, profileShortcutBtn);
        return header;
    }

    private void showSelf() {
        if (navigation != null) {
            navigation.showSettings(onBack);
        }
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

    private void renderSettings() {
        contentArea.getChildren().clear();

        // 1. Appearance Section
        contentArea.getChildren().add(buildAppearanceCard());

        // 2. Notifications Section (Honest status without fake switches)
        contentArea.getChildren().add(buildNotificationsCard());

        // 3. Privacy & Data Practices Section
        contentArea.getChildren().add(buildPrivacyCard());

        // 4. Application & Technology Section
        contentArea.getChildren().add(buildApplicationCard());

        // 5. Account Management & Session Section
        contentArea.getChildren().add(buildAccountCard());
    }

    private VBox buildAppearanceCard() {
        VBox card = new VBox(14);
        card.setPadding(new Insets(22, 26, 22, 26));
        Theme.applyCardStyle(card);

        Label sectionTitle = new Label("APPEARANCE");
        sectionTitle.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: #818cf8; -fx-letter-spacing: 0.5px;");

        HBox themeRow = new HBox(14);
        themeRow.setAlignment(Pos.CENTER_LEFT);

        VBox themeInfo = new VBox(4);
        Label themeLabel = new Label("Dark Theme (Active / Default)");
        themeLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #f1f3f9;");

        Label themeDesc = new Label(
                "EmoSense utilizes a calibrated dark navy/charcoal color palette (#0c0e17) to provide a restful, " +
                "calm environment for emotional reflection and reduce visual fatigue during journaling."
        );
        themeDesc.setStyle("-fx-font-size: 12.5px; -fx-text-fill: " + Theme.COLOR_TEXT_SECONDARY + ";");
        themeDesc.setWrapText(true);
        themeInfo.getChildren().addAll(themeLabel, themeDesc);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label activeBadge = new Label("ACTIVE");
        activeBadge.setStyle(
                "-fx-background-color: rgba(99, 102, 241, 0.2);" +
                "-fx-text-fill: #a5b4fc;" +
                "-fx-font-size: 11px;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 4 10;" +
                "-fx-background-radius: 6;" +
                "-fx-border-color: rgba(99, 102, 241, 0.4);" +
                "-fx-border-radius: 6;"
        );

        themeRow.getChildren().addAll(themeInfo, spacer, activeBadge);
        card.getChildren().addAll(sectionTitle, themeRow);
        return card;
    }

    private VBox buildNotificationsCard() {
        VBox card = new VBox(14);
        card.setPadding(new Insets(22, 26, 22, 26));
        Theme.applyCardStyle(card);

        Label sectionTitle = new Label("NOTIFICATIONS & REMINDERS");
        sectionTitle.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: #818cf8; -fx-letter-spacing: 0.5px;");

        // Item 1: In-App Check-In Prompts
        VBox promptBox = new VBox(4);
        HBox promptHeader = new HBox(10);
        promptHeader.setAlignment(Pos.CENTER_LEFT);

        Label promptTitle = new Label("In-App Reflection Prompts");
        promptTitle.setStyle("-fx-font-size: 13.5px; -fx-font-weight: bold; -fx-text-fill: #f1f3f9;");

        Region pSpacer = new Region();
        HBox.setHgrow(pSpacer, Priority.ALWAYS);

        Label promptBadge = new Label("Enabled");
        promptBadge.setStyle(
                "-fx-background-color: rgba(16, 185, 129, 0.15);" +
                "-fx-text-fill: #34d399;" +
                "-fx-font-size: 11px;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 3 8;" +
                "-fx-background-radius: 6;"
        );
        promptHeader.getChildren().addAll(promptTitle, pSpacer, promptBadge);

        Label promptDesc = new Label("Contextual check-in prompts and supportive reflections appear dynamically within the app interface.");
        promptDesc.setStyle("-fx-font-size: 12px; -fx-text-fill: " + Theme.COLOR_TEXT_SECONDARY + ";");
        promptDesc.setWrapText(true);
        promptBox.getChildren().addAll(promptHeader, promptDesc);

        // Item 2: System Push Notifications
        VBox sysBox = new VBox(4);
        HBox sysHeader = new HBox(10);
        sysHeader.setAlignment(Pos.CENTER_LEFT);

        Label sysTitle = new Label("System Tray / Push Notifications");
        sysTitle.setStyle("-fx-font-size: 13.5px; -fx-font-weight: bold; -fx-text-fill: #f1f3f9;");

        Region sSpacer = new Region();
        HBox.setHgrow(sSpacer, Priority.ALWAYS);

        Label sysBadge = new Label("Not Configured in Prototype");
        sysBadge.setStyle(
                "-fx-background-color: rgba(100, 116, 139, 0.2);" +
                "-fx-text-fill: #94a3b8;" +
                "-fx-font-size: 11px;" +
                "-fx-padding: 3 8;" +
                "-fx-background-radius: 6;"
        );
        sysHeader.getChildren().addAll(sysTitle, sSpacer, sysBadge);

        Label sysDesc = new Label(
                "Operating system desktop notifications are not active in this academic desktop prototype. " +
                "All reflection prompts remain strictly in-app to protect user privacy."
        );
        sysDesc.setStyle("-fx-font-size: 12px; -fx-text-fill: " + Theme.COLOR_TEXT_SECONDARY + ";");
        sysDesc.setWrapText(true);
        sysBox.getChildren().addAll(sysHeader, sysDesc);

        card.getChildren().addAll(sectionTitle, promptBox, sysBox);
        return card;
    }

    private VBox buildPrivacyCard() {
        VBox card = new VBox(14);
        card.setPadding(new Insets(22, 26, 22, 26));
        Theme.applyCardStyle(card);

        Label sectionTitle = new Label("PRIVACY & DATA HANDLING");
        sectionTitle.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: #818cf8; -fx-letter-spacing: 0.5px;");

        Label desc = new Label(
                "EmoSense processes your submitted facial images and written thoughts to produce probabilistic " +
                "emotional signals. Learn more about data isolation, local storage, and the non-clinical prototype notice."
        );
        desc.setStyle("-fx-font-size: 12.5px; -fx-text-fill: " + Theme.COLOR_TEXT_SECONDARY + ";");
        desc.setWrapText(true);

        Button privacyBtn = new Button("View Privacy & Data Practices  →");
        Theme.applySecondaryButton(privacyBtn);
        privacyBtn.setStyle(
                privacyBtn.getStyle() +
                "-fx-font-size: 12px;" +
                "-fx-border-color: #635bff;" +
                "-fx-text-fill: #c7d2fe;" +
                "-fx-padding: 8 16;"
        );
        privacyBtn.setOnAction(e -> {
            if (navigation != null) navigation.showPrivacy(this::showSelf);
        });

        card.getChildren().addAll(sectionTitle, desc, privacyBtn);
        return card;
    }

    private VBox buildApplicationCard() {
        VBox card = new VBox(14);
        card.setPadding(new Insets(22, 26, 22, 26));
        Theme.applyCardStyle(card);

        Label sectionTitle = new Label("APPLICATION & ABOUT");
        sectionTitle.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: #818cf8; -fx-letter-spacing: 0.5px;");

        VBox infoBox = new VBox(4);
        Label appName = new Label("EmoSense — Academic Affective Computing Prototype");
        appName.setStyle("-fx-font-size: 13.5px; -fx-font-weight: bold; -fx-text-fill: #f1f3f9;");

        Label appDesc = new Label("B.Tech Capstone Project • Java 17 • JavaFX Desktop • ONNX Runtime Machine Learning");
        appDesc.setStyle("-fx-font-size: 12px; -fx-text-fill: " + Theme.COLOR_TEXT_SECONDARY + ";");

        infoBox.getChildren().addAll(appName, appDesc);

        Button techBtn = new Button("About & Technology Architecture  →");
        Theme.applySecondaryButton(techBtn);
        techBtn.setStyle(
                techBtn.getStyle() +
                "-fx-font-size: 12px;" +
                "-fx-border-color: #635bff;" +
                "-fx-text-fill: #c7d2fe;" +
                "-fx-padding: 8 16;"
        );
        techBtn.setOnAction(e -> {
            if (navigation != null) navigation.showTechnology(this::showSelf);
        });

        card.getChildren().addAll(sectionTitle, infoBox, techBtn);
        return card;
    }

    private VBox buildAccountCard() {
        VBox card = new VBox(14);
        card.setPadding(new Insets(22, 26, 22, 26));
        Theme.applyCardStyle(card);

        Label sectionTitle = new Label("ACCOUNT & SESSION");
        sectionTitle.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: #818cf8; -fx-letter-spacing: 0.5px;");

        User user = UserSession.getInstance().getCurrentUser();
        if (user == null && navigation != null && navigation.getAuthService() != null) {
            user = navigation.getAuthService().getCurrentUser();
        }

        String userDisplay = (user != null && user.getFullName() != null && !user.getFullName().isBlank())
                ? user.getFullName() + " (" + user.getEmail() + ")"
                : (user != null && user.getEmail() != null) ? user.getEmail() : "Guest / Active Session";

        Label userLabel = new Label("Signed in as: " + userDisplay);
        userLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #f1f3f9;");

        HBox btnRow = new HBox(12);
        btnRow.setAlignment(Pos.CENTER_LEFT);

        Button profileBtn = new Button("Manage Profile  →");
        Theme.applySecondaryButton(profileBtn);
        profileBtn.setOnAction(e -> {
            if (navigation != null) navigation.showProfile(this::showSelf);
        });

        Button signOutBtn = new Button("Sign Out");
        Theme.applySecondaryButton(signOutBtn);
        signOutBtn.setStyle(
                signOutBtn.getStyle() +
                "-fx-background-color: " + Theme.COLOR_DANGER_BG + ";" +
                "-fx-text-fill: " + Theme.COLOR_DANGER_TEXT + ";" +
                "-fx-border-color: rgba(248, 113, 113, 0.35);"
        );
        signOutBtn.setOnAction(e -> {
            if (navigation != null) navigation.signOut();
            else UserSession.getInstance().logout();
        });

        btnRow.getChildren().addAll(profileBtn, signOutBtn);
        card.getChildren().addAll(sectionTitle, userLabel, btnRow);
        return card;
    }

    public VBox getContentArea() {
        return contentArea;
    }
}
