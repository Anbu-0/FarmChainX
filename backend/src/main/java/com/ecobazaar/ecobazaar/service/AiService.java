package com.ecobazaar.ecobazaar.service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import org.springframework.stereotype.Service;
import java.net.URL;
import java.net.URLConnection;

@Service
public class AiService {

    private final Random random = new Random();

    public Map<String, Object> predictQuality(String imagePath, String cropName) throws IOException {

        // ✅ Cloudinary URL: force JPG format via URL transformation (handles webp too)
        // ✅ Local file: block .webp since ImageIO can't decode it
        if (imagePath != null && imagePath.contains("cloudinary.com")) {
            // f_jpg converts ANY format (including webp) to JPEG before we download it
            imagePath = imagePath.replaceFirst("/upload/", "/upload/f_jpg,q_80/");
        } else if (imagePath != null && imagePath.toLowerCase().endsWith(".webp")) {
            throw new IOException("WEBP not supported for local files. Use JPG or PNG.");
        }

        BufferedImage img;

        try {
            if (imagePath != null && (imagePath.startsWith("http://") || imagePath.startsWith("https://"))) {

                URL url = new URL(imagePath);
                URLConnection conn = url.openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(10000);
                // Identify as browser so CDN returns image data
                conn.setRequestProperty("User-Agent", "Mozilla/5.0");

                try (var in = conn.getInputStream()) {
                    img = ImageIO.read(in);
                }

            } else {
                File file = new File(imagePath);

                // 🚫 File size limit (2MB)
                if (file.length() > 2 * 1024 * 1024) {
                    throw new IOException("Image too large (Max 2MB)");
                }

                img = ImageIO.read(file);
            }

        } catch (OutOfMemoryError e) {
            throw new IOException("Image too large or unsupported format");
        }

        if (img == null) {
            throw new IOException("Unsupported image format — could not decode image from: " + imagePath);
        }

        // ✅ Resize using headless-safe approach (SCALE_SMOOTH breaks on Render/Linux headless)
        try {
            img = resizeImage(img, 224);
        } catch (OutOfMemoryError e) {
            throw new IOException("Out of memory while processing image");
        }

        // Feature extraction
        double redRatio        = calculateRedRatio(img);
        double greenRatio      = calculateGreenRatio(img);
        double yellowRatio     = calculateYellowRatio(img);
        double orangeRatio     = calculateOrangeRatio(img);
        double purpleRatio     = calculatePurpleRatio(img);
        double cropPixelRatio  = calculateCropPixelRatio(img);
        double brownBlackRatio = calculateBrownBlackRatio(img, cropPixelRatio);
        double uniformity      = calculateUniformity(brownBlackRatio);

        String cropHint = normalizeCropName(cropName);
        String grade = determineGrade(cropHint, redRatio, greenRatio, yellowRatio, orangeRatio, purpleRatio, brownBlackRatio, uniformity);
        double confidence = calculateConfidence(grade, brownBlackRatio, uniformity);

        Map<String, Object> result = new HashMap<>();
        result.put("grade", grade);
        result.put("confidence", confidence);

        return result;
    }

    public Map<String, Object> predictQuality(String imagePath) throws IOException {
        return predictQuality(imagePath, guessCropFromFilename(imagePath));
    }

    private String normalizeCropName(String cropName) {
        if (cropName == null) return "unknown";
        String name = cropName.trim().toLowerCase();

        if (name.contains("carrot")) return "carrot";
        if (name.contains("mango")) return "mango";
        if (name.contains("tomato")) return "tomato";
        if (name.contains("apple")) return "apple";
        if (name.contains("banana")) return "banana";
        if (name.contains("potato")) return "potato";
        if (name.contains("onion")) return "onion";
        if (name.contains("orange") || name.contains("kinnow")) return "orange";
        if (name.contains("grape")) return "grape";
        if (name.contains("watermelon")) return "watermelon";
        if (name.contains("rice")) return "rice";
        if (name.contains("wheat")) return "wheat";
        if (name.contains("corn")) return "corn";

        return "unknown";
    }

    private String guessCropFromFilename(String path) {
        try {
            String name;
            if (path != null && path.startsWith("http")) {
                String p = new URL(path).getPath();
                int slash = p.lastIndexOf('/');
                name = (slash >= 0 ? p.substring(slash + 1) : p).toLowerCase();
            } else {
                name = Path.of(path).getFileName().toString().toLowerCase();
            }
            return normalizeCropName(name);
        } catch (Exception ignored) {}
        return "unknown";
    }

    private String determineGrade(String crop, double red, double green, double yellow, double orange, double purple, double bad, double uniform) {
        boolean hasDamage = bad > 0.35 || uniform < 0.25;
        if (hasDamage) return "C";

        return switch (crop) {
            case "carrot" ->
                    orange > 0.30 && uniform > 0.50 ? (random.nextDouble() < 0.5 ? "A+" : "A")
                            : orange > 0.15 ? "B+" : "B";
            case "onion" ->
                    purple + red > 0.30 && uniform > 0.55 ? (random.nextDouble() < 0.4 ? "A+" : "A")
                            : purple + red > 0.15 ? "B+" : "B";
            case "mango", "tomato", "apple" ->
                    red + yellow > 0.55 && uniform > 0.60 ? (random.nextDouble() < 0.4 ? "A+" : "A")
                            : red + yellow > 0.35 ? "B+" : green > 0.5 ? "C" : "B";
            case "banana" ->
                    yellow > 0.60 && uniform > 0.65 ? "A+" : yellow > 0.40 ? "A" : "B+";
            case "potato" ->
                    bad < 0.10 && uniform > 0.60 ? "A" : "B";
            case "orange" ->
                    orange + red + yellow > 0.60 ? "A+" : orange + red + yellow > 0.40 ? "A" : "B+";
            case "grape" ->
                    purple > 0.25 && uniform > 0.55 ? "A+" : purple > 0.15 ? "A" : "B+";
            case "watermelon" ->
                    green > 0.25 && uniform > 0.55 ? "A" : "B+";
            case "rice", "wheat", "corn" ->
                    yellow > 0.30 && uniform > 0.55 ? "A" : uniform > 0.45 ? "B+" : "B";
            default ->
                    red + yellow + orange + purple > 0.40 && uniform > 0.50
                            ? (random.nextDouble() < 0.35 ? "A+" : "A")
                            : uniform > 0.45 ? "B+" : "B";
        };
    }

    private double calculateConfidence(String grade, double badSpots, double uniform) {
        double base = switch (grade) {
            case "A+" -> 0.945;
            case "A"  -> 0.905;
            case "B+" -> 0.855;
            case "B"  -> 0.795;
            case "C"  -> 0.720;
            default   -> 0.720;
        };

        double penalty = badSpots * 0.15 + (1 - uniform) * 0.08;
        double result = base - penalty + random.nextDouble() * 0.04;

        return Math.round(Math.max(0.5, result) * 10000.0) / 10000.0;
    }

    private double sampleRatio(BufferedImage img, ColorCheck check) {
        long match = 0;
        int total = 0;

        for (int x = 0; x < img.getWidth(); x += 12) {
            for (int y = 0; y < img.getHeight(); y += 12) {
                Color c = new Color(img.getRGB(x, y));
                if (check.test(c)) match++;
                total++;
            }
        }

        return total == 0 ? 0.5 : (double) match / total;
    }

    private double calculateRedRatio(BufferedImage img) {
        return sampleRatio(img, c -> c.getRed() > 170 && c.getRed() > c.getGreen() + 40 && c.getRed() > c.getBlue() + 40);
    }

    private double calculateGreenRatio(BufferedImage img) {
        return sampleRatio(img, c -> c.getGreen() > 160 && c.getGreen() > c.getRed() + 30 && c.getGreen() > c.getBlue() + 30);
    }

    private double calculateYellowRatio(BufferedImage img) {
        return sampleRatio(img, c -> c.getRed() > 160 && c.getGreen() > 160 && c.getBlue() < 120);
    }

    private double calculateOrangeRatio(BufferedImage img) {
        return sampleRatio(img, c ->
                c.getRed() > 180 &&
                c.getGreen() > 80 && c.getGreen() < 180 &&
                c.getBlue() < 80 &&
                c.getRed() > c.getGreen() + 30);
    }

    private double calculatePurpleRatio(BufferedImage img) {
        return sampleRatio(img, c ->
                c.getRed() > 100 && c.getRed() < 220 &&
                c.getBlue() > 80 &&
                c.getGreen() < 120);
    }

    private double calculateCropPixelRatio(BufferedImage img) {
        return sampleRatio(img, c -> (c.getRed() + c.getGreen() + c.getBlue()) / 3 > 60);
    }

    private double calculateBrownBlackRatio(BufferedImage img, double cropRatio) {
        double rawDark = sampleRatio(img, c -> (c.getRed() + c.getGreen() + c.getBlue()) / 3 < 60);
        return cropRatio > 0.1 ? rawDark * (1.0 - cropRatio * 0.6) : rawDark;
    }

    private double calculateUniformity(double brownBlackRatio) {
        return Math.max(0.0, 1.0 - brownBlackRatio * 1.2);
    }

    private BufferedImage resizeImage(BufferedImage original, int size) {
        // ✅ Headless-safe resize — does NOT use SCALE_SMOOTH which requires AWT display
        BufferedImage resized = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resized.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(original, 0, 0, size, size, null);
        g.dispose();
        return resized;
    }

    @FunctionalInterface
    interface ColorCheck {
        boolean test(Color c);
    }
}
