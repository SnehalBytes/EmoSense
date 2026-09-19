package com.checkin;

import com.checkin.dao.InMemoryAnalysisResultDAO;
import com.checkin.dao.InMemoryCheckInDAO;
import com.checkin.model.CheckInRecord;
import com.checkin.model.User;
import com.checkin.screens.*;
import com.checkin.services.AuthenticationService;
import com.checkin.services.HistoryService;
import com.checkin.services.InsightsService;
import com.checkin.services.UserSession;
import com.checkin.utils.Navigation;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for Phase 5A: Supporting Screens + Navigation.
 * Verifies ProfileScreen, SettingsScreen, PrivacyScreen, TechnologyScreen,
 * navigation transitions, back button actions, and session clearing on logout.
 */
public class Phase5ASupportingScreensTest {

    private InMemoryCheckInDAO checkInDAO;
    private InMemoryAnalysisResultDAO resultDAO;
    private HistoryService historyService;

    @BeforeAll
    static void initJavaFx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // JavaFX already initialized
        }
    }

    @BeforeEach
    void setUp() {
        UserSession.getInstance().logout();
        checkInDAO = new InMemoryCheckInDAO();
        resultDAO = new InMemoryAnalysisResultDAO();
        historyService = new HistoryService(checkInDAO, resultDAO);
    }

    @AfterEach
    void tearDown() {
        UserSession.getInstance().logout();
    }

    // =========================================================================
    // 1. ProfileScreen Tests
    // =========================================================================

    @Test
    @DisplayName("1. ProfileScreen displays authenticated user information without fake data")
    void testProfileScreenWithAuthenticatedUser() {
        LocalDateTime created = LocalDateTime.of(2026, 1, 15, 10, 30);
        User user = new User("usr-123", "Alex Morgan", "alex@emosense.test", "hash123", created);
        UserSession.getInstance().login(user);

        // Add 2 check-ins for this user
        checkInDAO.save(new CheckInRecord("chk-1", "usr-123", "photo1.jpg", "Thoughts 1",
                "Happy", 95.0, "Joy", 90.0, "Happy Signal", 92.5, LocalDateTime.now()));
        checkInDAO.save(new CheckInRecord("chk-2", "usr-123", null, "Thoughts 2",
                null, 0.0, "Calm", 88.0, null, 0.0, LocalDateTime.now()));

        ProfileScreen screen = new ProfileScreen(null, historyService, null);
        List<String> labels = collectAllLabelTexts(screen);

        assertTrue(labels.contains("Alex Morgan"), "Profile must display user full name");
        assertTrue(labels.contains("alex@emosense.test"), "Profile must display user email");
        assertTrue(labels.contains("usr-123"), "Profile must display user ID");
        assertTrue(labels.contains("AM"), "Avatar must display user initials");
        assertTrue(labels.contains("2"), "Profile must display genuine check-in count");
        assertTrue(labels.contains("Active Contributor"), "Status must be active contributor");
    }

    @Test
    @DisplayName("2. ProfileScreen gracefully handles unauthenticated/guest session")
    void testProfileScreenUnauthenticatedSession() {
        // No user logged in
        ProfileScreen screen = new ProfileScreen(null, historyService, null);
        List<String> labels = collectAllLabelTexts(screen);

        assertNotNull(screen);
        assertTrue(labels.contains("● Guest Session") || labels.contains("Active Member"),
                "Unauthenticated session should render neutral indicator");
        assertFalse(labels.contains("fake@user.com"), "Must not invent fake emails");
    }

    @Test
    @DisplayName("3. ProfileScreen back button executes back action")
    void testProfileScreenBackNavigation() {
        AtomicBoolean backCalled = new AtomicBoolean(false);
        ProfileScreen screen = new ProfileScreen(null, historyService, () -> backCalled.set(true));

        List<Button> buttons = collectAllButtons(screen);
        Button backBtn = buttons.stream()
                .filter(b -> b.getText() != null && b.getText().contains("Back"))
                .findFirst()
                .orElse(null);

        assertNotNull(backBtn, "ProfileScreen must have a Back button");
        backBtn.fire();
        assertTrue(backCalled.get(), "Back button must execute the onBack runnable");
    }

    @Test
    @DisplayName("4. ProfileScreen Sign Out invokes Navigation signOut and clears UserSession")
    void testProfileScreenSignOut() {
        User user = new User("usr-logout", "Test User", "test@emosense.test", "hash");
        UserSession.getInstance().login(user);
        assertTrue(UserSession.getInstance().isLoggedIn());

        AtomicBoolean signOutInvoked = new AtomicBoolean(false);
        Navigation mockNav = new Navigation(null, new AuthenticationService()) {
            @Override
            public void signOut() {
                signOutInvoked.set(true);
                UserSession.getInstance().logout();
            }
        };

        ProfileScreen screen = new ProfileScreen(mockNav, historyService, null);
        List<Button> buttons = collectAllButtons(screen);
        Button signOutBtn = buttons.stream()
                .filter(b -> b.getText() != null && b.getText().contains("Sign Out"))
                .findFirst()
                .orElse(null);

        assertNotNull(signOutBtn, "ProfileScreen must have a Sign Out button");
        signOutBtn.fire();

        assertTrue(signOutInvoked.get(), "Sign out button must invoke navigation.signOut()");
        assertFalse(UserSession.getInstance().isLoggedIn(), "UserSession must be cleared after sign out");
        assertNull(UserSession.getInstance().getCurrentUser(), "Current user must be null");
    }

    // =========================================================================
    // 2. SettingsScreen Tests
    // =========================================================================

    @Test
    @DisplayName("5. SettingsScreen renders Appearance, Notifications, Privacy, Technology, and Account sections")
    void testSettingsScreenSections() {
        User user = new User("usr-settings", "Settings Tester", "settings@emosense.test", "pass");
        UserSession.getInstance().login(user);

        SettingsScreen screen = new SettingsScreen(null, null);
        List<String> labels = collectAllLabelTexts(screen);

        assertTrue(labels.contains("APPEARANCE"), "Must contain Appearance section");
        assertTrue(labels.contains("NOTIFICATIONS & REMINDERS"), "Must contain Notifications section");
        assertTrue(labels.contains("PRIVACY & DATA HANDLING"), "Must contain Privacy section");
        assertTrue(labels.contains("APPLICATION & ABOUT"), "Must contain Application section");
        assertTrue(labels.contains("ACCOUNT & SESSION"), "Must contain Account section");
        assertTrue(labels.stream().anyMatch(l -> l.contains("Dark Theme")), "Must display dark theme status");
        assertTrue(labels.stream().anyMatch(l -> l.contains("Not Configured in Prototype")), "Must honestly report push notification status");
    }

    @Test
    @DisplayName("6. SettingsScreen navigates to Privacy, Technology, and Profile screens")
    void testSettingsScreenNavigationLinks() {
        AtomicBoolean privacyNav = new AtomicBoolean(false);
        AtomicBoolean techNav = new AtomicBoolean(false);
        AtomicBoolean profileNav = new AtomicBoolean(false);

        Navigation mockNav = new Navigation(null, new AuthenticationService()) {
            @Override
            public void showPrivacy(Runnable onBack) {
                privacyNav.set(true);
            }
            @Override
            public void showTechnology(Runnable onBack) {
                techNav.set(true);
            }
            @Override
            public void showProfile(Runnable onBack) {
                profileNav.set(true);
            }
        };

        SettingsScreen screen = new SettingsScreen(mockNav, null);
        List<Button> buttons = collectAllButtons(screen);

        Button privacyBtn = buttons.stream()
                .filter(b -> b.getText() != null && b.getText().contains("Privacy"))
                .findFirst()
                .orElse(null);
        assertNotNull(privacyBtn, "Must have Privacy button");
        privacyBtn.fire();
        assertTrue(privacyNav.get(), "Must navigate to Privacy");

        Button techBtn = buttons.stream()
                .filter(b -> b.getText() != null && (b.getText().contains("Technology") || b.getText().contains("About")))
                .findFirst()
                .orElse(null);
        assertNotNull(techBtn, "Must have Technology button");
        techBtn.fire();
        assertTrue(techNav.get(), "Must navigate to Technology");

        Button profileBtn = buttons.stream()
                .filter(b -> b.getText() != null && b.getText().contains("Profile"))
                .findFirst()
                .orElse(null);
        assertNotNull(profileBtn, "Must have Profile button");
        profileBtn.fire();
        assertTrue(profileNav.get(), "Must navigate to Profile");
    }

    @Test
    @DisplayName("7. SettingsScreen Back button executes onBack action")
    void testSettingsScreenBackNavigation() {
        AtomicBoolean backCalled = new AtomicBoolean(false);
        SettingsScreen screen = new SettingsScreen(null, () -> backCalled.set(true));

        List<Button> buttons = collectAllButtons(screen);
        Button backBtn = buttons.stream()
                .filter(b -> b.getText() != null && b.getText().contains("Back"))
                .findFirst()
                .orElse(null);

        assertNotNull(backBtn);
        backBtn.fire();
        assertTrue(backCalled.get(), "Back button must execute the onBack runnable");
    }

    // =========================================================================
    // 3. PrivacyScreen Tests
    // =========================================================================

    @Test
    @DisplayName("8. PrivacyScreen renders non-clinical disclaimer, academic purpose, and probabilistic notices")
    void testPrivacyScreenContent() {
        PrivacyScreen screen = new PrivacyScreen(null, null);
        List<String> labels = collectAllLabelTexts(screen);

        assertTrue(labels.contains("Privacy & Data Practices"), "Must contain title");
        assertTrue(labels.stream().anyMatch(l -> l.contains("NOT a medical") || l.contains("NON-MEDICAL")),
                "Must clearly display non-medical notice");
        assertTrue(labels.stream().anyMatch(l -> l.contains("B.Tech") || l.contains("Academic Prototype")),
                "Must state academic prototype scope");
        assertTrue(labels.stream().anyMatch(l -> l.contains("probabilistic")),
                "Must explain probabilistic nature of model outputs");
        assertTrue(labels.stream().anyMatch(l -> l.contains("voluntary")),
                "Must state voluntary nature of photo/text inputs");
    }

    @Test
    @DisplayName("9. PrivacyScreen Back button executes onBack action")
    void testPrivacyScreenBackNavigation() {
        AtomicBoolean backCalled = new AtomicBoolean(false);
        PrivacyScreen screen = new PrivacyScreen(null, () -> backCalled.set(true));

        List<Button> buttons = collectAllButtons(screen);
        Button backBtn = buttons.stream()
                .filter(b -> b.getText() != null && b.getText().contains("Back"))
                .findFirst()
                .orElse(null);

        assertNotNull(backBtn);
        backBtn.fire();
        assertTrue(backCalled.get(), "Back button must execute the onBack runnable");
    }

    // =========================================================================
    // 4. TechnologyScreen Tests
    // =========================================================================

    @Test
    @DisplayName("10. TechnologyScreen renders title, subtitle, multimodal capabilities, and tech stack")
    void testTechnologyScreenContent() {
        TechnologyScreen screen = new TechnologyScreen(null, null);
        List<String> labels = collectAllLabelTexts(screen);

        assertTrue(labels.contains("EmoSense"), "Must contain title EmoSense");
        assertTrue(labels.contains("Understanding the emotions we don't always express."),
                "Must contain exact subtitle");
        assertTrue(labels.contains("FACIAL SIGNAL ANALYSIS"), "Must explain Facial Signal Analysis");
        assertTrue(labels.contains("TEXTUAL SIGNAL ANALYSIS"), "Must explain Textual Signal Analysis");
        assertTrue(labels.contains("MULTIMODAL INSIGHT"), "Must explain Multimodal Insight");
        assertTrue(labels.contains("CHECK-IN HISTORY & INSIGHTS"), "Must explain History");
        assertTrue(labels.contains("AI COMPANION"), "Must explain AI Companion");
        assertTrue(labels.stream().anyMatch(l -> l.contains("Java 17")), "Must list Java 17 in stack");
        assertTrue(labels.stream().anyMatch(l -> l.contains("ONNX Runtime")), "Must list ONNX Runtime in stack");
        assertTrue(labels.stream().anyMatch(l -> l.contains("B.Tech")), "Must mention B.Tech academic scope");
    }

    @Test
    @DisplayName("11. TechnologyScreen Back button executes onBack action")
    void testTechnologyScreenBackNavigation() {
        AtomicBoolean backCalled = new AtomicBoolean(false);
        TechnologyScreen screen = new TechnologyScreen(null, () -> backCalled.set(true));

        List<Button> buttons = collectAllButtons(screen);
        Button backBtn = buttons.stream()
                .filter(b -> b.getText() != null && b.getText().contains("Back"))
                .findFirst()
                .orElse(null);

        assertNotNull(backBtn);
        backBtn.fire();
        assertTrue(backCalled.get(), "Back button must execute the onBack runnable");
    }

    // =========================================================================
    // 5. Dashboard Navbar Navigation Tests
    // =========================================================================

    @Test
    @DisplayName("12. Dashboard navbar exposes Profile, Settings, Privacy, and About buttons")
    void testDashboardNavbarNavigation() {
        User user = new User("usr-dash", "Nav User", "nav@emosense.test", "pass");
        UserSession.getInstance().login(user);

        AtomicBoolean profileNav = new AtomicBoolean(false);
        AtomicBoolean settingsNav = new AtomicBoolean(false);
        AtomicBoolean privacyNav = new AtomicBoolean(false);
        AtomicBoolean aboutNav = new AtomicBoolean(false);
        AtomicBoolean signOutNav = new AtomicBoolean(false);

        Navigation mockNav = new Navigation(null, new AuthenticationService()) {
            @Override
            public void showProfile() {
                profileNav.set(true);
            }
            @Override
            public void showSettings() {
                settingsNav.set(true);
            }
            @Override
            public void showPrivacy() {
                privacyNav.set(true);
            }
            @Override
            public void showTechnology() {
                aboutNav.set(true);
            }
            @Override
            public void signOut() {
                signOutNav.set(true);
            }
        };

        DashboardScreen screen = new DashboardScreen(mockNav, historyService, new InsightsService(historyService));
        List<Button> buttons = collectAllButtons(screen);

        Button profileBtn = buttons.stream().filter(b -> "Profile".equals(b.getText())).findFirst().orElse(null);
        assertNotNull(profileBtn, "Navbar must contain Profile button");
        profileBtn.fire();
        assertTrue(profileNav.get(), "Profile button must invoke showProfile()");

        Button settingsBtn = buttons.stream().filter(b -> "Settings".equals(b.getText())).findFirst().orElse(null);
        assertNotNull(settingsBtn, "Navbar must contain Settings button");
        settingsBtn.fire();
        assertTrue(settingsNav.get(), "Settings button must invoke showSettings()");

        Button privacyBtn = buttons.stream().filter(b -> "Privacy".equals(b.getText())).findFirst().orElse(null);
        assertNotNull(privacyBtn, "Navbar must contain Privacy button");
        privacyBtn.fire();
        assertTrue(privacyNav.get(), "Privacy button must invoke showPrivacy()");

        Button aboutBtn = buttons.stream().filter(b -> "About".equals(b.getText())).findFirst().orElse(null);
        assertNotNull(aboutBtn, "Navbar must contain About button");
        aboutBtn.fire();
        assertTrue(aboutNav.get(), "About button must invoke showTechnology()");

        Button signOutBtn = buttons.stream().filter(b -> "Sign Out".equals(b.getText())).findFirst().orElse(null);
        assertNotNull(signOutBtn, "Navbar must contain Sign Out button");
        signOutBtn.fire();
        assertTrue(signOutNav.get(), "Sign Out button must invoke signOut()");
    }

    // =========================================================================
    // 6. Navigation Method Instantiation Tests
    // =========================================================================

    @Test
    @DisplayName("13. Navigation showProfile, showSettings, showPrivacy, showTechnology methods construct properly")
    void testNavigationMethodsInstantiation() {
        AtomicReference<Parent> lastScene = new AtomicReference<>();
        Navigation testNav = new Navigation(null, new AuthenticationService()) {
            @Override
            public void setScene(Parent root) {
                lastScene.set(root);
            }
        };

        testNav.showProfile();
        assertTrue(lastScene.get() instanceof ProfileScreen, "showProfile must create ProfileScreen");

        testNav.showSettings();
        assertTrue(lastScene.get() instanceof SettingsScreen, "showSettings must create SettingsScreen");

        testNav.showPrivacy();
        assertTrue(lastScene.get() instanceof PrivacyScreen, "showPrivacy must create PrivacyScreen");

        testNav.showTechnology();
        assertTrue(lastScene.get() instanceof TechnologyScreen, "showTechnology must create TechnologyScreen");
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private List<String> collectAllLabelTexts(Node root) {
        List<String> list = new ArrayList<>();
        collectLabelsRecursive(root, list);
        return list;
    }

    private void collectLabelsRecursive(Node node, List<String> list) {
        if (node instanceof Label l && l.getText() != null) {
            list.add(l.getText());
        }
        if (node instanceof ScrollPane sp && sp.getContent() != null) {
            collectLabelsRecursive(sp.getContent(), list);
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                collectLabelsRecursive(child, list);
            }
        }
    }

    private List<Button> collectAllButtons(Node root) {
        List<Button> list = new ArrayList<>();
        collectButtonsRecursive(root, list);
        return list;
    }

    private void collectButtonsRecursive(Node node, List<Button> list) {
        if (node instanceof Button b) {
            list.add(b);
        }
        if (node instanceof ScrollPane sp && sp.getContent() != null) {
            collectButtonsRecursive(sp.getContent(), list);
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                collectButtonsRecursive(child, list);
            }
        }
    }
}
