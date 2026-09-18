package com.checkin.ml;

/**
 * Base marker interface for emotion classification models.
 */
public interface EmotionModel {
    String getModelName();
    boolean isTrainedModel();
}
