package com.checkin.services;

import com.checkin.ml.MockFacialEmotionModel;

/**
 * Dedicated prototype mock service for facial analysis.
 * Kept completely separated from production inference pipelines.
 */
public class MockPhotoAnalysisService extends PhotoAnalysisService {

    public MockPhotoAnalysisService() {
        super(new MockFacialEmotionModel());
    }
}
