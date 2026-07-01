package com.rxpharma.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rxpharma.dto.request.SupplierRequest;
import com.rxpharma.entity.Supplier;
import com.rxpharma.security.CustomUserDetailsService;
import com.rxpharma.security.JwtUtil;
import com.rxpharma.service.SupplierService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = SupplierController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
class SupplierControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SupplierService supplierService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    private Supplier createTestSupplier() {
        return Supplier.builder().id(1L).companyName("MediSupply Ethiopia")
                .contactPerson("Abebe").email("medi@supply.com")
                .phone("+251911234567").status(Supplier.Status.ACTIVE)
                .supplierType(Supplier.SupplierType.WHOLESALER)
                .address("Addis Ababa").build();
    }

    @Test
    void getAllSuppliers_returnsList() throws Exception {
        when(supplierService.getAllSuppliers()).thenReturn(List.of(createTestSupplier()));

        mockMvc.perform(get("/api/suppliers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].companyName").value("MediSupply Ethiopia"));
    }

    @Test
    void getAllSuppliers_withTypeFilter() throws Exception {
        when(supplierService.getSuppliersByType(Supplier.SupplierType.WHOLESALER))
                .thenReturn(List.of(createTestSupplier()));

        mockMvc.perform(get("/api/suppliers").param("type", "WHOLESALER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].supplierType").value("WHOLESALER"));
    }

    @Test
    void getSupplierById_found() throws Exception {
        when(supplierService.getSupplierById(1L)).thenReturn(createTestSupplier());

        mockMvc.perform(get("/api/suppliers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyName").value("MediSupply Ethiopia"));
    }

    @Test
    void createSupplier_success() throws Exception {
        SupplierRequest request = new SupplierRequest();
        request.setCompanyName("New Supplier");
        request.setContactPerson("Kebede");
        request.setEmail("new@supply.com");
        request.setPhone("+251911111111");
        request.setSupplierType(Supplier.SupplierType.IMPORTER);

        Supplier created = Supplier.builder().id(2L).companyName("New Supplier")
                .status(Supplier.Status.ACTIVE).supplierType(Supplier.SupplierType.IMPORTER).build();
        when(supplierService.createSupplier(any())).thenReturn(created);

        mockMvc.perform(post("/api/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyName").value("New Supplier"));
    }

    @Test
    void updateSupplier_success() throws Exception {
        SupplierRequest request = new SupplierRequest();
        request.setCompanyName("Updated");
        request.setContactPerson("Dawit");
        request.setEmail("updated@supply.com");
        request.setPhone("+251922222222");

        Supplier updated = Supplier.builder().id(1L).companyName("Updated")
                .status(Supplier.Status.ACTIVE).supplierType(Supplier.SupplierType.WHOLESALER).build();
        when(supplierService.updateSupplier(eq(1L), any())).thenReturn(updated);

        mockMvc.perform(put("/api/suppliers/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyName").value("Updated"));
    }

    @Test
    void deleteSupplier_noContent() throws Exception {
        mockMvc.perform(delete("/api/suppliers/1"))
                .andExpect(status().isNoContent());
    }
}
