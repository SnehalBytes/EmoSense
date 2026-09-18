package com.checkin;

import ai.onnxruntime.NodeInfo;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;
import ai.onnxruntime.TensorInfo;
import com.checkin.ml.ModelManager;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class OnnxMetadataTest {

    @Test
    void inspectModelMetadata() throws Exception {
        ModelManager mm = ModelManager.getInstance();
        OrtEnvironment env = mm.getEnvironment();

        System.out.println("==================================================");
        System.out.println("ONNX MODEL METADATA INSPECTION");
        System.out.println("==================================================");

        // 1. Facial Model
        Optional<File> facialFile = mm.locateModelFile("models/facial/face_emotion_model.onnx");
        assertTrue(facialFile.isPresent(), "Facial model face_emotion_model must be present");
        try (OrtSession session = env.createSession(facialFile.get().getAbsolutePath())) {
            System.out.println("[FACIAL MODEL] " + facialFile.get().getName());
            printSessionInfo(session);
        } catch (Exception e) {
            System.err.println("[FACIAL MODEL uint8 LOAD FAILED]: " + e.getMessage());
        }

        // 2. Text Model
        Optional<File> textFile = mm.locateModelFile("models/text/goemotions_model.onnx");
        assertTrue(textFile.isPresent(), "Text model must be present");
        try (OrtSession session = env.createSession(textFile.get().getAbsolutePath())) {
            System.out.println("\n[TEXT MODEL] " + textFile.get().getName());
            printSessionInfo(session);
        } catch (Exception e) {
            System.err.println("[TEXT MODEL LOAD FAILED]: " + e.getMessage());
        }

        // 3. Face Detector Model
        Optional<File> detectorFile = mm.locateModelFile("models/face_detection/face_detector.onnx");
        assertTrue(detectorFile.isPresent(), "Detector model must be present");
        try (OrtSession session = env.createSession(detectorFile.get().getAbsolutePath())) {
            System.out.println("\n[FACE DETECTOR MODEL] " + detectorFile.get().getName());
            printSessionInfo(session);
        } catch (Exception e) {
            System.err.println("[FACE DETECTOR LOAD FAILED]: " + e.getMessage());
        }

        System.out.println("==================================================");
    }

    private void printSessionInfo(OrtSession session) throws Exception {
        System.out.println("Inputs (" + session.getNumInputs() + "):");
        for (Map.Entry<String, NodeInfo> entry : session.getInputInfo().entrySet()) {
            TensorInfo info = (TensorInfo) entry.getValue().getInfo();
            System.out.println("  - " + entry.getKey() + " : type=" + info.type + ", shape=" + java.util.Arrays.toString(info.getShape()));
        }
        System.out.println("Outputs (" + session.getNumOutputs() + "):");
        for (Map.Entry<String, NodeInfo> entry : session.getOutputInfo().entrySet()) {
            TensorInfo info = (TensorInfo) entry.getValue().getInfo();
            System.out.println("  - " + entry.getKey() + " : type=" + info.type + ", shape=" + java.util.Arrays.toString(info.getShape()));
        }
    }
}
