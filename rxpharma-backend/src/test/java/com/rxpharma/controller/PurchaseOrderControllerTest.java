package com.rxpharma.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rxpharma.dto.request.DeliverOrderRequest;
import com.rxpharma.dto.request.PurchaseOrderItemRequest;
import com.rxpharma.dto.request.PurchaseOrderRequest;
import com.rxpharma.entity.PurchaseOrder;
import com.rxpharma.entity.Supplier;
import com.rxpharma.security.CustomUserDetailsService;
import com.rxpharma.security.JwtUtil;
import com.rxpharma.service.PurchaseOrderService;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = PurchaseOrderController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
class PurchaseOrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PurchaseOrderService purchaseOrderService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    private PurchaseOrder createTestOrder() {
        Supplier supplier = Supplier.builder().id(1L).companyName("MediSupply").build();
        return PurchaseOrder.builder().id(1L).supplier(supplier)
                .status(PurchaseOrder.Status.DRAFT)
                .totalCost(new BigDecimal("5000.00"))
                .orderDate(LocalDateTime.now())
                .items(new ArrayList<>()).build();
    }

    @Test
    void getAllOrders_returnsPage() throws Exception {
        Page<PurchaseOrder> page = new PageImpl<>(List.of(createTestOrder()));
        when(purchaseOrderService.getAllOrders(any())).thenReturn(page);

        mockMvc.perform(get("/api/purchase-orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("DRAFT"));
    }

    @Test
    void getOrderById_found() throws Exception {
        when(purchaseOrderService.getOrderById(1L)).thenReturn(createTestOrder());

        mockMvc.perform(get("/api/purchase-orders/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    void createOrder_success() throws Exception {
        PurchaseOrderItemRequest item = new PurchaseOrderItemRequest();
        item.setDrugId(1L);
        item.setQuantity(50);
        item.setUnitCost(new BigDecimal("100.00"));

        PurchaseOrderRequest request = new PurchaseOrderRequest();
        request.setSupplierId(1L);
        request.setOrderedById(1L);
        request.setDeliveryDate(LocalDate.now().plusDays(7));
        request.setItems(List.of(item));

        when(purchaseOrderService.createOrder(anyLong(), anyLong(), any(), any(), anyList()))
                .thenReturn(createTestOrder());

        mockMvc.perform(post("/api/purchase-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    void updateStatus_success() throws Exception {
        PurchaseOrder sent = createTestOrder();
        sent.setStatus(PurchaseOrder.Status.SENT);
        when(purchaseOrderService.updateOrderStatus(1L, PurchaseOrder.Status.SENT)).thenReturn(sent);

        mockMvc.perform(patch("/api/purchase-orders/1/status")
                        .param("status", "SENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SENT"));
    }

    @Test
    void deliverOrder_success() throws Exception {
        DeliverOrderRequest request = new DeliverOrderRequest();
        request.setDeliveryDate(LocalDate.now());

        PurchaseOrder delivered = createTestOrder();
        delivered.setStatus(PurchaseOrder.Status.DELIVERED);
        delivered.setDeliveryDate(LocalDate.now());
        when(purchaseOrderService.deliverOrder(1L, LocalDate.now())).thenReturn(delivered);

        mockMvc.perform(patch("/api/purchase-orders/1/deliver")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DELIVERED"));
    }

    @Test
    void deleteOrder_noContent() throws Exception {
        mockMvc.perform(delete("/api/purchase-orders/1"))
                .andExpect(status().isNoContent());
    }
}
