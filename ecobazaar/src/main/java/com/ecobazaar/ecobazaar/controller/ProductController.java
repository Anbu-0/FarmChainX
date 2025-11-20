package com.ecobazaar.ecobazaar.controller;

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
import com.ecobazaar.ecobazaar.model.SupplyChainLog;
import com.ecobazaar.ecobazaar.repository.ProductRepository;


@RestController
@RequestMapping("/api")
public class ProductController {

    private final ProductService productService;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    public ProductController(ProductService productService, UserRepository userRepository, ProductRepository productRepository) {
        this.productService = productService;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
    }

    @PostMapping("/products/upload")
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
        	if (principal == null) return ResponseEntity.status(401).body(Map.of("error", "Authentication required"));
            if (imageFile == null || imageFile.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "Image is required"));

            String email = principal.getName();
            
            User farmer = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("Farmer not found"));
            String uploadDir = System.getProperty("user.dir") + java.io.File.separator + "uploads";
            java.io.File folder = new java.io.File(uploadDir);
            
            if (!folder.exists()) folder.mkdirs();
            String imagePath = uploadDir + java.io.File.separator + System.currentTimeMillis() + "_" + imageFile.getOriginalFilename();
            imageFile.transferTo(new java.io.File(imagePath));
            java.time.LocalDate parsedDate = null;
            
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

            saved.ensurePublicUuid(); 
            productRepository.save(saved);  
            return ResponseEntity.ok(Map.of("id", saved.getId(), "message", "Product uploaded"));

        } catch (RuntimeException re) {
            return ResponseEntity.badRequest().body(Map.of("error", re.getMessage()));
        } catch (Exception ex) {
        	ex.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error"));
        }
    }

    @PreAuthorize("hasRole('FARMER')")
    @GetMapping("/products/my")
    public ResponseEntity<?> getMyProducts(Principal principal) {
    	if (principal == null) return ResponseEntity.status(401).body(Map.of("error", "Authentication required"));
        String principalName = principal.getName();

        Optional<User> maybeFarmer = userRepository.findByEmail(principalName);

        if (maybeFarmer.isEmpty()) maybeFarmer = userRepository.findByName(principalName);

        User farmer = maybeFarmer.orElseThrow(() -> new RuntimeException("Farmer not found: " + principalName));

        List<Product> products = productService.getProductsByFarmerId(farmer.getId());

        return ResponseEntity.ok(products);
    }


    @PreAuthorize("hasAnyRole('FARMER','ADMIN')")
    @PostMapping("/products/{id}/qrcode")
    public ResponseEntity<?> generateProductQrCode(@PathVariable Long id, Principal principal) {
        if (principal == null) return ResponseEntity.status(401).body(Map.of("error", "Authentication required"));
        
        String email = principal.getName();
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Product product = productService.getProductById(id);
        
        boolean isOwner = product.getFarmer().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRoles().stream()
                .anyMatch(r -> "ROLE_ADMIN".equalsIgnoreCase(r.getName()));

        if (!isOwner && !isAdmin) {
            return ResponseEntity.status(403).body(Map.of("error", "You can only generate QR for your own products"));
        }

        String qrPath = productService.generateProductQr(id);

        return ResponseEntity.ok(Map.of(
            "message", "QR Code generated successfully",
            "qrPath", qrPath,
            "verifyUrl", "https://yourdomain.com/verify/" + product.getPublicUuid() // optional for frontend
        ));
    }

    @GetMapping("/products/{id}/qrcode/download")
    public ResponseEntity<byte[]> downloadProductQR(@PathVariable Long id) {
    	try {
            byte[] imageBytes = productService.getProductQRImage(id);
            return ResponseEntity.ok()
                    .header("Content-Type", "image/png")
                    .header("Content-Disposition", "attachment; filename=product_" + id + ".png")
                    .body(imageBytes);
        } catch (RuntimeException ex) {
            return ResponseEntity.status(404).body(null);
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN','RETAILER','DISTRIBUTOR')")
    @GetMapping("/products/filter")
    public List<Product> filterProducts(
            @RequestParam(required = false) String cropName,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return productService.filterProducts(cropName, endDate);
    }

    @GetMapping("/products/{id}/public")
    public Map<String, Object> getPublicView(@PathVariable Long id) {
        return productService.getPublicView(id);
    }

    @PreAuthorize("hasAnyRole('FARMER','ADMIN','DISTRIBUTOR','RETAILER')")
    @GetMapping("/products/{id}/details")
    public Map<String, Object> getAuthorizedView(@PathVariable Long id, Principal principal) {
        return productService.getAuthorizedView(id, principal != null ? principal.getName() : null);
    }
    @GetMapping("/products/by-uuid/{uuid}/public")
    public ResponseEntity<?> getPublicByUuid(@PathVariable String uuid) {
        return productRepository.findByPublicUuid(uuid)
                .map(p -> ResponseEntity.ok(productService.getPublicView(p.getId())))
                .orElseGet(() -> ResponseEntity.status(404).body(Map.of("error", "Product not found")));
    }

    @GetMapping("/verify/{uuid}")
    public ResponseEntity<?> verifyByUuid(@PathVariable("uuid") String uuid, Principal principal) {
        try {
            Map<String, Object> data = productService.getPublicViewByUuid(uuid);
            boolean canUpdate = false;

            if (principal != null) {
                User user = userRepository.findByEmail(principal.getName()).orElse(null);
                if (user != null) {
                    canUpdate = user.getRoles().stream().anyMatch(role ->
                        "ROLE_DISTRIBUTOR".equals(role.getName()) ||
                        "ROLE_RETAILER".equals(role.getName())
                        // FARMER & ADMIN intentionally REMOVED → they see public view only
                    );
                }
            }

            data.put("canUpdate", canUpdate);
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            return ResponseEntity.status(404).body(Map.of("error", "Product not found"));
        }
    }

    @PostMapping("/verify/{uuid}/track")
    @PreAuthorize("hasAnyRole('RETAILER','DISTRIBUTOR')")  // ← ONLY these roles!
    public ResponseEntity<?> addTrackingByUuid(@PathVariable("uuid") String uuid,
                                               @RequestBody Map<String, String> body,
                                               Principal principal) {
        try {
            String note = body.getOrDefault("note", "").trim();
            String location = body.get("location");
            if (note.isBlank()) return ResponseEntity.badRequest().body(Map.of("error", "note is required"));

            String addedBy = principal != null ? principal.getName() : "unknown";
            SupplyChainLog saved = productService.addTrackingByUuid(uuid, note, location, addedBy);

            Map<String, Object> productPublic = productRepository.findByPublicUuid(uuid)
                    .map(p -> Map.of("trackingHistory", productService.getPublicView(p.getId()).get("trackingHistory")))
                    .orElse(Map.of("trackingHistory", List.of(saved)));

            return ResponseEntity.ok(productPublic);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error"));
        }
    }
}