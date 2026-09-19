package com.checkin;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

/**
 * Centralized theme constants and styling helpers for EmoSense.
 * Adheres to a dark navy / charcoal AI-wellness aesthetic:
 * - Background: #0c0e17 (deep navy charcoal)
 * - Cards: #161a29 (slightly lighter dark charcoal)
 * - Card border: #232a3e (subtle dark blue/gray)
 * - Accent: #635bff (indigo/purple)
 * - Text primary: #f1f3f9, secondary: #8e98b0, muted: #64748b
 */
public final class Theme {

    private Theme() {}

    // Color constants
    public static final String COLOR_BG = "#0c0e17";
    public static final String COLOR_CARD_BG = "#161a29";
    public static final String COLOR_CARD_BORDER = "#232a3e";
    public static final String COLOR_DROPZONE_BG = "#111422";
    public static final String COLOR_DROPZONE_BORDER = "#2f3750";
    public static final String COLOR_DROPZONE_ACTIVE_BG = "#191d32";
    public static final String COLOR_DROPZONE_ACTIVE_BORDER = "#635bff";

    public static final String COLOR_ACCENT = "#635bff";
    public static final String COLOR_ACCENT_HOVER = "#756efc";
    public static final String COLOR_ACCENT_PRESSED = "#5147df";

    public static final String COLOR_SECONDARY_BTN = "#20263b";
    public static final String COLOR_SECONDARY_BTN_HOVER = "#2a324d";
    public static final String COLOR_SECONDARY_BTN_TEXT = "#c7d2fe";

    public static final String COLOR_DANGER_BG = "#351a23";
    public static final String COLOR_DANGER_TEXT = "#f87171";

    public static final String COLOR_TEXT_PRIMARY = "#f1f3f9";
    public static final String COLOR_TEXT_SECONDARY = "#8e98b0";
    public static final String COLOR_TEXT_MUTED = "#64748b";

    public static final String COLOR_INPUT_BG = "#101320";
    public static final String COLOR_INPUT_BORDER = "#262d42";
    public static final String COLOR_INPUT_FOCUS_BORDER = "#635bff";

    public static final String COLOR_SUCCESS = "#10b981";
    public static final String COLOR_ERROR = "#f87171";

    // Standardized typography snippets
    public static final String FONT_FAMILY = "'Segoe UI', -apple-system, BlinkMacSystemFont, Roboto, sans-serif";
    public static final String STYLE_PAGE_TITLE = "-fx-font-family: " + FONT_FAMILY + "; -fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: " + COLOR_TEXT_PRIMARY + ";";
    public static final String STYLE_PAGE_SUBTITLE = "-fx-font-family: " + FONT_FAMILY + "; -fx-font-size: 14px; -fx-text-fill: " + COLOR_TEXT_SECONDARY + ";";
    public static final String STYLE_SECTION_TITLE = "-fx-font-family: " + FONT_FAMILY + "; -fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: " + COLOR_TEXT_PRIMARY + ";";
    public static final String STYLE_BODY = "-fx-font-family: " + FONT_FAMILY + "; -fx-font-size: 13.5px; -fx-text-fill: " + COLOR_TEXT_PRIMARY + ";";
    public static final String STYLE_MUTED = "-fx-font-family: " + FONT_FAMILY + "; -fx-font-size: 12px; -fx-text-fill: " + COLOR_TEXT_MUTED + ";";

    // Style snippet helpers
    public static void applyCardStyle(Region region) {
        region.setStyle(
                "-fx-background-color: " + COLOR_CARD_BG + ";" +
                "-fx-background-radius: 14;" +
                "-fx-border-radius: 14;" +
                "-fx-border-color: " + COLOR_CARD_BORDER + ";" +
                "-fx-border-width: 1;" +
                "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.35), 16, 0, 0, 4);"
        );
    }

    public static void applyInteractiveCardStyle(Region region) {
        applyCardStyle(region);
        region.setOnMouseEntered(e -> region.setStyle(
                "-fx-background-color: #1a1f33;" +
                "-fx-background-radius: 14;" +
                "-fx-border-radius: 14;" +
                "-fx-border-color: #3b4668;" +
                "-fx-border-width: 1;" +
                "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.45), 18, 0, 0, 6);"
        ));
        region.setOnMouseExited(e -> applyCardStyle(region));
    }

    public static void applyInputField(TextField field) {
        field.setStyle(
                "-fx-background-color: " + COLOR_INPUT_BG + ";" +
                "-fx-text-fill: " + COLOR_TEXT_PRIMARY + ";" +
                "-fx-prompt-text-fill: " + COLOR_TEXT_MUTED + ";" +
                "-fx-background-radius: 8;" +
                "-fx-border-radius: 8;" +
                "-fx-border-color: " + COLOR_INPUT_BORDER + ";" +
                "-fx-border-width: 1;" +
                "-fx-padding: 10 14 10 14;" +
                "-fx-font-size: 14px;"
        );
        field.focusedProperty().addListener((obs, was, isNow) -> {
            String border = isNow ? COLOR_INPUT_FOCUS_BORDER : COLOR_INPUT_BORDER;
            field.setStyle(
                    "-fx-background-color: " + COLOR_INPUT_BG + ";" +
                    "-fx-text-fill: " + COLOR_TEXT_PRIMARY + ";" +
                    "-fx-prompt-text-fill: " + COLOR_TEXT_MUTED + ";" +
                    "-fx-background-radius: 8;" +
                    "-fx-border-radius: 8;" +
                    "-fx-border-color: " + border + ";" +
                    "-fx-border-width: 1;" +
                    "-fx-padding: 10 14 10 14;" +
                    "-fx-font-size: 14px;"
            );
        });
    }

    public static void applyPrimaryButton(Button button) {
        String base =
                "-fx-background-color: " + COLOR_ACCENT + ";" +
                "-fx-text-fill: #ffffff;" +
                "-fx-background-radius: 8;" +
                "-fx-font-size: 14px;" +
                "-fx-font-weight: bold;" +
                "-fx-cursor: hand;" +
                "-fx-padding: 11 22 11 22;";
        button.setStyle(base);
        button.setOnMouseEntered(e -> button.setStyle(
                "-fx-background-color: " + COLOR_ACCENT_HOVER + ";" +
                "-fx-text-fill: #ffffff;" +
                "-fx-background-radius: 8;" +
                "-fx-font-size: 14px;" +
                "-fx-font-weight: bold;" +
                "-fx-cursor: hand;" +
                "-fx-padding: 11 22 11 22;"
        ));
        button.setOnMouseExited(e -> button.setStyle(base));
    }

    public static void applySecondaryButton(Button button) {
        String base =
                "-fx-background-color: " + COLOR_SECONDARY_BTN + ";" +
                "-fx-text-fill: " + COLOR_SECONDARY_BTN_TEXT + ";" +
                "-fx-background-radius: 8;" +
                "-fx-font-size: 13px;" +
                "-fx-font-weight: bold;" +
                "-fx-cursor: hand;" +
                "-fx-padding: 8 16 8 16;";
        button.setStyle(base);
        button.setOnMouseEntered(e -> button.setStyle(
                "-fx-background-color: " + COLOR_SECONDARY_BTN_HOVER + ";" +
                "-fx-text-fill: " + COLOR_SECONDARY_BTN_TEXT + ";" +
                "-fx-background-radius: 8;" +
                "-fx-font-size: 13px;" +
                "-fx-font-weight: bold;" +
                "-fx-cursor: hand;" +
                "-fx-padding: 8 16 8 16;"
        ));
        button.setOnMouseExited(e -> button.setStyle(base));
    }

    public static void applyDangerButton(Button button) {
        String base =
                "-fx-background-color: " + COLOR_DANGER_BG + ";" +
                "-fx-text-fill: " + COLOR_DANGER_TEXT + ";" +
                "-fx-background-radius: 8;" +
                "-fx-font-size: 13px;" +
                "-fx-font-weight: bold;" +
                "-fx-cursor: hand;" +
                "-fx-padding: 8 16 8 16;";
        button.setStyle(base);
        button.setOnMouseEntered(e -> button.setStyle(
                "-fx-background-color: #481e2b;" +
                "-fx-text-fill: #fca5a5;" +
                "-fx-background-radius: 8;" +
                "-fx-font-size: 13px;" +
                "-fx-font-weight: bold;" +
                "-fx-cursor: hand;" +
                "-fx-padding: 8 16 8 16;"
        ));
        button.setOnMouseExited(e -> button.setStyle(base));
    }
}
