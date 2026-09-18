package com.checkin.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Central configuration for dataset paths.
 * Uses relative project paths rather than hard-coded machine-specific absolute paths,
 * allowing EmoSense to run reliably when cloned or moved to any environment.
 */
public final class DatasetConfig {

    private static final String SYSTEM_PROPERTY_DATA_DIR = "emosense.data.dir";
    private static Path dataRootPath;

    static {
        initDataRoot();
    }

    private DatasetConfig() {}

    private static void initDataRoot() {
        // 1. Check system property override
        String prop = System.getProperty(SYSTEM_PROPERTY_DATA_DIR);
        if (prop != null && !prop.isBlank()) {
            Path p = Paths.get(prop);
            if (Files.isDirectory(p)) {
                dataRootPath = p;
                return;
            }
        }

        // 2. Search common relative candidates
        Path[] candidates = new Path[] {
                Paths.get("data"),
                Paths.get("checkin-app", "data"),
                Paths.get("..", "data"),
                Paths.get("..", "checkin-app", "data")
        };

        for (Path candidate : candidates) {
            if (Files.isDirectory(candidate) &&
                    (Files.isDirectory(candidate.resolve("facial")) || Files.isDirectory(candidate.resolve("text")))) {
                dataRootPath = candidate.toAbsolutePath().normalize();
                return;
            }
        }

        // Default fallback to "data" relative to execution directory
        dataRootPath = Paths.get("data").toAbsolutePath().normalize();
    }

    public static Path getDataRoot() {
        return dataRootPath;
    }

    public static void setDataRoot(Path customPath) {
        if (customPath != null && Files.isDirectory(customPath)) {
            dataRootPath = customPath.toAbsolutePath().normalize();
        }
    }

    // Facial dataset paths
    public static Path getFacialDataDir() {
        return dataRootPath.resolve("facial");
    }

    public static Path getFacialTrainDir() {
        return getFacialDataDir().resolve("train");
    }

    public static Path getFacialTestDir() {
        return getFacialDataDir().resolve("test");
    }

    public static boolean isFacialDatasetAvailable() {
        Path train = getFacialTrainDir();
        return Files.isDirectory(train);
    }

    // Text dataset paths
    public static Path getTextDataDir() {
        return dataRootPath.resolve("text");
    }

    public static Path getTextDataSubdir() {
        return getTextDataDir().resolve("data");
    }

    public static Path getTextTrainFile() {
        return getTextDataSubdir().resolve("train.tsv");
    }

    public static Path getTextDevFile() {
        return getTextDataSubdir().resolve("dev.tsv");
    }

    public static Path getTextTestFile() {
        return getTextDataSubdir().resolve("test.tsv");
    }

    public static Path getTextEmotionsFile() {
        return getTextDataSubdir().resolve("emotions.txt");
    }

    public static Path getTextEkmanMappingFile() {
        return getTextDataSubdir().resolve("ekman_mapping.json");
    }

    public static boolean isTextDatasetAvailable() {
        return Files.isRegularFile(getTextTrainFile());
    }
}
