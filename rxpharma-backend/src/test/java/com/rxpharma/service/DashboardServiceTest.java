package com.rxpharma.service;

import com.rxpharma.dto.response.DashboardResponse;
import com.rxpharma.entity.Drug;
import com.rxpharma.entity.Prescription;
import com.rxpharma.entity.PurchaseOrder;
import com.rxpharma.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DashboardService Tests")
class DashboardServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private DrugRepository drugRepository;
    @Mock private SupplierRepository supplierRepository;
    @Mock private SaleRepository saleRepository;
    @Mock private PrescriptionRepository prescriptionRepository;
    @Mock private PurchaseOrderRepository purchaseOrderRepository;

    @InjectMocks private DashboardService dashboardService;

    private Drug normalDrug;
    private Drug lowStockDrug;
    private Drug expiringDrug;
    private Drug expiredDrug;

    @BeforeEach
    void setUp() {
        normalDrug = Drug.builder()
                .id(1L).name("Amoxicillin").stockQty(100)
                .expiryDate(LocalDate.now().plusYears(2)).build();

        lowStockDrug = Drug.builder()
                .id(2L).name("Paracetamol").stockQty(5)
                .expiryDate(LocalDate.now().plusYears(1)).build();

        expiringDrug = Drug.builder()
                .id(3L).name("Aspirin").stockQty(50)
                .expiryDate(LocalDate.now().plusDays(15)).build();

        expiredDrug = Drug.builder()
                .id(4L).name("Ibuprofen").stockQty(0)
                .expiryDate(LocalDate.now().minusDays(1)).build();
    }

    @Test
    @DisplayName("getStats - returns all counts and aggregations")
    void getStats_returnsAllFields() {
        when(userRepository.count()).thenReturn(10L);
        when(drugRepository.count()).thenReturn(50L);
        when(supplierRepository.count()).thenReturn(5L);
        when(saleRepository.count()).thenReturn(200L);
        when(prescriptionRepository.count()).thenReturn(100L);
        when(purchaseOrderRepository.count()).thenReturn(30L);

        when(drugRepository.findAll()).thenReturn(List.of(normalDrug, lowStockDrug, expiringDrug, expiredDrug));

        Page<Prescription> pendingPrescriptionsPage = new PageImpl<>(List.of(new Prescription()));
        when(prescriptionRepository.findByStatus(Prescription.Status.PENDING, Pageable.unpaged()))
                .thenReturn(pendingPrescriptionsPage);

        Page<PurchaseOrder> draftOrdersPage = new PageImpl<>(List.of(new PurchaseOrder(), new PurchaseOrder()));
        when(purchaseOrderRepository.findByStatus(PurchaseOrder.Status.DRAFT, Pageable.unpaged()))
                .thenReturn(draftOrdersPage);

        DashboardResponse result = dashboardService.getStats();

        assertThat(result.getTotalUsers()).isEqualTo(10);
        assertThat(result.getTotalDrugs()).isEqualTo(50);
        assertThat(result.getTotalSuppliers()).isEqualTo(5);
        assertThat(result.getTotalSales()).isEqualTo(200);
        assertThat(result.getTotalPrescriptions()).isEqualTo(100);
        assertThat(result.getTotalPurchaseOrders()).isEqualTo(30);

        assertThat(result.getLowStockCount()).isEqualTo(2);        // lowStockDrug (5) + expiredDrug (0)
        assertThat(result.getExpiringSoonCount()).isEqualTo(2);    // expiringDrug + expiredDrug (within 30 days or past)
        assertThat(result.getPendingPrescriptions()).isEqualTo(1);
        assertThat(result.getPendingOrders()).isEqualTo(2);
    }

    @Test
    @DisplayName("getStats - zero counts when nothing exists")
    void getStats_empty() {
        when(userRepository.count()).thenReturn(0L);
        when(drugRepository.count()).thenReturn(0L);
        when(supplierRepository.count()).thenReturn(0L);
        when(saleRepository.count()).thenReturn(0L);
        when(prescriptionRepository.count()).thenReturn(0L);
        when(purchaseOrderRepository.count()).thenReturn(0L);
        when(drugRepository.findAll()).thenReturn(List.of());

        Page<Prescription> emptyPrescriptions = Page.empty();
        when(prescriptionRepository.findByStatus(Prescription.Status.PENDING, Pageable.unpaged()))
                .thenReturn(emptyPrescriptions);

        Page<PurchaseOrder> emptyOrders = Page.empty();
        when(purchaseOrderRepository.findByStatus(PurchaseOrder.Status.DRAFT, Pageable.unpaged()))
                .thenReturn(emptyOrders);

        DashboardResponse result = dashboardService.getStats();

        assertThat(result.getTotalUsers()).isZero();
        assertThat(result.getTotalDrugs()).isZero();
        assertThat(result.getTotalSuppliers()).isZero();
        assertThat(result.getTotalSales()).isZero();
        assertThat(result.getTotalPrescriptions()).isZero();
        assertThat(result.getTotalPurchaseOrders()).isZero();
        assertThat(result.getLowStockCount()).isZero();
        assertThat(result.getExpiringSoonCount()).isZero();
        assertThat(result.getPendingPrescriptions()).isZero();
        assertThat(result.getPendingOrders()).isZero();
    }

    @Test
    @DisplayName("getStats - drugs with null expiry date are excluded from expiring count")
    void getStats_nullExpiryIgnored() {
        Drug noExpiry = Drug.builder().id(5L).name("NoExpiry").stockQty(20).expiryDate(null).build();

        when(drugRepository.findAll()).thenReturn(List.of(noExpiry));
        when(userRepository.count()).thenReturn(1L);
        when(drugRepository.count()).thenReturn(1L);
        when(supplierRepository.count()).thenReturn(0L);
        when(saleRepository.count()).thenReturn(0L);
        when(prescriptionRepository.count()).thenReturn(0L);
        when(purchaseOrderRepository.count()).thenReturn(0L);
        when(prescriptionRepository.findByStatus(any(), any())).thenReturn(Page.empty());
        when(purchaseOrderRepository.findByStatus(any(), any())).thenReturn(Page.empty());

        DashboardResponse result = dashboardService.getStats();

        assertThat(result.getExpiringSoonCount()).isZero();
    }

    @Test
    @DisplayName("getStats - all drugs are low stock when threshold is high")
    void getStats_lowStockBoundary() {
        Drug exactlyTen = Drug.builder().id(6L).name("ExactlyTen").stockQty(10).build();

        when(drugRepository.findAll()).thenReturn(List.of(exactlyTen));
        when(userRepository.count()).thenReturn(0L);
        when(drugRepository.count()).thenReturn(1L);
        when(supplierRepository.count()).thenReturn(0L);
        when(saleRepository.count()).thenReturn(0L);
        when(prescriptionRepository.count()).thenReturn(0L);
        when(purchaseOrderRepository.count()).thenReturn(0L);
        when(prescriptionRepository.findByStatus(any(), any())).thenReturn(Page.empty());
        when(purchaseOrderRepository.findByStatus(any(), any())).thenReturn(Page.empty());

        DashboardResponse result = dashboardService.getStats();

        // stockQty = 10 is NOT less than 10
        assertThat(result.getLowStockCount()).isZero();
    }
}
