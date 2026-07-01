package com.rxpharma.service;

import com.rxpharma.dto.request.StockAdjustRequest;
import com.rxpharma.entity.Drug;
import com.rxpharma.entity.Supplier;
import com.rxpharma.exception.BadRequestException;
import com.rxpharma.repository.DrugRepository;
import com.rxpharma.repository.SupplierRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DrugService Tests")
class DrugServiceTest {

    @Mock private DrugRepository drugRepository;
    @Mock private SupplierRepository supplierRepository;

    @InjectMocks private DrugService drugService;

    private Drug testDrug;
    private Supplier testSupplier;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        testSupplier = Supplier.builder()
                .id(1L)
                .companyName("MediSupply Ethiopia")
                .email("medi@supply.com")
                .phone("+251911234567")
                .status(Supplier.Status.ACTIVE)
                .build();

        testDrug = Drug.builder()
                .id(1L)
                .name("Amoxicillin 500mg")
                .sku("AMX-500")
                .category("Antibiotics")
                .price(new BigDecimal("25.00"))
                .stockQty(100)
                .expiryDate(LocalDate.now().plusYears(2))
                .supplier(testSupplier)
                .build();

        pageable = PageRequest.of(0, 10);
    }

    // ─── searchDrugs ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("searchDrugs - returns paged results")
    void searchDrugs_returnsPage() {
        Page<Drug> page = new PageImpl<>(List.of(testDrug));
        when(drugRepository.searchDrugs(any(), any(), any(), any(), any(Pageable.class))).thenReturn(page);

        Page<Drug> result = drugService.searchDrugs(null, null, null, null, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Amoxicillin 500mg");
    }

    @Test
    @DisplayName("searchDrugs - with category filter passes it through")
    void searchDrugs_withCategory() {
        Page<Drug> page = new PageImpl<>(List.of(testDrug));
        when(drugRepository.searchDrugs(eq("Antibiotics"), any(), any(), any(), any())).thenReturn(page);

        Page<Drug> result = drugService.searchDrugs("Antibiotics", null, null, null, pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    // ─── getDrugById ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("getDrugById - found returns drug")
    void getDrugById_found() {
        when(drugRepository.findById(1L)).thenReturn(Optional.of(testDrug));

        Drug result = drugService.getDrugById(1L);

        assertThat(result.getName()).isEqualTo("Amoxicillin 500mg");
        assertThat(result.getSku()).isEqualTo("AMX-500");
    }

    @Test
    @DisplayName("getDrugById - not found throws RuntimeException")
    void getDrugById_notFound() {
        when(drugRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> drugService.getDrugById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Drug not found with id: 99");
    }

    // ─── createDrug ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("createDrug - success saves and returns drug")
    void createDrug_success() {
        when(drugRepository.existsBySku("AMX-500")).thenReturn(false);
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(testSupplier));
        when(drugRepository.save(any(Drug.class))).thenReturn(testDrug);

        Drug result = drugService.createDrug("Amoxicillin 500mg", "AMX-500",
                "Antibiotics", new BigDecimal("25.00"), 100,
                LocalDate.now().plusYears(2), 1L);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Amoxicillin 500mg");
        verify(drugRepository).save(any(Drug.class));
    }

    @Test
    @DisplayName("createDrug - duplicate SKU throws RuntimeException")
    void createDrug_duplicateSku() {
        when(drugRepository.existsBySku("AMX-500")).thenReturn(true);

        assertThatThrownBy(() -> drugService.createDrug("Amox", "AMX-500",
                "Antibiotics", new BigDecimal("25.00"), 100,
                LocalDate.now().plusYears(2), 1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("SKU already exists");

        verify(drugRepository, never()).save(any());
    }

    @Test
    @DisplayName("createDrug - supplier not found throws RuntimeException")
    void createDrug_supplierNotFound() {
        when(drugRepository.existsBySku("AMX-500")).thenReturn(false);
        when(supplierRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> drugService.createDrug("Amox", "AMX-500",
                "Antibiotics", new BigDecimal("25.00"), 100,
                LocalDate.now().plusYears(2), 99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Supplier not found");
    }

    // ─── updateDrug ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateDrug - updates all fields correctly")
    void updateDrug_success() {
        when(drugRepository.findById(1L)).thenReturn(Optional.of(testDrug));
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(testSupplier));
        when(drugRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Drug result = drugService.updateDrug(1L, "Amoxicillin 250mg", "Antibiotics",
                new BigDecimal("15.00"), 50,
                LocalDate.now().plusYears(1), 1L);

        assertThat(result.getName()).isEqualTo("Amoxicillin 250mg");
        assertThat(result.getPrice()).isEqualByComparingTo("15.00");
        assertThat(result.getStockQty()).isEqualTo(50);
    }

    @Test
    @DisplayName("updateDrug - null supplierId keeps existing supplier")
    void updateDrug_nullSupplierId_keepsExistingSupplier() {
        when(drugRepository.findById(1L)).thenReturn(Optional.of(testDrug));
        when(drugRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Drug result = drugService.updateDrug(1L, "Updated", "Cat",
                new BigDecimal("10.00"), 20,
                LocalDate.now().plusYears(1), null);

        assertThat(result.getSupplier()).isEqualTo(testSupplier);
        verify(supplierRepository, never()).findById(any());
    }

    @Test
    @DisplayName("updateDrug - drug not found throws exception")
    void updateDrug_notFound() {
        when(drugRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> drugService.updateDrug(99L, "Name", "Cat",
                BigDecimal.TEN, 10, LocalDate.now().plusYears(1), null))
                .isInstanceOf(RuntimeException.class);
    }

    // ─── deleteDrug ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteDrug - success deletes drug")
    void deleteDrug_success() {
        when(drugRepository.existsById(1L)).thenReturn(true);

        drugService.deleteDrug(1L);

        verify(drugRepository).deleteById(1L);
    }

    @Test
    @DisplayName("deleteDrug - not found throws RuntimeException")
    void deleteDrug_notFound() {
        when(drugRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> drugService.deleteDrug(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Drug not found");

        verify(drugRepository, never()).deleteById(any());
    }

    // ─── getLowStockDrugs ────────────────────────────────────────────────────

    @Test
    @DisplayName("getLowStockDrugs - returns drugs below threshold")
    void getLowStockDrugs_returnsLowStock() {
        Drug lowStockDrug = Drug.builder().id(2L).name("Paracetamol").stockQty(5).build();
        when(drugRepository.findAll()).thenReturn(List.of(testDrug, lowStockDrug));

        List<Drug> result = drugService.getLowStockDrugs(10);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Paracetamol");
    }

    @Test
    @DisplayName("getLowStockDrugs - returns empty when all above threshold")
    void getLowStockDrugs_noneBelow() {
        when(drugRepository.findAll()).thenReturn(List.of(testDrug));

        List<Drug> result = drugService.getLowStockDrugs(10);

        assertThat(result).isEmpty();
    }

    // ─── getExpiringSoonDrugs ────────────────────────────────────────────────

    @Test
    @DisplayName("getExpiringSoonDrugs - returns drugs expiring within threshold")
    void getExpiringSoonDrugs_returnsExpiring() {
        Drug expiringDrug = Drug.builder().id(3L).name("Aspirin")
                .expiryDate(LocalDate.now().plusDays(10)).build();
        when(drugRepository.findAll()).thenReturn(List.of(testDrug, expiringDrug));

        List<Drug> result = drugService.getExpiringSoonDrugs(30);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Aspirin");
    }

    @Test
    @DisplayName("getExpiringSoonDrugs - ignores drugs with null expiry date")
    void getExpiringSoonDrugs_nullExpiryIgnored() {
        Drug noExpiry = Drug.builder().id(4L).name("NoExpiry").expiryDate(null).build();
        when(drugRepository.findAll()).thenReturn(List.of(noExpiry));

        List<Drug> result = drugService.getExpiringSoonDrugs(30);

        assertThat(result).isEmpty();
    }

    // ─── getExpiredDrugs ─────────────────────────────────────────────────────

    @Test
    @DisplayName("getExpiredDrugs - returns drugs past expiry date")
    void getExpiredDrugs_returnsExpired() {
        Drug expiredDrug = Drug.builder().id(5L).name("Expired")
                .expiryDate(LocalDate.now().minusDays(1)).build();
        when(drugRepository.findAll()).thenReturn(List.of(testDrug, expiredDrug));

        List<Drug> result = drugService.getExpiredDrugs();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Expired");
    }

    // ─── adjustStock ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("adjustStock ADD - adds quantity to existing stock")
    void adjustStock_add_success() {
        when(drugRepository.findById(1L)).thenReturn(Optional.of(testDrug));
        when(drugRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Drug result = drugService.adjustStock(1L, 50,
                StockAdjustRequest.AdjustmentType.ADD, "Restocked");

        assertThat(result.getStockQty()).isEqualTo(150);
    }

    @Test
    @DisplayName("adjustStock SUBTRACT - deducts quantity from stock")
    void adjustStock_subtract_success() {
        when(drugRepository.findById(1L)).thenReturn(Optional.of(testDrug));
        when(drugRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Drug result = drugService.adjustStock(1L, 30,
                StockAdjustRequest.AdjustmentType.SUBTRACT, "Dispensed");

        assertThat(result.getStockQty()).isEqualTo(70);
    }

    @Test
    @DisplayName("adjustStock SUBTRACT - insufficient stock throws BadRequestException")
    void adjustStock_subtract_insufficientStock() {
        when(drugRepository.findById(1L)).thenReturn(Optional.of(testDrug));

        assertThatThrownBy(() -> drugService.adjustStock(1L, 200,
                StockAdjustRequest.AdjustmentType.SUBTRACT, "Too many"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Insufficient stock");
    }

    @Test
    @DisplayName("adjustStock SET - sets stock to exact quantity")
    void adjustStock_set_success() {
        when(drugRepository.findById(1L)).thenReturn(Optional.of(testDrug));
        when(drugRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Drug result = drugService.adjustStock(1L, 75,
                StockAdjustRequest.AdjustmentType.SET, "Manual count");

        assertThat(result.getStockQty()).isEqualTo(75);
    }

    @Test
    @DisplayName("adjustStock SET - negative quantity throws BadRequestException")
    void adjustStock_set_negativeQuantity() {
        when(drugRepository.findById(1L)).thenReturn(Optional.of(testDrug));

        assertThatThrownBy(() -> drugService.adjustStock(1L, -10,
                StockAdjustRequest.AdjustmentType.SET, "Invalid"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("cannot be negative");
    }

    @Test
    @DisplayName("adjustStock - drug not found throws RuntimeException")
    void adjustStock_drugNotFound() {
        when(drugRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> drugService.adjustStock(99L, 10,
                StockAdjustRequest.AdjustmentType.ADD, "Test"))
                .isInstanceOf(RuntimeException.class);
    }
}