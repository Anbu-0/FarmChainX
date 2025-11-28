package com.ecobazaar.ecobazaar.controller;


import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.ecobazaar.ecobazaar.model.SupplyChainLog;
import com.ecobazaar.ecobazaar.model.User;
import com.ecobazaar.ecobazaar.repository.UserRepository;
import com.ecobazaar.ecobazaar.service.SupplyChainService;
import com.ecobazaar.ecobazaar.repository.SupplyChainLogRepository;

import java.io.Serializable;
import java.security.Principal;
import java.util.List;
import java.util.Map;


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

    @PostMapping("/update-chain")
    @PreAuthorize("hasAnyRole('DISTRIBUTOR','RETAILER')")
    @Transactional
    public ResponseEntity<?> updateChain(@RequestBody Map<String, Object> payload, Principal principal) {
        try {
        	Long productId = Long.valueOf(String.valueOf(payload.get("productId")));
            String location = String.valueOf(payload.get("location")).trim();
            String notes = payload.containsKey("notes") && payload.get("notes") != null
                    ? String.valueOf(payload.get("notes")).trim() : "";
            Long toUserId = payload.containsKey("toUserId") && payload.get("toUserId") != null
                    ? Long.valueOf(String.valueOf(payload.get("toUserId"))) : null;

            if (location.isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Location is required"));
            }

            User currentUser = userRepository.findByEmail(principal.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            String createdBy = (currentUser.getName() != null && !currentUser.getName().isBlank())
                    ? currentUser.getName()
                    : currentUser.getEmail();

            SupplyChainLog lastLog = supplyChainLogRepository
                    .findTopByProductIdOrderByTimestampDesc(productId)
                    .orElse(null);


            if (currentUser.hasRole("ROLE_DISTRIBUTOR")) {

            	if (lastLog == null || (lastLog.getFromUserId() == null && lastLog.getToUserId() == null)) {
                    supplyChainService.addLog(
                            productId, null, currentUser.getId(), location,
                            notes.isBlank() ? "Distributor received from farmer" : notes,
                            createdBy
                    );
                    return ResponseEntity.ok(Map.of("message", "You have taken possession from farmer"));
                }

            	if (lastLog.isConfirmed() && lastLog.getToUserId() != null &&
                        !lastLog.getToUserId().equals(currentUser.getId())) {
                    return ResponseEntity.status(HttpStatus.CONFLICT)
                            .body(Map.of("error", "Another distributor already has possession of this product"));
                }

            	if (lastLog.getToUserId() != null && !lastLog.getToUserId().equals(currentUser.getId())) {
                    return ResponseEntity.status(HttpStatus.CONFLICT)
                            .body(Map.of("error", "You no longer have possession of this product"));
                }

            	if (lastLog.getToUserId() == null || !lastLog.getToUserId().equals(currentUser.getId()) || !lastLog.isConfirmed()) {
                    return ResponseEntity.status(HttpStatus.CONFLICT)
                            .body(Map.of("error", "You do not have confirmed possession of this product"));
                }

            	if (toUserId != null) {
                    if (toUserId.equals(currentUser.getId())) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Cannot handover to yourself"));
                    }
                    supplyChainService.addLog(
                            productId, currentUser.getId(), toUserId, location,
                            notes.isBlank() ? "Final handover to retailer" : notes,
                            createdBy
                    );
                    return ResponseEntity.ok(Map.of("message", "Product handed over to retailer"));
                }

                supplyChainService.addLog(
                        productId, currentUser.getId(), currentUser.getId(), location,
                        notes.isBlank() ? "In-transit update" : notes,
                        createdBy
                );
                return ResponseEntity.ok(Map.of("message", "Supply chain updated"));

            } else if (currentUser.hasRole("ROLE_RETAILER")) {

            	if (lastLog == null || !lastLog.getToUserId().equals(currentUser.getId())
                        || lastLog.getFromUserId() == null || lastLog.isConfirmed()) {
                    return ResponseEntity.status(HttpStatus.CONFLICT)
                            .body(Map.of("error", "No pending handover for you to confirm"));
                }

            	supplyChainService.confirmReceipt(productId, currentUser.getId(), location, notes, createdBy);
                return ResponseEntity.ok(Map.of("message", "Receipt confirmed successfully"));
            }

            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Unauthorized"));
        } catch (Exception e) {
        	return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", String.valueOf(e.getMessage())));
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