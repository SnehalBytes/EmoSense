package com.checkin.screens;

import com.checkin.Theme;
import com.checkin.services.AuthenticationService;
import com.checkin.utils.Navigation;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * Sign Up screen for EmoSense.
 * Allows new users to register an account with validation.
 */
public class SignUpScreen extends StackPane {

    private final Navigation navigation;
    private final TextField fullNameField = new TextField();
    private final TextField emailField = new TextField();
    private final PasswordField passwordField = new PasswordField();
    private final PasswordField confirmPasswordField = new PasswordField();
    private final Label messageLabel = new Label();

    public SignUpScreen(Navigation navigation) {
        this.navigation = navigation;
        getStyleClass().add("screen-root");
        setPadding(new Insets(30));

        VBox contentBox = new VBox(20);
        contentBox.setAlignment(Pos.CENTER);
        contentBox.setMaxWidth(460);

        // Header branding
        VBox brandingBox = buildBranding();

        // Auth Card
        VBox card = buildSignUpCard();

        contentBox.getChildren().addAll(brandingBox, card);
        getChildren().add(contentBox);
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

    private VBox buildSignUpCard() {
        VBox card = new VBox(14);
        card.setPadding(new Insets(28));
        card.setAlignment(Pos.TOP_LEFT);
        Theme.applyCardStyle(card);

        // Switch tabs at top
        HBox switchBox = buildSwitchTabs();

        Label cardTitle = new Label("Create Account");
        cardTitle.setStyle(
                "-fx-font-size: 20px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: " + Theme.COLOR_TEXT_PRIMARY + ";"
        );

        Label cardSubtitle = new Label("Start your emotional wellness journey with EmoSense.");
        cardSubtitle.setStyle(
                "-fx-font-size: 12px;" +
                "-fx-text-fill: " + Theme.COLOR_TEXT_SECONDARY + ";"
        );

        // Message Label
        messageLabel.setVisible(false);
        messageLabel.setManaged(false);
        messageLabel.setWrapText(true);

        // Full Name Field
        VBox nameBox = new VBox(5);
        Label nameLabel = new Label("Full Name");
        nameLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " + Theme.COLOR_TEXT_SECONDARY + ";");
        fullNameField.setPromptText("Jane Doe");
        Theme.applyInputField(fullNameField);
        nameBox.getChildren().addAll(nameLabel, fullNameField);

        // Email Field
        VBox emailBox = new VBox(5);
        Label emailLabel = new Label("Email Address");
        emailLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " + Theme.COLOR_TEXT_SECONDARY + ";");
        emailField.setPromptText("you@example.com");
        Theme.applyInputField(emailField);
        emailBox.getChildren().addAll(emailLabel, emailField);

        // Password Field
        VBox passwordBox = new VBox(5);
        Label passwordLabel = new Label("Password (min. 6 characters)");
        passwordLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " + Theme.COLOR_TEXT_SECONDARY + ";");
        passwordField.setPromptText("••••••••");
        Theme.applyInputField(passwordField);
        passwordBox.getChildren().addAll(passwordLabel, passwordField);

        // Confirm Password Field
        VBox confirmBox = new VBox(5);
        Label confirmLabel = new Label("Confirm Password");
        confirmLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " + Theme.COLOR_TEXT_SECONDARY + ";");
        confirmPasswordField.setPromptText("••••••••");
        Theme.applyInputField(confirmPasswordField);
        confirmBox.getChildren().addAll(confirmLabel, confirmPasswordField);

        // Enter key submits
        confirmPasswordField.setOnAction(e -> handleSignUp());

        // Create Account Button
        Button createAccountBtn = new Button("Create Account");
        createAccountBtn.setMaxWidth(Double.MAX_VALUE);
        Theme.applyPrimaryButton(createAccountBtn);
        createAccountBtn.setOnAction(e -> handleSignUp());

        // Back to Sign In
        HBox footer = new HBox(6);
        footer.setAlignment(Pos.CENTER);
        footer.setPadding(new Insets(6, 0, 0, 0));

        Label alreadyLabel = new Label("Already have an account?");
        alreadyLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: " + Theme.COLOR_TEXT_SECONDARY + ";");

        Hyperlink signInLink = new Hyperlink("Sign In");
        signInLink.setStyle(
                "-fx-font-size: 13px;" +
                "-fx-text-fill: " + Theme.COLOR_ACCENT + ";" +
                "-fx-font-weight: bold;" +
                "-fx-underline: false;"
        );
        signInLink.setOnAction(e -> navigation.showSignIn());
        footer.getChildren().addAll(alreadyLabel, signInLink);

        card.getChildren().addAll(
                switchBox,
                cardTitle,
                cardSubtitle,
                messageLabel,
                nameBox,
                emailBox,
                passwordBox,
                confirmBox,
                createAccountBtn,
                footer
        );
        return card;
    }

    private HBox buildSwitchTabs() {
        HBox switchBox = new HBox(8);
        switchBox.setAlignment(Pos.CENTER);
        switchBox.setPadding(new Insets(0, 0, 6, 0));

        Button signInTab = new Button("Sign In");
        signInTab.setStyle(
                "-fx-background-color: transparent;" +
                "-fx-text-fill: " + Theme.COLOR_TEXT_SECONDARY + ";" +
                "-fx-font-size: 12px;" +
                "-fx-font-weight: bold;" +
                "-fx-cursor: hand;" +
                "-fx-padding: 6 18 6 18;"
        );
        signInTab.setOnAction(e -> navigation.showSignIn());

        Button signUpTab = new Button("Create Account");
        signUpTab.setStyle(
                "-fx-background-color: " + Theme.COLOR_ACCENT + ";" +
                "-fx-text-fill: white;" +
                "-fx-background-radius: 6;" +
                "-fx-font-size: 12px;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 6 18 6 18;"
        );

        switchBox.getChildren().addAll(signInTab, signUpTab);
        return switchBox;
    }

    private void handleSignUp() {
        String name = fullNameField.getText();
        String email = emailField.getText();
        String pass = passwordField.getText();
        String confirm = confirmPasswordField.getText();

        AuthenticationService.AuthResult result = navigation.getAuthService().signUp(name, email, pass, confirm);
        if (result.success()) {
            SignInScreen signIn = new SignInScreen(navigation);
            signIn.showSuccess("Account created successfully! Please sign in with your password.");
            navigation.setScene(signIn);
        } else {
            showError(result.message());
        }
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
}
