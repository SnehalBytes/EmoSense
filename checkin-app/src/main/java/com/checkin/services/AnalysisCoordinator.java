package com.checkin.services;

import com.checkin.ml.EmotionPrediction;
import com.checkin.ml.ModelManager;
import com.checkin.ml.OnnxFacialEmotionModel;
import com.checkin.ml.OnnxTextEmotionModel;
import com.checkin.ml.OnnxTextTokenizer;
import com.checkin.model.AnalysisResult;
import com.checkin.model.CheckInData;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Coordinates check-in analysis across photo, text, and multimodal services.
 * Implements asynchronous background execution to ensure the JavaFX Application Thread
 * is never blocked during analysis.
 *
 * Supports both REAL ONNX neural pipelines and prototype MOCK engines.
 */
public class AnalysisCoordinator {

    private final PhotoAnalysisService photoService;
    private final TextAnalysisService textService;
    private final MultimodalAnalysisService multimodalService;
    private final ExecutorService executor;

    /**
     * Default constructor creates real ONNX model pipeline (or mock if mockMode explicitly enabled).
     */
    public AnalysisCoordinator() {
        this(ModelManager.getInstance().isMockMode() ?
                new MockPhotoAnalysisService() :
                new PhotoAnalysisService(new OnnxFacialEmotionModel(ModelManager.getInstance(), ModelManager.getInstance().getFacialConfig())),
             ModelManager.getInstance().isMockMode() ?
                new MockTextAnalysisService() :
                new TextAnalysisService(new OnnxTextEmotionModel(ModelManager.getInstance(), ModelManager.getInstance().getTextConfig(), new OnnxTextTokenizer(ModelManager.getInstance(), ModelManager.getInstance().getTextConfig()))),
             new MultimodalAnalysisService());
    }

    public AnalysisCoordinator(PhotoAnalysisService photoService,
                               TextAnalysisService textService,
                               MultimodalAnalysisService multimodalService) {
        this.photoService = photoService;
        this.textService = textService;
        this.multimodalService = multimodalService;
        this.executor = Executors.newFixedThreadPool(2, r -> {
            Thread t = new Thread(r, "analysis-worker");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Factory method to create real ONNX-backed analysis pipeline.
     */
    public static AnalysisCoordinator createReal(ModelManager modelManager) {
        ModelManager mm = modelManager != null ? modelManager : ModelManager.getInstance();
        return new AnalysisCoordinator(
                new PhotoAnalysisService(new OnnxFacialEmotionModel(mm, mm.getFacialConfig())),
                new TextAnalysisService(new OnnxTextEmotionModel(mm, mm.getTextConfig(), new OnnxTextTokenizer(mm, mm.getTextConfig()))),
                new MultimodalAnalysisService()
        );
    }

    /**
     * Factory method to create mock analysis pipeline for offline development.
     */
    public static AnalysisCoordinator createMock() {
        return new AnalysisCoordinator(new MockPhotoAnalysisService(), new MockTextAnalysisService(), new MultimodalAnalysisService());
    }

    /**
     * Executes the analysis pipeline synchronously for the provided check-in data.
     */
    public AnalysisResult analyze(CheckInData data) {
        EmotionPrediction facialPred = null;
        EmotionPrediction textPred = null;

        if (data.hasPhoto()) {
            try {
                facialPred = photoService.analyze(data.getPhotoFile());
            } catch (Exception e) {
                facialPred = EmotionPrediction.unavailable(EmotionPrediction.Modality.FACIAL, "Facial emotion analysis could not be completed: " + e.getMessage());
            }
        }

        if (data.hasThoughts()) {
            try {
                textPred = textService.analyze(data.getThoughtsText());
            } catch (Exception e) {
                textPred = EmotionPrediction.unavailable(EmotionPrediction.Modality.TEXTUAL, "Text emotion analysis could not be completed: " + e.getMessage());
            }
        }

        var combined = multimodalService.fuse(facialPred, textPred);

        String fLabel = facialPred != null ? facialPred.primaryEmotion() : null;
        double fConf = facialPred != null ? facialPred.confidence() : 0.0;

        String tLabel = textPred != null ? textPred.primaryEmotion() : null;
        double tConf = textPred != null ? textPred.confidence() : 0.0;

        return new AnalysisResult(
                fLabel, fConf,
                tLabel, tConf,
                combined.combinedLabel(), combined.combinedConfidence(),
                facialPred, textPred, combined
        );
    }

    /**
     * Executes the analysis asynchronously in a background thread.
     */
    public CompletableFuture<AnalysisResult> analyzeAsync(CheckInData data) {
        return CompletableFuture.supplyAsync(() -> analyze(data), executor);
    }

    public PhotoAnalysisService getPhotoService() {
        return photoService;
    }

    public TextAnalysisService getTextService() {
        return textService;
    }

    public MultimodalAnalysisService getMultimodalService() {
        return multimodalService;
    }
}
