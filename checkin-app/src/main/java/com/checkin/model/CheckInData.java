package com.checkin.model;

import java.io.File;

/**
 * Holds the state for a single "check-in": an optional photo and optional
 * free-text thoughts. Both belong to the SAME check-in and are analyzed
 * together when both are present.
 */
public class CheckInData {

    private File photoFile;
    private String thoughtsText = "";

    public boolean hasPhoto() {
        return photoFile != null;
    }

    public boolean hasThoughts() {
        return thoughtsText != null && !thoughtsText.trim().isEmpty();
    }

    public File getPhotoFile() {
        return photoFile;
    }

    public void setPhotoFile(File photoFile) {
        this.photoFile = photoFile;
    }

    public String getThoughtsText() {
        return thoughtsText;
    }

    public void setThoughtsText(String thoughtsText) {
        this.thoughtsText = thoughtsText == null ? "" : thoughtsText;
    }

    public void clearPhoto() {
        this.photoFile = null;
    }

    public void clearThoughts() {
        this.thoughtsText = "";
    }

    /** The analysis mode this check-in resolves to, based on available input. */
    public AnalysisMode resolveMode() {
        if (hasPhoto() && hasThoughts()) {
            return AnalysisMode.MULTIMODAL;
        } else if (hasPhoto()) {
            return AnalysisMode.PHOTO_ONLY;
        } else if (hasThoughts()) {
            return AnalysisMode.TEXT_ONLY;
        }
        return AnalysisMode.NONE;
    }

    public enum AnalysisMode {
        PHOTO_ONLY,
        TEXT_ONLY,
        MULTIMODAL,
        NONE
    }
}
