package com.rxpharma.controller;

import com.rxpharma.dto.response.DashboardResponse;
import com.rxpharma.security.CustomUserDetailsService;
import com.rxpharma.security.JwtUtil;
import com.rxpharma.service.DashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = DashboardController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DashboardService dashboardService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void getStats_success() throws Exception {
        DashboardResponse stats = DashboardResponse.builder()
                .totalUsers(10).totalDrugs(50).totalSuppliers(5)
                .totalSales(200).totalPrescriptions(100).totalPurchaseOrders(30)
                .lowStockCount(2).expiringSoonCount(3)
                .pendingPrescriptions(5).pendingOrders(8)
                .build();

        when(dashboardService.getStats()).thenReturn(stats);

        mockMvc.perform(get("/api/dashboard/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(10))
                .andExpect(jsonPath("$.totalDrugs").value(50))
                .andExpect(jsonPath("$.totalSuppliers").value(5))
                .andExpect(jsonPath("$.totalSales").value(200))
                .andExpect(jsonPath("$.lowStockCount").value(2))
                .andExpect(jsonPath("$.pendingOrders").value(8));
    }
}
