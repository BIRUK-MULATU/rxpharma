package com.rxpharma.controller;

import com.rxpharma.dto.request.DrugRequest;
import com.rxpharma.dto.request.StockAdjustRequest;
import com.rxpharma.dto.response.DrugResponse;
import com.rxpharma.entity.Drug;
import com.rxpharma.service.DrugService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/drugs")
@RequiredArgsConstructor
public class DrugController {

    private final DrugService drugService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','PHARMACIST','CASHIER','SUPPLIER_MANAGER')")
    public ResponseEntity<Page<DrugResponse>> searchDrugs(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) Boolean lowStock,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expiringBefore,
            Pageable pageable) {

        Page<DrugResponse> drugs = drugService
                .searchDrugs(category, supplierId, lowStock, expiringBefore, pageable)
                .map(this::toResponse);
        return ResponseEntity.ok(drugs);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','PHARMACIST','CASHIER','SUPPLIER_MANAGER')")
    public ResponseEntity<DrugResponse> getDrugById(@PathVariable Long id) {
        return ResponseEntity.ok(toResponse(drugService.getDrugById(id)));
    }

    @GetMapping("/alerts/low-stock")
    @PreAuthorize("hasAnyRole('ADMIN','PHARMACIST')")
    public ResponseEntity<List<DrugResponse>> getLowStockDrugs(
            @RequestParam(defaultValue = "10") int threshold) {
        List<DrugResponse> drugs = drugService.getLowStockDrugs(threshold)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(drugs);
    }

    @GetMapping("/alerts/expiring-soon")
    @PreAuthorize("hasAnyRole('ADMIN','PHARMACIST')")
    public ResponseEntity<List<DrugResponse>> getExpiringSoonDrugs(
            @RequestParam(defaultValue = "30") int days) {
        List<DrugResponse> drugs = drugService.getExpiringSoonDrugs(days)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(drugs);
    }

    @GetMapping("/alerts/expired")
    @PreAuthorize("hasAnyRole('ADMIN','PHARMACIST')")
    public ResponseEntity<List<DrugResponse>> getExpiredDrugs() {
        List<DrugResponse> drugs = drugService.getExpiredDrugs()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(drugs);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','PHARMACIST')")
    public ResponseEntity<DrugResponse> createDrug(@Valid @RequestBody DrugRequest request) {
        Drug drug = drugService.createDrug(
                request.getName(), request.getSku(), request.getCategory(),
                request.getPrice(), request.getStockQty(),
                request.getExpiryDate(), request.getSupplierId()
        );
        return ResponseEntity.ok(toResponse(drug));
    }


    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','PHARMACIST')")
    public ResponseEntity<DrugResponse> updateDrug(@PathVariable Long id,
                                                   @RequestBody DrugRequest request) {
        Drug drug = drugService.updateDrug(
                id, request.getName(), request.getCategory(),
                request.getPrice(), request.getStockQty(), request.getExpiryDate(),
                request.getSupplierId()
        );
        return ResponseEntity.ok(toResponse(drug));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteDrug(@PathVariable Long id) {
        drugService.deleteDrug(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/stock")
    @PreAuthorize("hasAnyRole('ADMIN','PHARMACIST')")
    public ResponseEntity<DrugResponse> adjustStock(
            @PathVariable Long id,
            @Valid @RequestBody StockAdjustRequest request) {
        Drug drug = drugService.adjustStock(id, request.getQuantity(),
                request.getType(), request.getReason());
        return ResponseEntity.ok(toResponse(drug));
    }


private DrugResponse toResponse(Drug drug) {
    return DrugResponse.builder()
            .id(drug.getId())
            .name(drug.getName())
            .sku(drug.getSku())
            .category(drug.getCategory())
            .price(drug.getPrice())
            .stockQty(drug.getStockQty())
            .expiryDate(drug.getExpiryDate())
            .supplierId(drug.getSupplier() != null ? drug.getSupplier().getId() : null)
            .supplierName(drug.getSupplier() != null ? drug.getSupplier().getCompanyName() : "Unassigned")
            .lowStock(drug.getStockQty() < 10)
            .expiringSoon(drug.getExpiryDate() != null && drug.getExpiryDate().isBefore(LocalDate.now().plusDays(30)))
            .build();
}
}