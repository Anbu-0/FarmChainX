package com.ecobazaar.ecobazaar.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.ecobazaar.ecobazaar.model.SupplyChainLog;
import com.ecobazaar.ecobazaar.model.User;
import com.ecobazaar.ecobazaar.repository.UserRepository;
import com.ecobazaar.ecobazaar.service.SupplyChainService;
import com.ecobazaar.ecobazaar.repository.SupplyChainLogRepository;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.io.Serializable;

@RestController
@RequestMapping("/api/track")
public class SupplyChainController {

    private final SupplyChainService supplyChainService;
    private final UserRepository userRepository;
    private final SupplyChainLogRepository supplyChainLogRepository;

    public SupplyChainController(
            SupplyChainService supplyChainService,
            UserRepository userRepository,
            SupplyChainLogRepository supplyChainLogRepository) {
        this.supplyChainService = supplyChainService;
        this.userRepository = userRepository;
        this.supplyChainLogRepository = supplyChainLogRepository;
    }

    @PreAuthorize("hasAnyRole('DISTRIBUTOR','RETAILER')")
    @PostMapping("/update-chain")
    public ResponseEntity<?> updateChain(@RequestBody Map<String, Object> payload, Principal principal) {
        try {
            Long productId = Long.valueOf(payload.get("productId").toString());
            String location = payload.get("location").toString();
            String notes = payload.get("notes") != null ? payload.get("notes").toString() : "";
            String email = principal.getName();

            User currentUser = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found: " + email));

            SupplyChainLog lastLog = supplyChainLogRepository
                    .findTopByProductIdOrderByTimestampDesc(productId)
                    .orElse(null);

            SupplyChainLog newLog;

            if (currentUser.hasRole("ROLE_DISTRIBUTOR")) {

                if (lastLog == null || (lastLog.getToUserId() == null && lastLog.getFromUserId() == null)) {
                    newLog = supplyChainService.addLog(
                        productId, null, currentUser.getId(), location,
                        notes.isBlank() ? "Distributor received from farmer" : notes
                    );
                    return ResponseEntity.ok(Map.of(
                        "message", "You have successfully taken the product from the farmer",
                        "log", newLog
                    ));
                }

                if (lastLog.getToUserId() != null && lastLog.getToUserId().equals(currentUser.getId())) {
                    if (payload.get("toUserId") == null) {
                        throw new RuntimeException("Please select a retailer to hand over the product");
                    }
                    Long toRetailerId = Long.valueOf(payload.get("toUserId").toString());

                    newLog = supplyChainService.addLog(
                        productId, currentUser.getId(), toRetailerId, location,
                        notes.isBlank() ? "Product handed over to retailer" : notes
                    );

                    return ResponseEntity.ok(Map.of(
                        "message", "Product handed over to retailer. Now visible in their Pending Receipts.",
                        "log", newLog
                    ));
                }

                throw new RuntimeException("You cannot update this product at this stage");
            }

            else if (currentUser.hasRole("ROLE_RETAILER")) {
                if (lastLog == null || !lastLog.getToUserId().equals(currentUser.getId()) || lastLog.getFromUserId() == null) {
                    throw new RuntimeException("This product has not been handed over to you by the distributor yet");
                }

                newLog = supplyChainService.confirmReceipt(productId, currentUser.getId(), location, notes);
                return ResponseEntity.ok(Map.of(
                    "message", "Receipt confirmed! Product is now fully received.",
                    "log", newLog
                ));
            }

            throw new RuntimeException("Unauthorized action");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN','DISTRIBUTOR','RETAILER')")
    @GetMapping("/{productId}")
    public ResponseEntity<List<SupplyChainLog>> getProductChain(@PathVariable Long productId) {
        return ResponseEntity.ok(supplyChainService.getLogsByProduct(productId));
    }

    @PreAuthorize("hasRole('RETAILER')")
    @GetMapping("/pending")
    public ResponseEntity<?> getPendingForRetailer(
            Principal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "timestamp,desc") String sort) {

        User user = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String[] parts = sort.split(",", 2);
        String sortProp = parts[0];
        boolean asc = parts.length > 1 && "asc".equalsIgnoreCase(parts[1]);
        var pageable = org.springframework.data.domain.PageRequest.of(
                page, size,
                asc ? org.springframework.data.domain.Sort.Direction.ASC : org.springframework.data.domain.Sort.Direction.DESC,
                sortProp
        );

        var pageRes = supplyChainLogRepository.findPendingForRetailer(user.getId(), pageable);
        return ResponseEntity.ok(pageRes); // Page<SupplyChainLog>
    }

    
    @GetMapping("/users/retailers")
    @PreAuthorize("hasRole('DISTRIBUTOR')")
    public List<Map<String, Serializable>> getRetailers() {
        return userRepository.findAll().stream()
                .filter(user -> user.getRoles().stream()
                        .anyMatch(role -> "ROLE_RETAILER".equals(role.getName())))
                .map(user -> Map.<String, Serializable>of(
                        "id", user.getId(),
                        "name", user.getName(),
                        "email", user.getEmail()
                ))
                .sorted((a, b) -> ((String) a.get("name")).compareToIgnoreCase((String) b.get("name")))
                .toList();
    }
}