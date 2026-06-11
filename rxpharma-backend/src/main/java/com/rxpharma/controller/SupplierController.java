package com.rxpharma.controller;

import com.rxpharma.dto.request.SupplierRequest;
import com.rxpharma.dto.response.SupplierResponse;
import com.rxpharma.entity.Supplier;
import com.rxpharma.service.SupplierService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/suppliers")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierService supplierService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPPLIER_MANAGER','PHARMACIST')")
    public ResponseEntity<List<SupplierResponse>> getAllSuppliers(
            @RequestParam(required = false) String type) {

        List<Supplier> suppliers;
        if (type != null && !type.isBlank()) {
            try {
                Supplier.SupplierType supplierType = Supplier.SupplierType.valueOf(type.toUpperCase());
                suppliers = supplierService.getSuppliersByType(supplierType);
            } catch (IllegalArgumentException e) {
                suppliers = supplierService.getAllSuppliers();
            }
        } else {
            suppliers = supplierService.getAllSuppliers();
        }

        List<SupplierResponse> response = suppliers.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPPLIER_MANAGER')")
    public ResponseEntity<SupplierResponse> getSupplierById(@PathVariable Long id) {
        return ResponseEntity.ok(toResponse(supplierService.getSupplierById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPPLIER_MANAGER')")
    public ResponseEntity<SupplierResponse> createSupplier(@Valid @RequestBody SupplierRequest request) {
        Supplier supplier = supplierService.createSupplier(request);
        return ResponseEntity.ok(toResponse(supplier));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPPLIER_MANAGER')")
    public ResponseEntity<SupplierResponse> updateSupplier(@PathVariable Long id,
                                                           @Valid @RequestBody SupplierRequest request) {
        Supplier supplier = supplierService.updateSupplier(id, request);
        return ResponseEntity.ok(toResponse(supplier));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteSupplier(@PathVariable Long id) {
        supplierService.deleteSupplier(id);
        return ResponseEntity.noContent().build();
    }

    private SupplierResponse toResponse(Supplier supplier) {
        return SupplierResponse.builder()
                .id(supplier.getId())
                .companyName(supplier.getCompanyName())
                .contactPerson(supplier.getContactPerson())
                .email(supplier.getEmail())
                .phone(supplier.getPhone())
                .status(supplier.getStatus().name())
                .supplierType(supplier.getSupplierType() != null
                        ? supplier.getSupplierType().name() : "WHOLESALER")
                .address(supplier.getAddress())
                .createdAt(supplier.getCreatedAt())
                .build();
    }
}