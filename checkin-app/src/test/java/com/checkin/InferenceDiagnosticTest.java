package com.checkin;

import com.checkin.ml.EmotionPrediction;
import com.checkin.services.MockPhotoAnalysisService;
import com.checkin.services.MockTextAnalysisService;
import com.checkin.services.PhotoAnalysisService;
import com.checkin.services.TextAnalysisService;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class InferenceDiagnosticTest {

    @Test
    void runFacialDiagnostic() throws Exception {
        PhotoAnalysisService service = new MockPhotoAnalysisService();

        Map<String, String> testSamples = new LinkedHashMap<>();
        testSamples.put("angry", "data/facial/test/angry/PrivateTest_10131363.jpg");
        testSamples.put("disgust", "data/facial/test/disgust/PrivateTest_11895083.jpg");
        testSamples.put("fear", "data/facial/test/fear/PrivateTest_10153550.jpg");
        testSamples.put("happy", "data/facial/test/happy/PrivateTest_10077120.jpg");
        testSamples.put("neutral", "data/facial/test/neutral/PrivateTest_10086748.jpg");
        testSamples.put("sad", "data/facial/test/sad/PrivateTest_10247676.jpg");
        testSamples.put("surprise", "data/facial/test/surprise/PrivateTest_10072988.jpg");

        System.out.println("==================================================");
        System.out.println("TASK 4 DIAGNOSTIC: TESTING FACIAL TEST SAMPLES");
        System.out.println("==================================================");

        for (Map.Entry<String, String> entry : testSamples.entrySet()) {
            String actualClass = entry.getKey();
            File file = new File(entry.getValue());
            if (!file.exists()) {
                file = new File("checkin-app/" + entry.getValue());
            }

            System.out.println("--- Test Sample ---");
            System.out.println("Filename: " + file.getName());
            System.out.println("Actual dataset class: " + actualClass);

            if (file.exists()) {
                EmotionPrediction pred = service.analyze(file);
                System.out.println("Raw model output: " + pred.probabilities());
                System.out.println("Predicted class: " + pred.primaryEmotion());
                System.out.println("Confidence: " + pred.confidence() + "%");
                assertNotNull(pred);
            } else {
                System.out.println("File not found: " + file.getAbsolutePath());
            }
        }
    }

    @Test
    void runTextDiagnostic() throws Exception {
        TextAnalysisService service = new MockTextAnalysisService();

        String[] testInputs = {
                "I am extremely happy today. Everything went perfectly.",
                "I feel very sad and disappointed.",
                "I am angry about what happened.",
                "I am worried about tomorrow.",
                "I feel calm and relaxed."
        };

        System.out.println("==================================================");
        System.out.println("TASK 9 DIAGNOSTIC: TESTING TEXT SAMPLES");
        System.out.println("==================================================");

        for (String input : testInputs) {
            System.out.println("--- Text Sample ---");
            System.out.println("Input: \"" + input + "\"");
            EmotionPrediction pred = service.analyze(input);
            System.out.println("Raw model output: " + pred.probabilities());
            System.out.println("Predicted class: " + pred.primaryEmotion());
            System.out.println("Confidence: " + pred.confidence() + "%");
            assertNotNull(pred);
        }
    }
}
