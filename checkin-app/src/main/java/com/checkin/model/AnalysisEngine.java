package com.checkin.model;

import com.checkin.ml.ModelManager;
import com.checkin.services.AnalysisCoordinator;

/**
 * AnalysisEngine adapter that delegates to the modular AnalysisCoordinator.
 * Configured to connect to the real ONNX neural model pipeline by default.
 * If model files are absent, reports UNAVAILABLE without falling back to Neutral.
 */
public class AnalysisEngine {

    private final AnalysisCoordinator coordinator;

    public AnalysisEngine() {
        this(createDefaultCoordinator());
    }

    public AnalysisEngine(AnalysisCoordinator coordinator) {
        this.coordinator = coordinator != null ? coordinator : createDefaultCoordinator();
    }

    private static AnalysisCoordinator createDefaultCoordinator() {
        ModelManager mm = ModelManager.getInstance();
        if (mm.isMockMode()) {
            return AnalysisCoordinator.createMock();
        }
        return AnalysisCoordinator.createReal(mm);
    }

    /**
     * Analyzes check-in data through photo, text, and multimodal services.
     */
    public AnalysisResult analyze(CheckInData data) {
        return coordinator.analyze(data);
    }

    public AnalysisCoordinator getCoordinator() {
        return coordinator;
    }
}
