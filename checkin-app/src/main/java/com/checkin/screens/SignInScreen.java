package com.checkin.screens;

import com.checkin.Theme;
import com.checkin.services.AuthenticationService;
import com.checkin.utils.Navigation;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * Sign In screen for EmoSense.
 * Centers a polished authentication card with email and password inputs,
 * input validation, and direct switching to the Sign Up screen.
 */
public class SignInScreen extends StackPane {

    private final Navigation navigation;
    private final TextField emailField = new TextField();
    private final PasswordField passwordField = new PasswordField();
    private final Label messageLabel = new Label();

    public SignInScreen(Navigation navigation) {
        this.navigation = navigation;
        getStyleClass().add("screen-root");
        setPadding(new Insets(30));

        VBox contentBox = new VBox(22);
        contentBox.setAlignment(Pos.CENTER);
        contentBox.setMaxWidth(460);

        // Header branding
        VBox brandingBox = buildBranding();

        // Auth Card
        VBox card = buildSignInCard();

        contentBox.getChildren().addAll(brandingBox, card);

        StackPane centerContainer = new StackPane(contentBox);
        centerContainer.setAlignment(Pos.CENTER);
        centerContainer.setPadding(new Insets(20));

        ScrollPane scrollPane = new ScrollPane(centerContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.getStyleClass().add("edge-to-edge");
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        getChildren().add(scrollPane);
        setAlignment(Pos.CENTER);
    }

    private VBox buildBranding() {
        VBox box = new VBox(8);
        box.setAlignment(Pos.CENTER);

        Label logo = new Label("EMOSENSE");
        logo.setStyle(
                "-fx-font-size: 30px;" +
                "-fx-font-weight: 800;" +
                "-fx-text-fill: #f1f3f9;" +
                "-fx-letter-spacing: 3px;"
        );

        Label tagline = new Label("Understanding the emotions we don't always express.");
        tagline.setStyle(
                "-fx-font-size: 14px;" +
                "-fx-text-fill: " + Theme.COLOR_TEXT_SECONDARY + ";"
        );
        tagline.setWrapText(true);
        tagline.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        box.getChildren().addAll(logo, tagline);
        return box;
    }

    private VBox buildSignInCard() {
        VBox card = new VBox(16);
        card.setPadding(new Insets(32));
        card.setAlignment(Pos.TOP_LEFT);
        Theme.applyCardStyle(card);

        // Segmented tab switch at top of card
        HBox switchBox = buildSwitchTabs();

        Label cardTitle = new Label("Welcome Back");
        cardTitle.setStyle(
                "-fx-font-size: 20px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: " + Theme.COLOR_TEXT_PRIMARY + ";"
        );

        Label cardSubtitle = new Label("Enter your credentials to access your check-ins.");
        cardSubtitle.setStyle(
                "-fx-font-size: 12px;" +
                "-fx-text-fill: " + Theme.COLOR_TEXT_SECONDARY + ";"
        );

        // Message Label (for error or success)
        messageLabel.setVisible(false);
        messageLabel.setManaged(false);
        messageLabel.setWrapText(true);

        // Form Fields
        VBox emailBox = new VBox(6);
        Label emailLabel = new Label("Email Address");
        emailLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " + Theme.COLOR_TEXT_SECONDARY + ";");
        emailField.setPromptText("you@example.com");
        Theme.applyInputField(emailField);
        emailBox.getChildren().addAll(emailLabel, emailField);

        VBox passwordBox = new VBox(6);
        Label passwordLabel = new Label("Password");
        passwordLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " + Theme.COLOR_TEXT_SECONDARY + ";");
        passwordField.setPromptText("••••••••");
        Theme.applyInputField(passwordField);
        passwordBox.getChildren().addAll(passwordLabel, passwordField);

        // Submit action on Enter key
        passwordField.setOnAction(e -> handleSignIn());
        emailField.setOnAction(e -> passwordField.requestFocus());

        // Sign In Button
        Button signInBtn = new Button("Sign In");
        signInBtn.setMaxWidth(Double.MAX_VALUE);
        Theme.applyPrimaryButton(signInBtn);
        signInBtn.setOnAction(e -> handleSignIn());

        // Footer navigation
        HBox footer = new HBox(6);
        footer.setAlignment(Pos.CENTER);
        footer.setPadding(new Insets(8, 0, 0, 0));

        Label noAccountLabel = new Label("Don't have an account?");
        noAccountLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: " + Theme.COLOR_TEXT_SECONDARY + ";");

        Hyperlink createAccountLink = new Hyperlink("Create Account");
        createAccountLink.setStyle(
                "-fx-font-size: 13px;" +
                "-fx-text-fill: " + Theme.COLOR_ACCENT + ";" +
                "-fx-font-weight: bold;" +
                "-fx-underline: false;"
        );
        createAccountLink.setOnAction(e -> navigation.showSignUp());
        footer.getChildren().addAll(noAccountLabel, createAccountLink);

        // Ensure credentials start completely empty
        emailField.setText("");
        passwordField.setText("");

        card.getChildren().addAll(
                switchBox,
                cardTitle,
                cardSubtitle
        );

        if (navigation != null && navigation.getAuthService() != null && !navigation.getAuthService().isPersistentStorageAvailable()) {
            Label dbNotice = new Label("⚠ Database Offline: Running in session-only mode. Accounts created will not persist after restart.");
            dbNotice.setStyle(
                    "-fx-font-size: 11px;" +
                    "-fx-text-fill: #fbbf24;" +
                    "-fx-background-color: rgba(251, 191, 36, 0.12);" +
                    "-fx-border-color: rgba(251, 191, 36, 0.3);" +
                    "-fx-border-radius: 4;" +
                    "-fx-padding: 6 10;" +
                    "-fx-background-radius: 4;"
            );
            dbNotice.setWrapText(true);
            card.getChildren().add(dbNotice);
        }

        card.getChildren().addAll(
                messageLabel,
                emailBox,
                passwordBox,
                signInBtn,
                footer
        );
        return card;
    }

    public TextField getEmailField() {
        return emailField;
    }

    public PasswordField getPasswordField() {
        return passwordField;
    }

    private HBox buildSwitchTabs() {
        HBox switchBox = new HBox(8);
        switchBox.setAlignment(Pos.CENTER);
        switchBox.setPadding(new Insets(0, 0, 8, 0));

        Button signInTab = new Button("Sign In");
        signInTab.setStyle(
                "-fx-background-color: " + Theme.COLOR_ACCENT + ";" +
                "-fx-text-fill: white;" +
                "-fx-background-radius: 6;" +
                "-fx-font-size: 12px;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 6 18 6 18;"
        );

        Button signUpTab = new Button("Create Account");
        signUpTab.setStyle(
                "-fx-background-color: transparent;" +
                "-fx-text-fill: " + Theme.COLOR_TEXT_SECONDARY + ";" +
                "-fx-font-size: 12px;" +
                "-fx-font-weight: bold;" +
                "-fx-cursor: hand;" +
                "-fx-padding: 6 18 6 18;"
        );
        signUpTab.setOnAction(e -> navigation.showSignUp());

        switchBox.getChildren().addAll(signInTab, signUpTab);
        return switchBox;
    }

    private void handleSignIn() {
        String email = emailField.getText();
        String password = passwordField.getText();

        AuthenticationService.AuthResult result = navigation.getAuthService().signIn(email, password);
        if (result.success()) {
            hideMessage();
            navigation.showDashboard();
        } else {
            showError(result.message());
        }
    }

    public void showSuccess(String message) {
        messageLabel.setText(message);
        messageLabel.setStyle(
                "-fx-font-size: 12px;" +
                "-fx-text-fill: " + Theme.COLOR_SUCCESS + ";" +
                "-fx-background-color: rgba(16, 185, 129, 0.12);" +
                "-fx-padding: 8 12 8 12;" +
                "-fx-background-radius: 6;"
        );
        messageLabel.setVisible(true);
        messageLabel.setManaged(true);
    }

    private void showError(String message) {
        messageLabel.setText(message);
        messageLabel.setStyle(
                "-fx-font-size: 12px;" +
                "-fx-text-fill: " + Theme.COLOR_ERROR + ";" +
                "-fx-background-color: rgba(248, 113, 113, 0.12);" +
                "-fx-padding: 8 12 8 12;" +
                "-fx-background-radius: 6;"
        );
        messageLabel.setVisible(true);
        messageLabel.setManaged(true);
    }

    private void hideMessage() {
        messageLabel.setVisible(false);
        messageLabel.setManaged(false);
    }
}
