package com.ecobazaar.ecobazaar.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // ✅ use Spring’s version
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.client.RestTemplate;

import com.ecobazaar.ecobazaar.model.Product;
import com.ecobazaar.ecobazaar.repository.ProductRepository;
import com.ecobazaar.ecobazaar.repository.UserRepository;
import com.ecobazaar.ecobazaar.util.QrCodeGenerator;
import com.ecobazaar.ecobazaar.model.SupplyChainLog;
import com.ecobazaar.ecobazaar.repository.SupplyChainLogRepository;;

@Service
public class ProductService {

	private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final AiService aiService;
    private final SupplyChainLogRepository supplyChainLogRepository;

    public ProductService(ProductRepository productRepository,
                          UserRepository userRepository,
                          AiService aiService,
                          SupplyChainLogRepository supplyChainLogRepository) {
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.aiService = aiService;
        this.supplyChainLogRepository = supplyChainLogRepository;
    }

    @Transactional
    public Product saveProduct(Product product) {
        Product saved = productRepository.save(product);
        try {
            String path = saved.getImagePath();
            if (path == null || path.isBlank()) {
                throw new IllegalStateException("Image path missing for grading");
            }

            // ✅ Let AiService handle URL or local file. Do NOT pre-check with File.exists() here.
            Map<String, Object> aiResult = aiService.predictQuality(path);

            if (aiResult != null && aiResult.get("grade") != null && aiResult.get("confidence") != null) {
                saved.setQualityGrade(String.valueOf(aiResult.get("grade")));
                saved.setConfidenceScore(Double.parseDouble(aiResult.get("confidence").toString()));
                return productRepository.save(saved);
            } else {
                throw new IllegalStateException("AI returned empty result");
            }
        } catch (Exception e) {
            // Non-fatal: keep the product, leave grading as pending
            System.err.println("[AI Grading Error] " + e.getClass().getSimpleName() + ": " + e.getMessage());
            return saved;
        }
    }

    public List<Product> getProductsByFarmerId(Long farmerId) {
        userRepository.findById(farmerId).orElseThrow(() -> new RuntimeException("Farmer not found"));
        return productRepository.findByFarmerId(farmerId);
    }

    public Product getProductById(Long id) {
        return productRepository.findById(id).orElseThrow(() -> new RuntimeException("Product not found"));
    }

    public List<Product> filterProducts(String cropName, LocalDate endDate) {
        return productRepository.filterProducts(cropName, endDate);
    }

    public String generateProductQr(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        product.ensurePublicUuid();
        productRepository.save(product);
        String publicUuid = product.getPublicUuid();

        String frontendBase = System.getenv("FRONTEND_URL");
        if (frontendBase == null || frontendBase.isBlank()) {
            frontendBase = "http://localhost:4200";
        }

        String qrText = frontendBase + "/verify/" + publicUuid;
        try {
            Path qrDir = Path.of("uploads", "qrcodes");
            Files.createDirectories(qrDir);
            String fileName = "qr_" + publicUuid + ".png";
            Path qrFilePath = qrDir.resolve(fileName);
            QrCodeGenerator.generateQR(qrText, qrFilePath.toString());
            String webPath = "/uploads/qrcodes/" + fileName;
            product.setQrCodePath(webPath);
            productRepository.save(product);
            return webPath;
        } catch (Exception e) {
            throw new RuntimeException("Error generating QR code: " + e.getMessage(), e);
        }
    }

    public byte[] getProductQRImage(Long productId) {
        Product product = productRepository.findById(productId).orElseThrow(() -> new RuntimeException("Product not found"));
        String qrPath = product.getQrCodePath();
        if (qrPath == null || qrPath.isEmpty()) throw new RuntimeException("QR code not generated yet for this product");
        try {
            Path path1 = Path.of(qrPath.startsWith("/") ? qrPath.substring(1) : qrPath);
            Path path2 = Path.of("uploads", "qrcodes", "product_" + productId + ".png");
            Path actual = Files.exists(path1) ? path1 : (Files.exists(path2) ? path2 : null);
            if (actual == null) throw new RuntimeException("QR file not found.");
            return Files.readAllBytes(actual);
        } catch (IOException e) {
            throw new RuntimeException("Unable to read the QR code image: " + e.getMessage(), e);
        }
    }

    public Map<String, Object> getPublicView(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        Map<String, Object> data = new HashMap<>();
        data.put("cropName", product.getCropName());
        data.put("harvestDate", product.getHarvestDate());
        data.put("qualityGrade", product.getQualityGrade() != null ? product.getQualityGrade() : "Pending");
        data.put("confidence", product.getConfidenceScore() != null ? product.getConfidenceScore() : 0.0);
        data.put("imageUrl", product.getImagePath());
        data.put("gpsLocation", product.getGpsLocation());
        data.put("displayLocation", reverseToName(product.getGpsLocation()));
        data.put("productId", product.getId());
        data.put("publicUuid", product.getPublicUuid());
        data.put("qrCodePath", product.getQrCodePath());

        List<SupplyChainLog> logs = supplyChainLogRepository.findByProductIdOrderByTimestampAsc(productId);
        List<Map<String, Object>> trackingHistory = logs.stream().map(log -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", log.getId());
            m.put("productId", log.getProductId());
            m.put("fromUserId", log.getFromUserId());
            m.put("toUserId", log.getToUserId());
            m.put("location", log.getLocation() != null && !log.getLocation().isBlank() ? log.getLocation() : "Farm Origin");
            m.put("notes", log.getNotes() != null && !log.getNotes().isBlank() ? log.getNotes() : "Product harvested and entered supply chain");
            m.put("timestamp", log.getTimestamp() != null ? log.getTimestamp() : LocalDateTime.now());
            m.put("createdBy", log.getCreatedBy() != null && !log.getCreatedBy().isBlank() ? log.getCreatedBy() : "Farmer");
            m.put("prevHash", log.getPrevHash());
            m.put("hash", log.getHash());
            m.put("confirmed", log.isConfirmed());
            m.put("confirmedAt", log.getConfirmedAt());
            m.put("confirmedById", log.getConfirmedById());
            m.put("rejected", log.isRejected());
            m.put("rejectReason", log.getRejectReason());
            return m;
        }).collect(Collectors.toList());

        if (trackingHistory.isEmpty()) {
            Map<String, Object> initial = new HashMap<>();
            initial.put("id", null);
            initial.put("productId", product.getId());
            initial.put("fromUserId", null);
            initial.put("toUserId", null);
            initial.put("location", "Farm Origin");
            initial.put("notes", "Product harvested and entered the FarmChainX blockchain");
            initial.put("timestamp", product.getHarvestDate() != null ? product.getHarvestDate().atStartOfDay() : LocalDateTime.now());
            initial.put("createdBy", "Farmer");
            initial.put("prevHash", "");
            initial.put("hash", "");
            initial.put("confirmed", false);
            initial.put("confirmedAt", null);
            initial.put("confirmedById", null);
            initial.put("rejected", false);
            initial.put("rejectReason", null);
            trackingHistory.add(initial);
        }

        data.put("trackingHistory", trackingHistory);
        return data;
    }

    public Map<String, Object> getPublicViewByUuid(String publicUuid) {
        Product product = productRepository.findByPublicUuid(publicUuid)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        return getPublicView(product.getId());
    }

    public SupplyChainLog addTrackingByUuid(String publicUuid, String notes, String location, String addedByUsername) {
        Product product = productRepository.findByPublicUuid(publicUuid)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        SupplyChainLog log = new SupplyChainLog();
        log.setProductId(product.getId());
        log.setNotes(notes);
        log.setLocation(location);
        log.setTimestamp(LocalDateTime.now());
        log.setFromUserId(null);
        log.setToUserId(null);
        log.setCreatedBy(addedByUsername != null ? addedByUsername : "Anonymous User");
        return supplyChainLogRepository.save(log);
    }

    public Map<String, Object> getAuthorizedView(Long productId, Object userPrincipal) {
        Map<String, Object> data = new HashMap<>(getPublicView(productId));
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        data.put("soilType", product.getSoilType());
        data.put("pesticides", product.getPesticides());
        data.put("canUpdateChain", false);
        data.put("requestedBy", "unknown");
        try {
            if (userPrincipal instanceof UserDetails ud) {
                data.put("requestedBy", ud.getUsername());
            } else if (userPrincipal instanceof String s) {
                data.put("requestedBy", s);
            }
        } catch (Exception ignored) {}
        return data;
    }

    private String reverseToName(String gps) {
        try {
            if (gps == null || !gps.contains(",")) return gps;
            String[] parts = gps.split(",");
            double lat = Double.parseDouble(parts[0].trim());
            double lon = Double.parseDouble(parts[1].trim());
            String url = "https://nominatim.openstreetmap.org/reverse?format=json&lat="
                    + URLEncoder.encode(String.valueOf(lat), StandardCharsets.UTF_8)
                    + "&lon=" + URLEncoder.encode(String.valueOf(lon), StandardCharsets.UTF_8)
                    + "&zoom=10&addressdetails=1";
            RestTemplate rest = new RestTemplate();
            Map<?, ?> response = rest.getForObject(url, Map.class);
            if (response == null) return gps;
            Object display = response.get("display_name");
            if (display == null) return gps;
            String[] tokens = display.toString().split(",");
            return tokens.length > 0 ? tokens[0].trim() : gps;
        } catch (Exception e) {
            return gps;
        }
    }
}