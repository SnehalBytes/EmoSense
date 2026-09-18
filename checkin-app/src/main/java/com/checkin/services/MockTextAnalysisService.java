package com.checkin.services;

import com.checkin.ml.MockTextEmotionModel;

/**
 * Dedicated prototype mock service for textual analysis.
 * Kept completely separated from production inference pipelines.
 */
public class MockTextAnalysisService extends TextAnalysisService {

    public MockTextAnalysisService() {
        super(new MockTextEmotionModel());
    }
}
