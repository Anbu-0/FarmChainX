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

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // ✅ use Spring’s version
import org.springframework.security.core.userdetails.UserDetails;

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

    public ProductService(ProductRepository productRepository, UserRepository userRepository, AiService aiService, SupplyChainLogRepository supplyChainLogRepository) {
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.aiService = aiService;
        this.supplyChainLogRepository = supplyChainLogRepository;
    }
    @Transactional
    public Product saveProduct(Product product) {

    	Product saved = productRepository.save(product);
       

        try {
            if (saved.getImagePath() != null) {
                Map<String, Object> aiResult = aiService.predictQuality(saved.getImagePath());

                if (aiResult != null) {
                    saved.setQualityGrade(String.valueOf(aiResult.get("grade")));
                    saved.setConfidenceScore(Double.parseDouble(aiResult.get("confidence").toString()));
                    productRepository.save(saved);

                }
           
            }
        } catch (Exception e) {
            System.out.println("AI Error: " + e.getMessage());
        }

        return saved;
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

     // Ensure public UUID exists
        product.ensurePublicUuid();
        productRepository.save(product); // save if new UUID was generated

        String publicUuid = product.getPublicUuid();

     // CHANGE THIS IN PRODUCTION!
        String frontendBase = System.getenv("FRONTEND_URL");
        if (frontendBase == null || frontendBase.isBlank()) {
            frontendBase = "http://localhost:4200"; // dev fallback
        }

        String qrText = frontendBase + "/verify/" + publicUuid;

        try {
        	Path qrDir = Path.of("uploads", "qrcodes");
            Files.createDirectories(qrDir);

            String fileName = "qr_" + publicUuid + ".png"; // Better: use UUID instead of ID
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
 // ENHANCED PUBLIC VIEW — Now with beautiful, readable tracking history
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
        data.put("productId", product.getId());
        data.put("publicUuid", product.getPublicUuid());
        data.put("qrCodePath", product.getQrCodePath());

     // Beautiful tracking history with fallbacks
        List<SupplyChainLog> logs = supplyChainLogRepository.findByProductIdOrderByTimestampAsc(productId);
        List<Map<String, Object>> trackingHistory = logs.stream().map(log -> {
            Map<String, Object> m = new HashMap<>();
            m.put("location", log.getLocation() != null && !log.getLocation().isBlank() ? log.getLocation() : "Farm Origin");
            m.put("notes", log.getNotes() != null && !log.getNotes().isBlank() ? log.getNotes() : "Product harvested and entered supply chain");
            m.put("timestamp", log.getTimestamp() != null ? log.getTimestamp() : LocalDateTime.now());
            m.put("createdBy", log.getCreatedBy() != null && !log.getCreatedBy().isBlank() ? log.getCreatedBy() : "Farmer");
            return m;
        }).collect(Collectors.toList());

        // Optional: Add initial "harvested" log if history is empty
        if (trackingHistory.isEmpty()) {
            Map<String, Object> initial = new HashMap<>();
            initial.put("location", "Farm Origin");
            initial.put("notes", "Product harvested and entered the FarmChainX blockchain");
            initial.put("timestamp", product.getHarvestDate() != null ? product.getHarvestDate().atStartOfDay() : LocalDateTime.now());
            initial.put("createdBy", "Farmer");
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
  

   

}