package com.checkin.services;

import com.checkin.config.DatasetConfig;
import com.checkin.model.FacialDatasetSample;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Service for discovering, inspecting, validating, and lazily loading
 * samples from the FER2013 facial expression dataset.
 *
 * Designed with LAZY LOADING: image pixel buffers are never preloaded into memory;
 * only directory structure and metadata are inspected on-demand.
 */
public class FacialDatasetService {

    public static final List<String> FACIAL_CLASSES = List.of(
            "angry", "disgust", "fear", "happy", "neutral", "sad", "surprise"
    );

    private final Path trainDir;
    private final Path testDir;

    public FacialDatasetService() {
        this.trainDir = DatasetConfig.getFacialTrainDir();
        this.testDir = DatasetConfig.getFacialTestDir();
    }

    public boolean isAvailable() {
        return Files.isDirectory(trainDir) && Files.isDirectory(testDir);
    }

    public List<String> getClasses() {
        return FACIAL_CLASSES;
    }

    /**
     * Counts the total number of images in a split ("train" or "test").
     */
    public int countImagesInSplit(String split) {
        Path dir = "test".equalsIgnoreCase(split) ? testDir : trainDir;
        if (!Files.isDirectory(dir)) return 0;

        try (Stream<Path> stream = Files.walk(dir, 2)) {
            return (int) stream.filter(Files::isRegularFile)
                    .filter(this::isJpgImage)
                    .count();
        } catch (IOException e) {
            return 0;
        }
    }

    /**
     * Retrieves the image count for each emotion class in the specified split.
     */
    public Map<String, Integer> getClassCounts(String split) {
        Path dir = "test".equalsIgnoreCase(split) ? testDir : trainDir;
        Map<String, Integer> counts = new LinkedHashMap<>();

        for (String cls : FACIAL_CLASSES) {
            Path classPath = dir.resolve(cls);
            if (Files.isDirectory(classPath)) {
                try (Stream<Path> files = Files.list(classPath)) {
                    long c = files.filter(Files::isRegularFile).filter(this::isJpgImage).count();
                    counts.put(cls, (int) c);
                } catch (IOException e) {
                    counts.put(cls, 0);
                }
            } else {
                counts.put(cls, 0);
            }
        }
        return counts;
    }

    /**
     * Lazily samples up to 'limit' image paths from a specific emotion class and split.
     */
    public List<FacialDatasetSample> sampleImages(String split, String emotionClass, int limit) {
        Path dir = ("test".equalsIgnoreCase(split) ? testDir : trainDir).resolve(emotionClass);
        if (!Files.isDirectory(dir)) return Collections.emptyList();

        List<FacialDatasetSample> samples = new ArrayList<>();
        try (Stream<Path> files = Files.list(dir)) {
            List<Path> paths = files.filter(Files::isRegularFile)
                    .filter(this::isJpgImage)
                    .limit(limit)
                    .toList();

            for (Path p : paths) {
                try {
                    long size = Files.size(p);
                    samples.add(new FacialDatasetSample(
                            p, p.getFileName().toString(), emotionClass, split, size, 48, 48
                    ));
                } catch (IOException ignored) {}
            }
        } catch (IOException e) {
            return Collections.emptyList();
        }
        return samples;
    }

    /**
     * Lazily loads a single image from disk on-demand into a BufferedImage.
     */
    public BufferedImage loadImage(Path imagePath) throws IOException {
        if (imagePath == null || !Files.isRegularFile(imagePath)) {
            throw new IOException("Facial image file does not exist: " + imagePath);
        }
        BufferedImage img = ImageIO.read(imagePath.toFile());
        if (img == null) {
            throw new IOException("Corrupted or unreadable image file: " + imagePath);
        }
        return img;
    }

    private boolean isJpgImage(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        return name.endsWith(".jpg") || name.endsWith(".jpeg");
    }
}
