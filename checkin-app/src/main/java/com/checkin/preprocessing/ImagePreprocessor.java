package com.checkin.preprocessing;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

/**
 * Preprocessing pipeline for input facial photos.
 *
 * Pipeline:
 * File -> File Validation -> Image Loading -> Format Normalization -> Model Ready Buffer
 *
 * NOTE: Model-specific tensor shapes (e.g. 48x48 vs 224x224) and normalization parameters
 * (e.g. mean/std scaling) are kept modular until the specific ONNX/Java model is selected.
 */
public class ImagePreprocessor {

    private static final List<String> ALLOWED_EXTENSIONS = List.of("jpg", "jpeg", "png");
    private static final long MAX_FILE_SIZE_BYTES = 10L * 1024 * 1024; // 10 MB

    public record PreprocessedImage(
            File sourceFile,
            BufferedImage bufferedImage,
            int originalWidth,
            int originalHeight,
            boolean isGrayscale
    ) {}

    public PreprocessedImage process(File file) throws IllegalArgumentException, IOException {
        validateFile(file);

        BufferedImage img = ImageIO.read(file);
        if (img == null) {
            throw new IOException("Unable to decode image file. Format may be unsupported or corrupted.");
        }

        boolean grayscale = (img.getType() == BufferedImage.TYPE_BYTE_GRAY);

        return new PreprocessedImage(
                file,
                img,
                img.getWidth(),
                img.getHeight(),
                grayscale
        );
    }

    public void validateFile(File file) throws IllegalArgumentException {
        if (file == null || !file.exists() || !file.isFile()) {
            throw new IllegalArgumentException("Selected image file does not exist.");
        }
        if (file.length() <= 0) {
            throw new IllegalArgumentException("Selected image file is empty.");
        }
        if (file.length() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("Selected image exceeds the 10 MB size limit.");
        }

        String name = file.getName().toLowerCase();
        String ext = name.contains(".") ? name.substring(name.lastIndexOf('.') + 1) : "";
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new IllegalArgumentException("Unsupported file type: ." + ext + ". Please select a JPG, JPEG, or PNG image.");
        }
    }
}
