package com.rxpharma.service;

import com.rxpharma.dto.request.StockAdjustRequest;
import com.rxpharma.entity.Drug;
import com.rxpharma.entity.Supplier;
import com.rxpharma.repository.DrugRepository;
import com.rxpharma.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DrugService {

    private final DrugRepository drugRepository;
    private final SupplierRepository supplierRepository;

    public Page<Drug> searchDrugs(String category, Long supplierId,
                                  Boolean lowStock, LocalDate expiringBefore,
                                  Pageable pageable) {
        return drugRepository.searchDrugs(category, supplierId, lowStock, expiringBefore, pageable);
    }

    public Drug getDrugById(Long id) {
        return drugRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Drug not found with id: " + id));
    }

    public Drug createDrug(String name, String sku, String category,
                           BigDecimal price, int stockQty,
                           LocalDate expiryDate, Long supplierId) {
        if (drugRepository.existsBySku(sku)) {
            throw new RuntimeException("SKU already exists: " + sku);
        }
        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new RuntimeException("Supplier not found with id: " + supplierId));

        Drug drug = Drug.builder()
                .name(name)
                .sku(sku)
                .category(category)
                .price(price)
                .stockQty(stockQty)
                .expiryDate(expiryDate)
                .supplier(supplier)
                .build();
        return drugRepository.save(drug);
    }

    public Drug updateDrug(Long id, String name, String category,
                           BigDecimal price, int stockQty, LocalDate expiryDate) {
        Drug drug = getDrugById(id);
        drug.setName(name);
        drug.setCategory(category);
        drug.setPrice(price);
        drug.setStockQty(stockQty);
        drug.setExpiryDate(expiryDate);
        return drugRepository.save(drug);
    }

    public void deleteDrug(Long id) {
        if (!drugRepository.existsById(id)) {
            throw new RuntimeException("Drug not found with id: " + id);
        }
        drugRepository.deleteById(id);
    }

    public List<Drug> getLowStockDrugs(int threshold) {
        return drugRepository.findAll()
                .stream()
                .filter(d -> d.getStockQty() < threshold)
                .collect(Collectors.toList());
    }

    public List<Drug> getExpiringSoonDrugs(int days) {
        LocalDate cutoff = LocalDate.now().plusDays(days);
        return drugRepository.findAll()
                .stream()
                .filter(d -> d.getExpiryDate() != null && d.getExpiryDate().isBefore(cutoff))
                .collect(Collectors.toList());
    }

    public List<Drug> getExpiredDrugs() {
        return drugRepository.findAll()
                .stream()
                .filter(d -> d.getExpiryDate() != null && d.getExpiryDate().isBefore(LocalDate.now()))
                .collect(Collectors.toList());
    }
    public Drug adjustStock(Long id, int quantity,
                            StockAdjustRequest.AdjustmentType type,
                            String reason) {
        Drug drug = getDrugById(id);

        switch (type) {
            case ADD -> drug.setStockQty(drug.getStockQty() + quantity);
            case SUBTRACT -> {
                if (drug.getStockQty() < quantity) {
                    throw new com.rxpharma.exception.BadRequestException(
                            "Insufficient stock. Current: " + drug.getStockQty());
                }
                drug.setStockQty(drug.getStockQty() - quantity);
            }
            case SET -> {
                if (quantity < 0) {
                    throw new com.rxpharma.exception.BadRequestException(
                            "Stock quantity cannot be negative");
                }
                drug.setStockQty(quantity);
            }
        }
        return drugRepository.save(drug);
    }
}
