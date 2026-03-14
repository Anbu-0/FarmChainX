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

	private static final String[] GRADES = {"A+", "A", "B+", "B", "C"};
    private final Random random = new Random();

    public Map<String, Object> predictQuality(String imagePath) throws IOException {
        BufferedImage img;

        if (imagePath != null && (imagePath.startsWith("http://") || imagePath.startsWith("https://"))) {
            URL url = new URL(imagePath);
            URLConnection conn = url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(10000);
            try (var in = conn.getInputStream()) {
                img = ImageIO.read(in);
            }
        } else {
            img = ImageIO.read(new File(imagePath));
        }

        if (img == null) throw new IOException("Unsupported image format for: " + imagePath);

        if (img.getWidth() > 800) {
            img = resizeImage(img, 800);
        }

        double redRatio       = calculateRedRatio(img);
        double greenRatio     = calculateGreenRatio(img);
        double yellowRatio    = calculateYellowRatio(img);
        double orangeRatio    = calculateOrangeRatio(img);
        double cropPixelRatio = calculateCropPixelRatio(img);
        double brownBlackRatio = calculateBrownBlackRatio(img, cropPixelRatio);
        double uniformity     = calculateUniformity(brownBlackRatio);

        String cropHint = guessCropFromFilename(imagePath);
        String grade = determineGrade(cropHint, redRatio, greenRatio, yellowRatio, orangeRatio, brownBlackRatio, uniformity);
        double confidence = calculateConfidence(grade, brownBlackRatio, uniformity);

        Map<String, Object> result = new HashMap<>();
        result.put("grade", grade);
        result.put("confidence", confidence);
        return result;
    }

    private String guessCropFromFilename(String path) {
        try {
            String name;
            if (path != null && (path.startsWith("http://") || path.startsWith("https://"))) {
                String p = new URL(path).getPath();
                int slash = p.lastIndexOf('/');
                name = (slash >= 0 ? p.substring(slash + 1) : p).toLowerCase();
            } else {
                name = Path.of(path).getFileName().toString().toLowerCase();
            }
            if (name.contains("mango"))  return "mango";
            if (name.contains("tomato")) return "tomato";
            if (name.contains("apple"))  return "apple";
            if (name.contains("banana")) return "banana";
            if (name.contains("potato")) return "potato";
            if (name.contains("carrot")) return "carrot";
            if (name.contains("orange") || name.contains("kinnow")) return "orange";
        } catch (Exception ignored) {}
        return "unknown";
    }

    private String determineGrade(String crop, double red, double green, double yellow, double orange, double bad, double uniform) {
        // Only fail if there are significant actual dark spots on the crop itself
        boolean hasDamage = bad > 0.35 || uniform < 0.25;
        if (hasDamage) return "C";

        return switch (crop) {
            case "carrot" ->
                    orange > 0.35 && uniform > 0.55 ? (random.nextDouble() < 0.5 ? "A+" : "A")
                            : orange > 0.20 ? "B+" : "B";
            case "mango", "tomato", "apple" ->
                    red + yellow > 0.55 && uniform > 0.6 ? (random.nextDouble() < 0.4 ? "A+" : "A")
                            : red + yellow > 0.35 ? "B+" : green > 0.5 ? "C" : "B";
            case "banana" ->
                    yellow > 0.6 && uniform > 0.65 ? "A+" : yellow > 0.4 ? "A" : "B+";
            case "potato" ->
                    bad < 0.1 && uniform > 0.6 ? "A" : "B";
            case "orange" ->
                    orange + red + yellow > 0.6 ? "A+" : orange + red + yellow > 0.4 ? "A" : "B+";
            default ->
                    red + yellow + orange > 0.45 && uniform > 0.55
                            ? (random.nextDouble() < 0.35 ? "A+" : "A")
                            : uniform > 0.5 ? "B+" : "B";
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

    private double calculateRedRatio(BufferedImage img) {
        return sampleRatio(img, c -> c.getRed() > 170 && c.getRed() > c.getGreen() + 40 && c.getRed() > c.getBlue() + 40);
    }

    private double calculateGreenRatio(BufferedImage img) {
        return sampleRatio(img, c -> c.getGreen() > 160 && c.getGreen() > c.getRed() + 30 && c.getGreen() > c.getBlue() + 30);
    }

    private double calculateYellowRatio(BufferedImage img) {
        return sampleRatio(img, c -> c.getRed() > 160 && c.getGreen() > 160 && c.getBlue() < 120);
    }

    // Orange: high red, medium green, low blue
    private double calculateOrangeRatio(BufferedImage img) {
        return sampleRatio(img, c ->
                c.getRed() > 180
                && c.getGreen() > 80 && c.getGreen() < 180
                && c.getBlue() < 80
                && c.getRed() > c.getGreen() + 30);
    }

    // Crop pixel ratio: pixels that are NOT very dark background
    private double calculateCropPixelRatio(BufferedImage img) {
        return sampleRatio(img, c -> (c.getRed() + c.getGreen() + c.getBlue()) / 3 > 60);
    }

    // Brown/black ratio adjusted for background coverage
    private double calculateBrownBlackRatio(BufferedImage img, double cropRatio) {
        double rawDark = sampleRatio(img, c -> (c.getRed() + c.getGreen() + c.getBlue()) / 3 < 60);
        return cropRatio > 0.1 ? rawDark * (1.0 - cropRatio * 0.6) : rawDark;
    }

    private double calculateUniformity(double brownBlackRatio) {
        return Math.max(0.0, 1.0 - brownBlackRatio * 1.2);
    }

    private double sampleRatio(BufferedImage img, ColorCheck check) {
        long match = 0;
        int total = 0;
        for (int x = 0; x < img.getWidth(); x += 8) {
            for (int y = 0; y < img.getHeight(); y += 8) {
                Color c = new Color(img.getRGB(x, y));
                if (check.test(c)) match++;
                total++;
            }
        }
        return total == 0 ? 0.5 : (double) match / total;
    }

    private BufferedImage resizeImage(BufferedImage original, int maxWidth) {
        int newHeight = (int) (original.getHeight() * ((double) maxWidth / original.getWidth()));
        Image scaled = original.getScaledInstance(maxWidth, newHeight, Image.SCALE_SMOOTH);
        BufferedImage resized = new BufferedImage(maxWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resized.createGraphics();
        g.drawImage(scaled, 0, 0, null);
        g.dispose();
        return resized;
    }

    @FunctionalInterface
    interface ColorCheck {
        boolean test(Color c);
    }
}
