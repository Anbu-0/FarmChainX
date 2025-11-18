package com.ecobazaar.ecobazaar.controller;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.security.Principal;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.access.prepost.PreAuthorize;

import com.ecobazaar.ecobazaar.model.Product;
import com.ecobazaar.ecobazaar.model.User;
import com.ecobazaar.ecobazaar.repository.UserRepository;
import com.ecobazaar.ecobazaar.service.ProductService;


@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;
    private final UserRepository userRepository;

    public ProductController(ProductService productService, UserRepository userRepository) {
        this.productService = productService;
        this.userRepository = userRepository;
    }

    @PostMapping("/upload")
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<?> uploadProduct(
            @RequestParam String cropName,
            @RequestParam String soilType,
            @RequestParam String pesticides,
            @RequestParam String harvestDate,
            @RequestParam String gpsLocation,
            @RequestParam("image") MultipartFile imageFile,
            Principal principal
    ) throws IOException {

        try {
            if (principal == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Authentication required"));
            }

            if (imageFile == null || imageFile.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Image is required"));
            }

            String email = principal.getName();
            User farmer = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Farmer not found"));

            String uploadDir = System.getProperty("user.dir") + File.separator + "uploads";
            File folder = new File(uploadDir);
            if (!folder.exists()) folder.mkdirs();

            String imagePath = uploadDir + File.separator + System.currentTimeMillis() + "_" + imageFile.getOriginalFilename();
            imageFile.transferTo(new File(imagePath));

            LocalDate parsedDate = null;
            if (harvestDate != null && !harvestDate.isBlank()) {
                try {
                    parsedDate = LocalDate.parse(harvestDate, DateTimeFormatter.ISO_DATE);
                } catch (DateTimeParseException ex1) {
                    String cleaned = harvestDate.replace(" ", "").replace("−", "-").replace("—", "-").replace("/", "-");
                    parsedDate = LocalDate.parse(cleaned, DateTimeFormatter.ofPattern("dd-MM-yyyy"));
                }
            }

            Product product = new Product();
            product.setCropName(cropName);
            product.setSoilType(soilType);
            product.setPesticides(pesticides);
            product.setHarvestDate(parsedDate);
            product.setGpsLocation(gpsLocation);
            product.setImagePath(imagePath);
            product.setQualityGrade("Pending");
            product.setConfidenceScore(0.0);
            product.setFarmer(farmer);

            Product saved = productService.saveProduct(product);

            return ResponseEntity.ok(Map.of(
                    "id", saved.getId(),
                    "message", "Product uploaded"
            ));

        } catch (RuntimeException re) {
            return ResponseEntity.badRequest().body(Map.of("error", re.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error"));
        }
    }

    @PreAuthorize("hasRole('FARMER')")
    @GetMapping("/my")
    public ResponseEntity<?> getMyProducts(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Authentication required"));
        }

        String principalName = principal.getName();

        // Try email first
        Optional<User> maybeFarmer = userRepository.findByEmail(principalName);

        // If not found by email, try name
        if (maybeFarmer.isEmpty()) {
            maybeFarmer = userRepository.findByName(principalName);
        }

        User farmer = maybeFarmer.orElseThrow(() -> new RuntimeException("Farmer not found: " + principalName));

        List<Product> products = productService.getProductsByFarmerId(farmer.getId());

        return ResponseEntity.ok(products);
    }


    @PreAuthorize("hasAnyRole('FARMER','ADMIN')")
    @PostMapping("/{id}/qrcode")
    public String generateProductQrCode(@PathVariable Long id, Principal principal) {
        String email = principal.getName();
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Product product = productService.getProductById(id);
        boolean isFarmer = currentUser.getRoles().stream().anyMatch(r -> r.getName().equalsIgnoreCase("ROLE_FARMER"));
        boolean isAdmin = currentUser.getRoles().stream().anyMatch(r -> r.getName().equalsIgnoreCase("ROLE_ADMIN"));

        if (isFarmer && product.getFarmer().getId() != currentUser.getId()) {
            throw new RuntimeException("Access Denied");
        }

        if (isAdmin || isFarmer) {
            return productService.generateProductQr(id);
        }

        throw new RuntimeException("Access Denied");
    }

    @GetMapping("/{id}/qrcode/download")
    public ResponseEntity<byte[]> downloadProductQR(@PathVariable Long id) {
        byte[] imageBytes = productService.getProductQRImage(id);
        return ResponseEntity.ok()
                .header("Content-Type", "image/png")
                .header("Content-Disposition", "attachment; filename=product_" + id + ".png")
                .body(imageBytes);
    }

    @PreAuthorize("hasAnyRole('ADMIN','RETAILER','DISTRIBUTOR')")
    @GetMapping("/filter")
    public List<Product> filterProducts(
            @RequestParam(required = false) String cropName,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return productService.filterProducts(cropName, endDate);
    }

    @GetMapping("/{id}/public")
    public Map<String, Object> getPublicView(@PathVariable Long id) {
        return productService.getPublicView(id);
    }

    @PreAuthorize("hasAnyRole('FARMER','ADMIN','DISTRIBUTOR','RETAILER')")
    @GetMapping("/{id}/details")
    public Map<String, Object> getAuthorizedView(@PathVariable Long id, Principal principal) {
        return productService.getAuthorizedView(id, principal != null ? principal.getName() : null);
    }
}