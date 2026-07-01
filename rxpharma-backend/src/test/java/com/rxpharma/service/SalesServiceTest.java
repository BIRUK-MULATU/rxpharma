package com.rxpharma.service;

import com.rxpharma.dto.response.InvoiceResponse;
import com.rxpharma.dto.response.SaleItemResponse;
import com.rxpharma.entity.*;
import com.rxpharma.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SaleService Tests")
class SaleServiceTest {

    @Mock private SaleRepository saleRepository;
    @Mock private SaleItemRepository saleItemRepository;
    @Mock private DrugRepository drugRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private SaleService saleService;

    private User testCashier;
    private Drug testDrug;
    private Drug testDrug2;
    private Sale testSale;
    private SaleItem testSaleItem;

    @BeforeEach
    void setUp() {
        testCashier = User.builder()
                .id(1L)
                .fullName("Tigist Cashier")
                .email("tigist@rxpharma.com")
                .role(User.Role.CASHIER)
                .build();

        testDrug = Drug.builder()
                .id(1L)
                .name("Amoxicillin 500mg")
                .sku("AMX-500")
                .price(new BigDecimal("25.00"))
                .stockQty(100)
                .build();

        testDrug2 = Drug.builder()
                .id(2L)
                .name("Paracetamol 500mg")
                .sku("PAR-500")
                .price(new BigDecimal("10.00"))
                .stockQty(200)
                .build();

        testSale = Sale.builder()
                .id(1L)
                .invoiceNumber("INV-ABC12345")
                .cashier(testCashier)
                .patientName("Abebe Girma")
                .paymentMethod(Sale.PaymentMethod.CASH)
                .totalAmount(new BigDecimal("287.50"))
                .taxAmount(new BigDecimal("37.50"))
                .saleDate(LocalDateTime.now())
                .build();

        testSaleItem = SaleItem.builder()
                .id(1L)
                .sale(testSale)
                .drug(testDrug)
                .quantity(10)
                .unitPrice(new BigDecimal("25.00"))
                .subtotal(new BigDecimal("250.00"))
                .build();
    }

    // ─── getSaleById ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("getSaleById - found returns sale")
    void getSaleById_found() {
        when(saleRepository.findById(1L)).thenReturn(Optional.of(testSale));

        Sale result = saleService.getSaleById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getPatientName()).isEqualTo("Abebe Girma");
        assertThat(result.getInvoiceNumber()).startsWith("INV-");
    }

    @Test
    @DisplayName("getSaleById - not found throws RuntimeException")
    void getSaleById_notFound() {
        when(saleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> saleService.getSaleById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Sale not found with id: 99");
    }

    // ─── createSale ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("createSale - success creates sale, deducts stock, calculates tax")
    void createSale_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testCashier));
        when(drugRepository.findById(1L)).thenReturn(Optional.of(testDrug));
        when(saleRepository.save(any(Sale.class))).thenAnswer(i -> i.getArgument(0));
        when(saleItemRepository.save(any(SaleItem.class))).thenAnswer(i -> i.getArgument(0));
        when(drugRepository.save(any(Drug.class))).thenAnswer(i -> i.getArgument(0));

        List<long[]> items = List.of(new long[]{1L, 10});
        Sale result = saleService.createSale(1L, "Abebe Girma",
                Sale.PaymentMethod.CASH, items);

        assertThat(result.getPatientName()).isEqualTo("Abebe Girma");
        assertThat(result.getPaymentMethod()).isEqualTo(Sale.PaymentMethod.CASH);
        assertThat(result.getInvoiceNumber()).startsWith("INV-");

        // tax = 250 * 0.15 = 37.50, total = 250 + 37.50 = 287.50
        assertThat(result.getTaxAmount()).isEqualByComparingTo("37.50");
        assertThat(result.getTotalAmount()).isEqualByComparingTo("287.50");

        // stock should be deducted
        assertThat(testDrug.getStockQty()).isEqualTo(90);
        verify(saleRepository, times(2)).save(any(Sale.class));
    }

    @Test
    @DisplayName("createSale - multiple items totals correctly")
    void createSale_multipleItems() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testCashier));
        when(drugRepository.findById(1L)).thenReturn(Optional.of(testDrug));
        when(drugRepository.findById(2L)).thenReturn(Optional.of(testDrug2));
        when(saleRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(saleItemRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(drugRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // 2 * 25 = 50  plus  3 * 10 = 30 => subtotal = 80, tax = 12, total = 92
        List<long[]> items = List.of(new long[]{1L, 2}, new long[]{2L, 3});
        Sale result = saleService.createSale(1L, "Patient", Sale.PaymentMethod.CARD, items);

        assertThat(result.getTaxAmount()).isEqualByComparingTo("12.00");
        assertThat(result.getTotalAmount()).isEqualByComparingTo("92.00");
    }

    @Test
    @DisplayName("createSale - cashier not found throws RuntimeException")
    void createSale_cashierNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> saleService.createSale(99L, "Patient",
                Sale.PaymentMethod.CASH, List.of()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Cashier not found");
    }

    @Test
    @DisplayName("createSale - drug not found throws RuntimeException")
    void createSale_drugNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testCashier));
        when(saleRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(drugRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> saleService.createSale(1L, "Patient",
                Sale.PaymentMethod.CASH, List.of(new long[]{99L, 5})))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Drug not found");
    }

    @Test
    @DisplayName("createSale - insufficient stock throws RuntimeException")
    void createSale_insufficientStock() {
        testDrug.setStockQty(5); // only 5 available
        when(userRepository.findById(1L)).thenReturn(Optional.of(testCashier));
        when(saleRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(drugRepository.findById(1L)).thenReturn(Optional.of(testDrug));

        assertThatThrownBy(() -> saleService.createSale(1L, "Patient",
                Sale.PaymentMethod.CASH, List.of(new long[]{1L, 10}))) // requesting 10
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Insufficient stock");
    }

    @Test
    @DisplayName("createSale - different payment methods are accepted")
    void createSale_paymentMethods() {
        for (Sale.PaymentMethod method : Sale.PaymentMethod.values()) {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testCashier));
            when(saleRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            Sale result = saleService.createSale(1L, "Patient", method, List.of());

            assertThat(result.getPaymentMethod()).isEqualTo(method);
        }
    }

    // ─── getSaleItems ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("getSaleItems - returns all items for sale")
    void getSaleItems_success() {
        when(saleRepository.findById(1L)).thenReturn(Optional.of(testSale));
        when(saleItemRepository.findBySaleId(1L)).thenReturn(List.of(testSaleItem));

        List<SaleItemResponse> result = saleService.getSaleItems(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDrugName()).isEqualTo("Amoxicillin 500mg");
        assertThat(result.get(0).getQuantity()).isEqualTo(10);
        assertThat(result.get(0).getUnitPrice()).isEqualByComparingTo("25.00");
        assertThat(result.get(0).getSubtotal()).isEqualByComparingTo("250.00");
    }

    @Test
    @DisplayName("getSaleItems - empty list when no items")
    void getSaleItems_empty() {
        when(saleRepository.findById(1L)).thenReturn(Optional.of(testSale));
        when(saleItemRepository.findBySaleId(1L)).thenReturn(List.of());

        List<SaleItemResponse> result = saleService.getSaleItems(1L);

        assertThat(result).isEmpty();
    }

    // ─── getInvoice ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("getInvoice - returns full invoice with all fields")
    void getInvoice_success() {
        when(saleRepository.findById(1L)).thenReturn(Optional.of(testSale));
        when(saleItemRepository.findBySaleId(1L)).thenReturn(List.of(testSaleItem));

        InvoiceResponse result = saleService.getInvoice(1L);

        assertThat(result.getSaleId()).isEqualTo(1L);
        assertThat(result.getInvoiceNumber()).isEqualTo("INV-ABC12345");
        assertThat(result.getPatientName()).isEqualTo("Abebe Girma");
        assertThat(result.getCashierName()).isEqualTo("Tigist Cashier");
        assertThat(result.getPaymentMethod()).isEqualTo("CASH");
        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getSubtotal()).isEqualByComparingTo("250.00");
        assertThat(result.getTaxAmount()).isEqualByComparingTo("37.50");
        assertThat(result.getTotalAmount()).isEqualByComparingTo("287.50");
    }

    @Test
    @DisplayName("getInvoice - cashier null shows null name")
    void getInvoice_nullCashier() {
        testSale.setCashier(null);
        when(saleRepository.findById(1L)).thenReturn(Optional.of(testSale));
        when(saleItemRepository.findBySaleId(1L)).thenReturn(List.of());

        InvoiceResponse result = saleService.getInvoice(1L);

        assertThat(result.getCashierName()).isNull();
    }

    @Test
    @DisplayName("getInvoice - sale not found throws RuntimeException")
    void getInvoice_saleNotFound() {
        when(saleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> saleService.getInvoice(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Sale not found");
    }

    @Test
    @DisplayName("getInvoice - subtotal calculated from items not from sale total")
    void getInvoice_subtotalCalculation() {
        SaleItem item2 = SaleItem.builder()
                .id(2L).sale(testSale).drug(testDrug2)
                .quantity(3).unitPrice(new BigDecimal("10.00"))
                .subtotal(new BigDecimal("30.00")).build();

        when(saleRepository.findById(1L)).thenReturn(Optional.of(testSale));
        when(saleItemRepository.findBySaleId(1L)).thenReturn(List.of(testSaleItem, item2));

        InvoiceResponse result = saleService.getInvoice(1L);

        // 250 + 30 = 280
        assertThat(result.getSubtotal()).isEqualByComparingTo("280.00");
    }
}