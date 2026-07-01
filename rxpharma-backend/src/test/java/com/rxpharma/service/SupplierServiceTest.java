package com.rxpharma.service;

import com.rxpharma.dto.request.SupplierRequest;
import com.rxpharma.entity.Supplier;
import com.rxpharma.repository.SupplierRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SupplierService Tests")
class SupplierServiceTest {

    @Mock private SupplierRepository supplierRepository;

    @InjectMocks private SupplierService supplierService;

    private Supplier testSupplier;
    private SupplierRequest testRequest;

    @BeforeEach
    void setUp() {
        testSupplier = Supplier.builder()
                .id(1L)
                .companyName("MediSupply Ethiopia")
                .contactPerson("Abebe Kebede")
                .email("medi@supply.com")
                .phone("+251911234567")
                .status(Supplier.Status.ACTIVE)
                .supplierType(Supplier.SupplierType.WHOLESALER)
                .address("Addis Ababa, Bole")
                .build();

        testRequest = new SupplierRequest();
        testRequest.setCompanyName("MediSupply Ethiopia");
        testRequest.setContactPerson("Abebe Kebede");
        testRequest.setEmail("medi@supply.com");
        testRequest.setPhone("+251911234567");
        testRequest.setSupplierType(Supplier.SupplierType.WHOLESALER);
        testRequest.setAddress("Addis Ababa, Bole");
    }

    // ─── getAllSuppliers ──────────────────────────────────────────────────────

    @Test
    @DisplayName("getAllSuppliers - returns all suppliers")
    void getAllSuppliers_returnsAll() {
        when(supplierRepository.findAll()).thenReturn(List.of(testSupplier));

        List<Supplier> result = supplierService.getAllSuppliers();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCompanyName()).isEqualTo("MediSupply Ethiopia");
    }

    @Test
    @DisplayName("getAllSuppliers - empty list when none exist")
    void getAllSuppliers_empty() {
        when(supplierRepository.findAll()).thenReturn(List.of());

        assertThat(supplierService.getAllSuppliers()).isEmpty();
    }

    // ─── getSuppliersByType ───────────────────────────────────────────────────

    @Test
    @DisplayName("getSuppliersByType WHOLESALER - returns only wholesalers")
    void getSuppliersByType_wholesaler() {
        when(supplierRepository.findBySupplierType(Supplier.SupplierType.WHOLESALER))
                .thenReturn(List.of(testSupplier));

        List<Supplier> result = supplierService.getSuppliersByType(Supplier.SupplierType.WHOLESALER);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSupplierType()).isEqualTo(Supplier.SupplierType.WHOLESALER);
    }

    @Test
    @DisplayName("getSuppliersByType IMPORTER - returns only importers")
    void getSuppliersByType_importer() {
        Supplier importer = Supplier.builder().id(2L).companyName("Global Imports")
                .supplierType(Supplier.SupplierType.IMPORTER).build();

        when(supplierRepository.findBySupplierType(Supplier.SupplierType.IMPORTER))
                .thenReturn(List.of(importer));

        List<Supplier> result = supplierService.getSuppliersByType(Supplier.SupplierType.IMPORTER);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSupplierType()).isEqualTo(Supplier.SupplierType.IMPORTER);
    }

    // ─── getSupplierById ─────────────────────────────────────────────────────

    @Test
    @DisplayName("getSupplierById - found returns supplier")
    void getSupplierById_found() {
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(testSupplier));

        Supplier result = supplierService.getSupplierById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getEmail()).isEqualTo("medi@supply.com");
    }

    @Test
    @DisplayName("getSupplierById - not found throws RuntimeException")
    void getSupplierById_notFound() {
        when(supplierRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> supplierService.getSupplierById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Supplier not found with id: 99");
    }

    // ─── createSupplier ──────────────────────────────────────────────────────

    @Test
    @DisplayName("createSupplier - success saves and returns supplier")
    void createSupplier_success() {
        when(supplierRepository.existsByEmail("medi@supply.com")).thenReturn(false);
        when(supplierRepository.save(any(Supplier.class))).thenReturn(testSupplier);

        Supplier result = supplierService.createSupplier(testRequest);

        assertThat(result).isNotNull();
        assertThat(result.getCompanyName()).isEqualTo("MediSupply Ethiopia");
        assertThat(result.getStatus()).isEqualTo(Supplier.Status.ACTIVE);
        verify(supplierRepository).save(any(Supplier.class));
    }

    @Test
    @DisplayName("createSupplier - defaults to WHOLESALER when type not specified")
    void createSupplier_defaultsToWholesaler() {
        testRequest.setSupplierType(null);
        when(supplierRepository.existsByEmail(anyString())).thenReturn(false);
        when(supplierRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Supplier result = supplierService.createSupplier(testRequest);

        assertThat(result.getSupplierType()).isEqualTo(Supplier.SupplierType.WHOLESALER);
    }

    @Test
    @DisplayName("createSupplier - duplicate email throws RuntimeException")
    void createSupplier_duplicateEmail() {
        when(supplierRepository.existsByEmail("medi@supply.com")).thenReturn(true);

        assertThatThrownBy(() -> supplierService.createSupplier(testRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Email already in use");

        verify(supplierRepository, never()).save(any());
    }

    @Test
    @DisplayName("createSupplier - IMPORTER type is persisted correctly")
    void createSupplier_importerType() {
        testRequest.setSupplierType(Supplier.SupplierType.IMPORTER);
        testRequest.setEmail("importer@supply.com");
        when(supplierRepository.existsByEmail("importer@supply.com")).thenReturn(false);
        when(supplierRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Supplier result = supplierService.createSupplier(testRequest);

        assertThat(result.getSupplierType()).isEqualTo(Supplier.SupplierType.IMPORTER);
    }

    // ─── updateSupplier ──────────────────────────────────────────────────────

    @Test
    @DisplayName("updateSupplier - updates all fields")
    void updateSupplier_success() {
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(testSupplier));
        when(supplierRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        SupplierRequest update = new SupplierRequest();
        update.setCompanyName("Updated Supply Co");
        update.setContactPerson("Dawit Alemu");
        update.setEmail("updated@supply.com");
        update.setPhone("+251922345678");
        update.setStatus(Supplier.Status.ON_HOLD);
        update.setSupplierType(Supplier.SupplierType.IMPORTER);
        update.setAddress("Addis Ababa, Kazanchis");

        Supplier result = supplierService.updateSupplier(1L, update);

        assertThat(result.getCompanyName()).isEqualTo("Updated Supply Co");
        assertThat(result.getContactPerson()).isEqualTo("Dawit Alemu");
        assertThat(result.getStatus()).isEqualTo(Supplier.Status.ON_HOLD);
        assertThat(result.getSupplierType()).isEqualTo(Supplier.SupplierType.IMPORTER);
    }

    @Test
    @DisplayName("updateSupplier - null status keeps existing status")
    void updateSupplier_nullStatus_keepsExisting() {
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(testSupplier));
        when(supplierRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        SupplierRequest update = new SupplierRequest();
        update.setCompanyName("Same");
        update.setContactPerson("Same");
        update.setEmail("same@email.com");
        update.setPhone("+251911111111");
        update.setStatus(null);

        Supplier result = supplierService.updateSupplier(1L, update);

        assertThat(result.getStatus()).isEqualTo(Supplier.Status.ACTIVE);
    }

    @Test
    @DisplayName("updateSupplier - not found throws RuntimeException")
    void updateSupplier_notFound() {
        when(supplierRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> supplierService.updateSupplier(99L, testRequest))
                .isInstanceOf(RuntimeException.class);
    }

    // ─── deleteSupplier ──────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteSupplier - success deletes supplier")
    void deleteSupplier_success() {
        when(supplierRepository.existsById(1L)).thenReturn(true);

        supplierService.deleteSupplier(1L);

        verify(supplierRepository).deleteById(1L);
    }

    @Test
    @DisplayName("deleteSupplier - not found throws RuntimeException")
    void deleteSupplier_notFound() {
        when(supplierRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> supplierService.deleteSupplier(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Supplier not found");

        verify(supplierRepository, never()).deleteById(any());
    }
}