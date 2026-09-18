package com.checkin.ml;

import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import com.checkin.ml.EmotionPrediction.ModelStatus;
import com.checkin.ml.config.FaceDetectorConfig;
import com.checkin.ml.config.FacialModelConfig;
import com.checkin.ml.config.TextModelConfig;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * Centralized Model Manager responsible for:
 * 1. Locating model files on disk (supporting multiple relative and configurable roots).
 * 2. Verifying genuine model file existence and size.
 * 3. Safely managing ONNX Runtime environment and session lifecycles.
 * 4. Transparently exposing availability status: REAL, MOCK, or UNAVAILABLE.
 * 5. Safely closing resources upon application shutdown.
 */
public class ModelManager implements AutoCloseable {

    private static ModelManager instance;

    private final FacialModelConfig facialConfig;
    private final TextModelConfig textConfig;
    private final FaceDetectorConfig faceDetectorConfig;

    private OrtEnvironment environment;
    private OrtSession facialSession;
    private OrtSession textSession;
    private OrtSession faceDetectorSession;

    private boolean mockMode = false;

    public ModelManager() {
        this(new FacialModelConfig(), new TextModelConfig(), new FaceDetectorConfig());
    }

    public ModelManager(FacialModelConfig facialConfig, TextModelConfig textConfig, FaceDetectorConfig faceDetectorConfig) {
        this.facialConfig = facialConfig != null ? facialConfig : new FacialModelConfig();
        this.textConfig = textConfig != null ? textConfig : new TextModelConfig();
        this.faceDetectorConfig = faceDetectorConfig != null ? faceDetectorConfig : new FaceDetectorConfig();
        initializeEnvironment();
    }

    public static synchronized ModelManager getInstance() {
        if (instance == null) {
            instance = new ModelManager();
        }
        return instance;
    }

    public static synchronized void resetInstance() {
        if (instance != null) {
            try {
                instance.close();
            } catch (Exception ignored) {
            }
            instance = null;
        }
    }

    private void initializeEnvironment() {
        try {
            this.environment = OrtEnvironment.getEnvironment("EmoSenseOrtEnv");
        } catch (Exception e) {
            System.err.println("[ModelManager] Warning: Failed to initialize OrtEnvironment: " + e.getMessage());
        }
    }

    /**
     * Resolves and verifies that a model file exists on disk and is non-empty.
     */
    public Optional<File> locateModelFile(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return Optional.empty();
        }

        // 1. Direct or absolute path
        File direct = new File(relativePath);
        if (direct.isAbsolute() && direct.exists() && direct.isFile() && direct.length() > 0) {
            return Optional.of(direct);
        }

        // 2. Custom system property or environment variable
        String customDir = System.getProperty("emosense.models.dir", System.getenv("EMOSENSE_MODELS_DIR"));
        if (customDir != null && !customDir.isBlank()) {
            File custom = new File(customDir, relativePath);
            if (custom.exists() && custom.isFile() && custom.length() > 0) {
                return Optional.of(custom);
            }
        }

        // 3. Search common search roots
        String[] candidateRoots = new String[]{
                "",                         // current working directory
                "checkin-app",              // if running from workspace root
                "..",                       // if running from subdirectory
                "../checkin-app"
        };

        for (String root : candidateRoots) {
            Path candidatePath = root.isEmpty() ? Paths.get(relativePath) : Paths.get(root, relativePath);
            File candidateFile = candidatePath.toFile();
            if (candidateFile.exists() && candidateFile.isFile() && candidateFile.length() > 0) {
                return Optional.of(candidateFile);
            }
        }

        return Optional.empty();
    }

    // =========================================================================
    // FACIAL EMOTION MODEL
    // =========================================================================

    public synchronized ModelStatus getFacialModelStatus() {
        if (mockMode) {
            return ModelStatus.MOCK;
        }

        Optional<File> modelFile = locateModelFile(facialConfig.getModelPath());
        if (modelFile.isEmpty()) {
            modelFile = locateModelFile(FacialModelConfig.FALLBACK_MODEL_PATH);
        }
        if (modelFile.isEmpty()) {
            return ModelStatus.UNAVAILABLE;
        }

        if (facialSession != null) {
            return ModelStatus.REAL;
        }

        // Attempt loading session for detected file
        try {
            ensureEnvironment();
            if (environment != null) {
                this.facialSession = environment.createSession(modelFile.get().getAbsolutePath());
                System.out.println("[ModelManager] Successfully loaded REAL Facial ONNX model from: " + modelFile.get().getAbsolutePath());
                return ModelStatus.REAL;
            }
        } catch (OrtException e) {
            System.err.println("[ModelManager] Error initializing Facial ONNX Session from " + modelFile.get().getAbsolutePath() + ": " + e.getMessage());
        }

        return ModelStatus.UNAVAILABLE;
    }

    public boolean isFacialModelAvailable() {
        return getFacialModelStatus() == ModelStatus.REAL;
    }

    public synchronized OrtSession getFacialSession() {
        if (facialSession == null) {
            getFacialModelStatus(); // triggers session loading if file is present
        }
        return facialSession;
    }

    // =========================================================================
    // TEXT EMOTION MODEL
    // =========================================================================

    public synchronized ModelStatus getTextModelStatus() {
        if (mockMode) {
            return ModelStatus.MOCK;
        }

        Optional<File> modelFile = locateModelFile(textConfig.getModelPath());
        if (modelFile.isEmpty()) {
            return ModelStatus.UNAVAILABLE;
        }

        if (textSession != null) {
            return ModelStatus.REAL;
        }

        // Attempt loading session for detected file
        try {
            ensureEnvironment();
            if (environment != null) {
                this.textSession = environment.createSession(modelFile.get().getAbsolutePath());
                System.out.println("[ModelManager] Successfully loaded REAL Text ONNX model from: " + modelFile.get().getAbsolutePath());
                return ModelStatus.REAL;
            }
        } catch (OrtException e) {
            System.err.println("[ModelManager] Error initializing Text ONNX Session from " + modelFile.get().getAbsolutePath() + ": " + e.getMessage());
        }

        return ModelStatus.UNAVAILABLE;
    }

    public boolean isTextModelAvailable() {
        return getTextModelStatus() == ModelStatus.REAL;
    }

    public synchronized OrtSession getTextSession() {
        if (textSession == null) {
            getTextModelStatus(); // triggers session loading if file is present
        }
        return textSession;
    }

    // =========================================================================
    // FACE DETECTOR MODEL
    // =========================================================================

    public synchronized ModelStatus getFaceDetectorStatus() {
        if (mockMode) {
            return ModelStatus.MOCK;
        }

        // Check for ONNX detector first
        Optional<File> onnxFile = locateModelFile(faceDetectorConfig.getModelPath());
        if (onnxFile.isPresent()) {
            if (faceDetectorSession != null) {
                return ModelStatus.REAL;
            }
            try {
                ensureEnvironment();
                if (environment != null) {
                    this.faceDetectorSession = environment.createSession(onnxFile.get().getAbsolutePath());
                    System.out.println("[ModelManager] Successfully loaded REAL Face Detector ONNX model from: " + onnxFile.get().getAbsolutePath());
                    return ModelStatus.REAL;
                }
            } catch (OrtException e) {
                System.err.println("[ModelManager] Error initializing Face Detector ONNX Session: " + e.getMessage());
            }
        }

        // Check for Haar Cascade classifier file
        Optional<File> cascadeFile = locateModelFile(faceDetectorConfig.getCascadePath());
        if (cascadeFile.isPresent()) {
            return ModelStatus.REAL;
        }

        return ModelStatus.UNAVAILABLE;
    }

    public boolean isFaceDetectorAvailable() {
        return getFaceDetectorStatus() == ModelStatus.REAL;
    }

    public synchronized OrtSession getFaceDetectorSession() {
        if (faceDetectorSession == null) {
            getFaceDetectorStatus();
        }
        return faceDetectorSession;
    }

    // =========================================================================
    // GENERAL CONTROLS AND LIFECYCLE
    // =========================================================================

    private void ensureEnvironment() {
        if (environment == null) {
            initializeEnvironment();
        }
    }

    public synchronized void reload() {
        closeSessions();
        getFacialModelStatus();
        getTextModelStatus();
        getFaceDetectorStatus();
    }

    private void closeSessions() {
        if (facialSession != null) {
            try {
                facialSession.close();
            } catch (Exception ignored) {
            }
            facialSession = null;
        }
        if (textSession != null) {
            try {
                textSession.close();
            } catch (Exception ignored) {
            }
            textSession = null;
        }
        if (faceDetectorSession != null) {
            try {
                faceDetectorSession.close();
            } catch (Exception ignored) {
            }
            faceDetectorSession = null;
        }
    }

    public boolean isMockMode() {
        return mockMode;
    }

    public void setMockMode(boolean mockMode) {
        this.mockMode = mockMode;
    }

    public FacialModelConfig getFacialConfig() {
        return facialConfig;
    }

    public TextModelConfig getTextConfig() {
        return textConfig;
    }

    public FaceDetectorConfig getFaceDetectorConfig() {
        return faceDetectorConfig;
    }

    public OrtEnvironment getEnvironment() {
        return environment;
    }

    @Override
    public synchronized void close() {
        closeSessions();
        if (environment != null) {
            try {
                environment.close();
            } catch (Exception ignored) {
            }
            environment = null;
        }
    }
}
