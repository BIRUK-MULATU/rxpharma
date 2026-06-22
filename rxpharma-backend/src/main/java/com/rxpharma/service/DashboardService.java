package com.rxpharma.service;

import com.rxpharma.dto.response.DashboardResponse;
import com.rxpharma.entity.Prescription;
import com.rxpharma.entity.PurchaseOrder;
import com.rxpharma.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final UserRepository userRepository;
    private final DrugRepository drugRepository;
    private final SupplierRepository supplierRepository;
    private final SaleRepository saleRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;

    public DashboardResponse getStats() {

        long lowStockCount = drugRepository.findAll()
                .stream()
                .filter(d -> d.getStockQty() < 10)
                .count();

        long expiringSoonCount = drugRepository.findAll()
                .stream()
                .filter(d -> d.getExpiryDate() != null && d.getExpiryDate().isBefore(LocalDate.now().plusDays(30)))
                .count();

        long pendingPrescriptions = prescriptionRepository
                .findByStatus(Prescription.Status.PENDING,
                        org.springframework.data.domain.Pageable.unpaged())
                .getTotalElements();

        // Updated to use DRAFT instead of PENDING
        long pendingOrders = purchaseOrderRepository
                .findByStatus(PurchaseOrder.Status.DRAFT,
                        org.springframework.data.domain.Pageable.unpaged())
                .getTotalElements();

        return DashboardResponse.builder()
                .totalUsers(userRepository.count())
                .totalDrugs(drugRepository.count())
                .totalSuppliers(supplierRepository.count())
                .totalSales(saleRepository.count())
                .totalPrescriptions(prescriptionRepository.count())
                .totalPurchaseOrders(purchaseOrderRepository.count())
                .lowStockCount(lowStockCount)
                .expiringSoonCount(expiringSoonCount)
                .pendingPrescriptions(pendingPrescriptions)
                .pendingOrders(pendingOrders)
                .build();
    }
}