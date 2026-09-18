package com.checkin.services;

import com.checkin.ml.EmotionPrediction;
import com.checkin.ml.TextEmotionModel;
import com.checkin.preprocessing.TextPreprocessor;

/**
 * Service orchestrating textual thoughts preprocessing and inference execution.
 * Decoupled from the underlying model implementation (mock or real inference).
 */
public class TextAnalysisService {

    private final TextPreprocessor preprocessor;
    private final TextEmotionModel model;

    public TextAnalysisService(TextEmotionModel model) {
        this(new TextPreprocessor(), model);
    }

    public TextAnalysisService(TextPreprocessor preprocessor, TextEmotionModel model) {
        this.preprocessor = preprocessor;
        this.model = model;
    }

    public EmotionPrediction analyze(String thoughtsText) throws Exception {
        var preprocessed = preprocessor.process(thoughtsText);
        EmotionPrediction prediction = model.predict(preprocessed);

        // DEV-ONLY Console Logging required by Task 3
        System.out.println("==================================================");
        System.out.println("[DEV LOG] TEXT INFERENCE TRACE");
        System.out.println("==================================================");
        System.out.println("MODEL LOADED: " + (model != null && model.isTrainedModel()));
        System.out.println("MODEL NAME: " + (model != null ? model.getModelName() : "None"));
        System.out.println("MODEL INPUT SHAPE: " + (model != null && model.isTrainedModel() ? "[1, seq_len]" : "N/A (Mock/Prototype active)"));
        System.out.println("MODEL OUTPUT SHAPE: " + (model != null && model.isTrainedModel() ? "[1, 28]" : "N/A (Mock/Prototype active)"));
        System.out.println("RAW OUTPUT: " + (prediction != null ? prediction.probabilities() : "N/A"));
        System.out.println("PREDICTED INDEX: " + (model != null && model.isTrainedModel() ? "N/A" : "N/A (Mock/Prototype active)"));
        System.out.println("PREDICTED LABEL: " + (prediction != null ? prediction.primaryEmotion() : "None"));
        System.out.println("CONFIDENCE: " + (prediction != null ? prediction.confidence() + "%" : "0%"));
        System.out.println("PROTOTYPE MOCK: " + (prediction != null && prediction.isPrototypeMock()));
        System.out.println("==================================================");

        return prediction;
    }

    public TextEmotionModel getModel() {
        return model;
    }

    public boolean isTrainedModel() {
        return model != null && model.isTrainedModel();
    }
}
