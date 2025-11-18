package com.ecobazaar.ecobazaar.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // ✅ use Spring’s version
import org.springframework.security.core.userdetails.UserDetails;

import com.ecobazaar.ecobazaar.model.Product;
import com.ecobazaar.ecobazaar.repository.ProductRepository;
import com.ecobazaar.ecobazaar.repository.UserRepository;
import com.ecobazaar.ecobazaar.model.User;
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
        System.out.println("✅ [Service] Saving product: " + product.getCropName());
        Product saved = productRepository.save(product);
        System.out.println("✅ [Service] After save, ID = " + saved.getId());

        try {
            if (saved.getImagePath() != null) {
                Map<String, Object> aiResult = aiService.predictQuality(saved.getImagePath());

                if (aiResult != null) {
                    saved.setQualityGrade(String.valueOf(aiResult.get("grade")));
                    saved.setConfidenceScore(Double.parseDouble(aiResult.get("confidence").toString()));
                    productRepository.save(saved);

                    System.out.println("🤖 AI Grade: " + saved.getQualityGrade() +
                            " | Confidence Score: " + saved.getConfidenceScore());
                }
            } else {
                System.out.println("⚠️ No image path found for product ID: " + saved.getId());
            }
        } catch (Exception e) {
            System.out.println("❌ AI Error: " + e.getMessage());
        }

        return saved;
    }

    public List<Product> getProductsByFarmerId(Long farmerId) {
        userRepository.findById(farmerId)
                .orElseThrow(() -> new RuntimeException("Farmer not found"));
        return productRepository.findByFarmerId(farmerId);
    }

    public Product getProductById(Long id) {
        return productRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Product not found"));
    }

    public List<Product> filterProducts(String cropName, LocalDate endDate) {
        return productRepository.filterProducts(cropName, endDate);
    }

    public String generateProductQr(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        // 🌍 Step 1: Always use localhost for student/local testing
        String baseUrl = getServerBaseUrl();
        String qrText = baseUrl + "/api/verify/" + product.getId();

        // 📁 Step 2: Define QR code save location
        String qrPath = "uploads/qrcodes/product_" + productId + ".png";

        try {
            File qrFile = new File(qrPath);
            File parentDir = qrFile.getParentFile();
            if (parentDir != null && !parentDir.exists() && !parentDir.mkdirs()) {
                throw new RuntimeException("Failed to create directory: " + parentDir.getAbsolutePath());
            }

            // 🧩 Step 3: Generate QR
            QrCodeGenerator.generateQR(qrText, qrPath);

            // 🗂️ Step 4: Save path in DB
            product.setQrCodePath(qrPath);
            productRepository.save(product);

            System.out.println("✅ [QR Generated] " + qrText);
            return qrPath;

        } catch (Exception e) {
            throw new RuntimeException("Error generating QR: " + e.getMessage());
        }
    }

    private String getServerBaseUrl() {
        try {
            // 🧩 Automatically get your computer’s LAN IP address
            String localIp = java.net.InetAddress.getLocalHost().getHostAddress();
            // ✅ Works on your laptop and mobile (same Wi-Fi)
            return "http://" + localIp + ":8081";
        } catch (Exception e) {
            // fallback if LAN IP not found
            return "http://localhost:8081";
        }
    }


    public byte[] getProductQRImage(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        String qrPath = product.getQrCodePath();

        if (qrPath == null || qrPath.isEmpty()) {
            throw new RuntimeException("QR code not generated yet for this product");
        }

        try {
            Path path = Path.of(qrPath);
            return Files.readAllBytes(path);
        } catch (IOException e) {
            throw new RuntimeException("Unable to read the QR code image: " + e.getMessage());
        }
    }

    public Map<String, Object> getPublicView(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        Map<String, Object> data = new HashMap<>();
        data.put("cropName", product.getCropName());
        data.put("harvestDate", product.getHarvestDate());
        data.put("qualityGrade", product.getQualityGrade());
        data.put("confidence", product.getConfidenceScore());
        data.put("imageUrl", product.getImagePath());
        data.put("gpsLocation", product.getGpsLocation());

        // 🌾 Include tracking history for everyone (even public)
        List<SupplyChainLog> logs = supplyChainLogRepository.findByProductIdOrderByTimestampAsc(productId);
        data.put("trackingHistory", logs);

        return data;
    }

    public Map<String, Object> getAuthorizedView(Long productId, Object userPrincipal) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        Map<String, Object> data = new HashMap<>(getPublicView(productId));
        data.put("soilType", product.getSoilType());
        data.put("pesticides", product.getPesticides());
        data.put("canUpdateChain", true);

        String requestedBy = "Unknown";
        try {
            if (userPrincipal instanceof UserDetails userDetails) {
                requestedBy = userDetails.getUsername();
            } else if (userPrincipal instanceof String strUser) {
                requestedBy = strUser;
            }
        } catch (Exception e) {
            System.out.println("⚠️ [AuthorizedView] Unable to resolve user: " + e.getMessage());
        }

        data.put("requestedBy", requestedBy);
        return data;
    }
  

   

}