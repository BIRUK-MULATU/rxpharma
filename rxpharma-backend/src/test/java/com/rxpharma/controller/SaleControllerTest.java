package com.rxpharma.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rxpharma.dto.request.SaleRequest;
import com.rxpharma.dto.response.InvoiceResponse;
import com.rxpharma.dto.response.SaleItemResponse;
import com.rxpharma.entity.Sale;
import com.rxpharma.entity.User;
import com.rxpharma.security.CustomUserDetailsService;
import com.rxpharma.security.JwtUtil;
import com.rxpharma.service.SaleService;
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
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = SaleController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
class SaleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SaleService saleService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    private Sale createTestSale() {
        User cashier = User.builder().id(1L).fullName("Tigist Cashier").build();
        return Sale.builder().id(1L).invoiceNumber("INV-TEST123")
                .cashier(cashier).patientName("Abebe")
                .paymentMethod(Sale.PaymentMethod.CASH)
                .totalAmount(new BigDecimal("287.50"))
                .taxAmount(new BigDecimal("37.50"))
                .saleDate(LocalDateTime.now()).build();
    }

    @Test
    void getAllSales_returnsPage() throws Exception {
        Page<Sale> page = new PageImpl<>(List.of(createTestSale()));
        when(saleService.getAllSales(any())).thenReturn(page);

        mockMvc.perform(get("/api/sales"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].invoiceNumber").value("INV-TEST123"));
    }

    @Test
    void getSaleById_found() throws Exception {
        when(saleService.getSaleById(1L)).thenReturn(createTestSale());

        mockMvc.perform(get("/api/sales/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invoiceNumber").value("INV-TEST123"));
    }

    @Test
    void createSale_success() throws Exception {
        SaleRequest.SaleItemRequest item = new SaleRequest.SaleItemRequest();
        item.setDrugId(1L);
        item.setQuantity(10);

        SaleRequest request = new SaleRequest();
        request.setCashierId(1L);
        request.setPatientName("Abebe");
        request.setPaymentMethod(Sale.PaymentMethod.CASH);
        request.setItems(List.of(item));

        when(saleService.createSale(anyLong(), anyString(), any(), anyList()))
                .thenReturn(createTestSale());

        mockMvc.perform(post("/api/sales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invoiceNumber").value("INV-TEST123"));
    }

    @Test
    void getSaleItems_returnsList() throws Exception {
        SaleItemResponse item = SaleItemResponse.builder()
                .drugId(1L).drugName("Amoxicillin").quantity(10)
                .unitPrice(new BigDecimal("25.00")).subtotal(new BigDecimal("250.00")).build();
        when(saleService.getSaleItems(1L)).thenReturn(List.of(item));

        mockMvc.perform(get("/api/sales/1/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].drugName").value("Amoxicillin"));
    }

    @Test
    void searchByPatientName_returnsPage() throws Exception {
        Page<Sale> page = new PageImpl<>(List.of(createTestSale()));
        when(saleService.searchByPatientName(eq("Abebe"), any())).thenReturn(page);

        mockMvc.perform(get("/api/sales/search").param("patientName", "Abebe"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].patientName").value("Abebe"));
    }

    @Test
    void getInvoice_returnsInvoice() throws Exception {
        InvoiceResponse invoice = InvoiceResponse.builder()
                .saleId(1L).invoiceNumber("INV-TEST123").patientName("Abebe")
                .cashierName("Tigist").paymentMethod("CASH")
                .subtotal(new BigDecimal("250.00")).taxAmount(new BigDecimal("37.50"))
                .totalAmount(new BigDecimal("287.50")).build();
        when(saleService.getInvoice(1L)).thenReturn(invoice);

        mockMvc.perform(get("/api/sales/1/invoice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invoiceNumber").value("INV-TEST123"))
                .andExpect(jsonPath("$.totalAmount").value(287.50));
    }
}
