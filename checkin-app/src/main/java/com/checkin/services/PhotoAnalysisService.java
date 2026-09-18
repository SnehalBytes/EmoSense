package com.checkin.services;

import com.checkin.ml.EmotionPrediction;
import com.checkin.ml.FacialEmotionModel;
import com.checkin.preprocessing.FacialImagePreprocessor;

import java.io.File;

/**
 * Service orchestrating facial photo preprocessing and inference execution.
 * Decoupled from the underlying model implementation (mock or real inference).
 */
public class PhotoAnalysisService {

    private final FacialImagePreprocessor preprocessor;
    private final FacialEmotionModel model;

    public PhotoAnalysisService(FacialEmotionModel model) {
        this(new FacialImagePreprocessor(), model);
    }

    public PhotoAnalysisService(FacialImagePreprocessor preprocessor, FacialEmotionModel model) {
        this.preprocessor = preprocessor;
        this.model = model;
    }

    public EmotionPrediction analyze(File photoFile) throws Exception {
        var preprocessed = preprocessor.prepareFacialImage(photoFile);

        EmotionPrediction prediction = model.predict(preprocessed);

        // DEV-ONLY Console Logging required by Task 3
        System.out.println("==================================================");
        System.out.println("[DEV LOG] FACIAL INFERENCE TRACE");
        System.out.println("==================================================");
        System.out.println("MODEL LOADED: " + (model != null && model.isTrainedModel()));
        System.out.println("MODEL NAME: " + (model != null ? model.getModelName() : "None"));
        System.out.println("MODEL INPUT SHAPE: " + (model != null && model.isTrainedModel() ? "[1, 1, 48, 48]" : "N/A (Mock/Prototype active)"));
        System.out.println("MODEL OUTPUT SHAPE: " + (model != null && model.isTrainedModel() ? "[1, 7]" : "N/A (Mock/Prototype active)"));
        System.out.println("RAW OUTPUT: " + (prediction != null ? prediction.probabilities() : "N/A"));
        System.out.println("PREDICTED INDEX: " + (model != null && model.isTrainedModel() ? "N/A" : "N/A (Mock/Prototype active)"));
        System.out.println("PREDICTED LABEL: " + (prediction != null ? prediction.primaryEmotion() : "None"));
        System.out.println("CONFIDENCE: " + (prediction != null ? prediction.confidence() + "%" : "0%"));
        System.out.println("PROTOTYPE MOCK: " + (prediction != null && prediction.isPrototypeMock()));
        System.out.println("==================================================");

        return prediction;
    }

    public FacialEmotionModel getModel() {
        return model;
    }

    public boolean isTrainedModel() {
        return model != null && model.isTrainedModel();
    }
}
