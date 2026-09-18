package com.checkin.services;

import com.checkin.model.DatasetStatistics;

import java.util.Map;

/**
 * Inspection coordinator that gathers verified statistics from both
 * the facial and text datasets and logs them.
 */
public class DatasetInspectionService {

    private final FacialDatasetService facialService;
    private final TextDatasetService textService;

    public DatasetInspectionService() {
        this(new FacialDatasetService(), new TextDatasetService());
    }

    public DatasetInspectionService(FacialDatasetService facialService, TextDatasetService textService) {
        this.facialService = facialService;
        this.textService = textService;
    }

    public DatasetStatistics gatherStatistics() {
        boolean facialAvail = facialService.isAvailable();
        int trainCount = facialService.countImagesInSplit("train");
        int testCount = facialService.countImagesInSplit("test");
        Map<String, Integer> trainClasses = facialService.getClassCounts("train");
        Map<String, Integer> testClasses = facialService.getClassCounts("test");

        boolean textAvail = textService.isAvailable();
        var trainMetrics = textService.inspectSplit("train");
        var devMetrics = textService.inspectSplit("dev");
        var testMetrics = textService.inspectSplit("test");

        int textGrandTotal = trainMetrics.total() + devMetrics.total() + testMetrics.total();
        int singleLabel = trainMetrics.singleLabel() + devMetrics.singleLabel() + testMetrics.singleLabel();
        int multiLabel = trainMetrics.multiLabel() + devMetrics.multiLabel() + testMetrics.multiLabel();
        int malformed = trainMetrics.malformed() + devMetrics.malformed() + testMetrics.malformed();
        int empty = trainMetrics.emptyText() + devMetrics.emptyText() + testMetrics.emptyText();

        return new DatasetStatistics(
                facialAvail,
                trainCount,
                testCount,
                trainCount + testCount,
                trainClasses,
                testClasses,
                "JPEG (.jpg)",
                "48x48 Grayscale",
                0,
                textAvail,
                trainMetrics.total(),
                devMetrics.total(),
                testMetrics.total(),
                textGrandTotal,
                singleLabel,
                multiLabel,
                malformed,
                empty,
                TextEmotionMapping.getAllGoEmotions().size()
        );
    }

    public void printConsoleReport() {
        DatasetStatistics stats = gatherStatistics();
        System.out.println(stats.formatSummary());
    }
}
