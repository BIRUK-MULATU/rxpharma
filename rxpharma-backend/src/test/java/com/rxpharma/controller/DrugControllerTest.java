package com.rxpharma.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rxpharma.dto.request.DrugRequest;
import com.rxpharma.dto.request.StockAdjustRequest;
import com.rxpharma.entity.Drug;
import com.rxpharma.entity.Supplier;
import com.rxpharma.security.CustomUserDetailsService;
import com.rxpharma.security.JwtUtil;
import com.rxpharma.service.DrugService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = DrugController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
class DrugControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DrugService drugService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    private Drug createTestDrug() {
        Supplier supplier = Supplier.builder().id(1L).companyName("MediSupply").build();
        return Drug.builder().id(1L).name("Amoxicillin").sku("AMX-500")
                .category("Antibiotics").price(new BigDecimal("25.00"))
                .stockQty(100).expiryDate(LocalDate.now().plusYears(2))
                .supplier(supplier).build();
    }

    @Test
    void searchDrugs_returnsPage() throws Exception {
        Page<Drug> page = new PageImpl<>(List.of(createTestDrug()));
        when(drugService.searchDrugs(any(), any(), any(), any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/drugs").param("category", "Antibiotics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Amoxicillin"));
    }

    @Test
    void getDrugById_found() throws Exception {
        when(drugService.getDrugById(1L)).thenReturn(createTestDrug());

        mockMvc.perform(get("/api/drugs/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Amoxicillin"))
                .andExpect(jsonPath("$.sku").value("AMX-500"));
    }

    @Test
    void createDrug_success() throws Exception {
        DrugRequest request = new DrugRequest();
        request.setName("Paracetamol");
        request.setSku("PAR-500");
        request.setCategory("Analgesics");
        request.setPrice(new BigDecimal("10.00"));
        request.setStockQty(200);
        request.setExpiryDate(LocalDate.now().plusYears(1));
        request.setSupplierId(1L);

        Drug drug = Drug.builder().id(2L).name("Paracetamol").sku("PAR-500").build();
        when(drugService.createDrug(anyString(), anyString(), anyString(), any(), anyInt(), any(), anyLong()))
                .thenReturn(drug);

        mockMvc.perform(post("/api/drugs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Paracetamol"));
    }

    @Test
    void deleteDrug_noContent() throws Exception {
        mockMvc.perform(delete("/api/drugs/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void adjustStock_success() throws Exception {
        StockAdjustRequest request = new StockAdjustRequest();
        request.setQuantity(50);
        request.setType(StockAdjustRequest.AdjustmentType.ADD);
        request.setReason("Restocked");

        Drug drug = createTestDrug();
        drug.setStockQty(150);
        when(drugService.adjustStock(anyLong(), anyInt(), any(), anyString())).thenReturn(drug);

        mockMvc.perform(patch("/api/drugs/1/stock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockQty").value(150));
    }

    @Test
    void getLowStockDrugs_returnsList() throws Exception {
        Drug low = Drug.builder().id(3L).name("Low Stock").stockQty(5).build();
        when(drugService.getLowStockDrugs(10)).thenReturn(List.of(low));

        mockMvc.perform(get("/api/drugs/alerts/low-stock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Low Stock"));
    }

    @Test
    void getExpiringSoonDrugs_returnsList() throws Exception {
        Drug exp = Drug.builder().id(4L).name("Expiring").expiryDate(LocalDate.now().plusDays(10)).build();
        when(drugService.getExpiringSoonDrugs(30)).thenReturn(List.of(exp));

        mockMvc.perform(get("/api/drugs/alerts/expiring-soon"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Expiring"));
    }

    @Test
    void getExpiredDrugs_returnsList() throws Exception {
        Drug exp = Drug.builder().id(5L).name("Expired").expiryDate(LocalDate.now().minusDays(1)).build();
        when(drugService.getExpiredDrugs()).thenReturn(List.of(exp));

        mockMvc.perform(get("/api/drugs/alerts/expired"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Expired"));
    }
}
