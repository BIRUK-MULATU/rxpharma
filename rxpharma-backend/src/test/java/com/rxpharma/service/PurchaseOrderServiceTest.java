package com.rxpharma.service;

import com.rxpharma.dto.request.PurchaseOrderItemRequest;
import com.rxpharma.entity.*;
import com.rxpharma.exception.BadRequestException;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PurchaseOrderService Tests")
class PurchaseOrderServiceTest {

    @Mock private PurchaseOrderRepository purchaseOrderRepository;
    @Mock private SupplierRepository supplierRepository;
    @Mock private UserRepository userRepository;
    @Mock private DrugRepository drugRepository;

    @InjectMocks private PurchaseOrderService purchaseOrderService;

    private Supplier testSupplier;
    private User testUser;
    private Drug testDrug;
    private PurchaseOrder draftOrder;
    private PurchaseOrder deliveredOrder;
    private PurchaseOrder cancelledOrder;
    private PurchaseOrderItemRequest itemRequest;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        testSupplier = Supplier.builder()
                .id(1L)
                .companyName("MediSupply Ethiopia")
                .build();

        testUser = User.builder()
                .id(1L)
                .fullName("Abebe Manager")
                .role(User.Role.SUPPLIER_MANAGER)
                .build();

        testDrug = Drug.builder()
                .id(1L)
                .name("Amoxicillin 500mg")
                .stockQty(100)
                .build();

        draftOrder = PurchaseOrder.builder()
                .id(1L)
                .supplier(testSupplier)
                .orderedBy(testUser)
                .status(PurchaseOrder.Status.DRAFT)
                .totalCost(new BigDecimal("5000.00"))
                .items(new ArrayList<>())
                .build();

        deliveredOrder = PurchaseOrder.builder()
                .id(2L)
                .supplier(testSupplier)
                .status(PurchaseOrder.Status.DELIVERED)
                .totalCost(new BigDecimal("3000.00"))
                .items(new ArrayList<>())
                .build();

        cancelledOrder = PurchaseOrder.builder()
                .id(3L)
                .supplier(testSupplier)
                .status(PurchaseOrder.Status.CANCELLED)
                .totalCost(new BigDecimal("1000.00"))
                .items(new ArrayList<>())
                .build();

        itemRequest = new PurchaseOrderItemRequest();
        itemRequest.setDrugId(1L);
        itemRequest.setQuantity(50);
        itemRequest.setUnitCost(new BigDecimal("100.00"));

        pageable = PageRequest.of(0, 10);
    }

    // ─── getAllOrders ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("getAllOrders - returns paged results")
    void getAllOrders_returnsPage() {
        Page<PurchaseOrder> page = new PageImpl<>(List.of(draftOrder, deliveredOrder));
        when(purchaseOrderRepository.findAll(pageable)).thenReturn(page);

        Page<PurchaseOrder> result = purchaseOrderService.getAllOrders(pageable);

        assertThat(result.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("getAllOrders - empty page when no orders")
    void getAllOrders_empty() {
        when(purchaseOrderRepository.findAll(pageable)).thenReturn(Page.empty());

        Page<PurchaseOrder> result = purchaseOrderService.getAllOrders(pageable);

        assertThat(result.getContent()).isEmpty();
    }

    // ─── getOrderById ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("getOrderById - found returns order")
    void getOrderById_found() {
        when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(draftOrder));

        PurchaseOrder result = purchaseOrderService.getOrderById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getStatus()).isEqualTo(PurchaseOrder.Status.DRAFT);
    }

    @Test
    @DisplayName("getOrderById - not found throws RuntimeException")
    void getOrderById_notFound() {
        when(purchaseOrderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> purchaseOrderService.getOrderById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Purchase order not found with id: 99");
    }

    // ─── createOrder ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("createOrder - success with items creates DRAFT order")
    void createOrder_success_withItems() {
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(testSupplier));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(drugRepository.findById(1L)).thenReturn(Optional.of(testDrug));
        when(purchaseOrderRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        PurchaseOrder result = purchaseOrderService.createOrder(
                1L, 1L, null, LocalDate.now().plusDays(7), List.of(itemRequest));

        assertThat(result.getStatus()).isEqualTo(PurchaseOrder.Status.DRAFT);
        assertThat(result.getSupplier()).isEqualTo(testSupplier);
        assertThat(result.getOrderedBy()).isEqualTo(testUser);
        // calculated total = 50 * 100 = 5000
        assertThat(result.getTotalCost()).isEqualByComparingTo("5000.00");
    }

    @Test
    @DisplayName("createOrder - explicit totalCost overrides calculated total")
    void createOrder_explicitTotalCostOverrides() {
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(testSupplier));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(drugRepository.findById(1L)).thenReturn(Optional.of(testDrug));
        when(purchaseOrderRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        BigDecimal explicitTotal = new BigDecimal("9999.00");
        PurchaseOrder result = purchaseOrderService.createOrder(
                1L, 1L, explicitTotal, null, List.of(itemRequest));

        assertThat(result.getTotalCost()).isEqualByComparingTo("9999.00");
    }

    @Test
    @DisplayName("createOrder - null orderedById sets orderedBy to null")
    void createOrder_nullOrderedBy() {
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(testSupplier));
        when(purchaseOrderRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(drugRepository.findById(1L)).thenReturn(Optional.of(testDrug));

        PurchaseOrder result = purchaseOrderService.createOrder(
                1L, null, new BigDecimal("1000.00"), null, List.of(itemRequest));

        assertThat(result.getOrderedBy()).isNull();
        verify(userRepository, never()).findById(any());
    }

    @Test
    @DisplayName("createOrder - supplier not found throws RuntimeException")
    void createOrder_supplierNotFound() {
        when(supplierRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> purchaseOrderService.createOrder(
                99L, 1L, null, null, List.of(itemRequest)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Supplier not found");
    }

    @Test
    @DisplayName("createOrder - drug not found throws RuntimeException")
    void createOrder_drugNotFound() {
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(testSupplier));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(purchaseOrderRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(drugRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> purchaseOrderService.createOrder(
                1L, 1L, null, null, List.of(itemRequest)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Drug not found");
    }

    @Test
    @DisplayName("createOrder - multiple items calculates combined total")
    void createOrder_multipleItems() {
        PurchaseOrderItemRequest item2 = new PurchaseOrderItemRequest();
        item2.setDrugId(1L);
        item2.setQuantity(20);
        item2.setUnitCost(new BigDecimal("50.00"));

        when(supplierRepository.findById(1L)).thenReturn(Optional.of(testSupplier));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(drugRepository.findById(1L)).thenReturn(Optional.of(testDrug));
        when(purchaseOrderRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // 50*100 + 20*50 = 5000+1000 = 6000
        PurchaseOrder result = purchaseOrderService.createOrder(
                1L, 1L, null, null, List.of(itemRequest, item2));

        assertThat(result.getTotalCost()).isEqualByComparingTo("6000.00");
    }

    // ─── updateOrderStatus ────────────────────────────────────────────────────

    @Test
    @DisplayName("updateOrderStatus - changes status and saves")
    void updateOrderStatus_success() {
        when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(draftOrder));
        when(purchaseOrderRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        PurchaseOrder result = purchaseOrderService.updateOrderStatus(1L, PurchaseOrder.Status.SENT);

        assertThat(result.getStatus()).isEqualTo(PurchaseOrder.Status.SENT);
        verify(purchaseOrderRepository).save(draftOrder);
    }

    @Test
    @DisplayName("updateOrderStatus - order not found throws RuntimeException")
    void updateOrderStatus_notFound() {
        when(purchaseOrderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> purchaseOrderService.updateOrderStatus(99L, PurchaseOrder.Status.SENT))
                .isInstanceOf(RuntimeException.class);
    }

    // ─── deliverOrder ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("deliverOrder - success sets DELIVERED and increases stock")
    void deliverOrder_success() {
        PurchaseOrderItem orderItem = PurchaseOrderItem.builder()
                .drug(testDrug).quantity(50).build();
        draftOrder.getItems().add(orderItem);

        when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(draftOrder));
        when(purchaseOrderRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        PurchaseOrder result = purchaseOrderService.deliverOrder(1L, LocalDate.now());

        assertThat(result.getStatus()).isEqualTo(PurchaseOrder.Status.DELIVERED);
        assertThat(result.getDeliveryDate()).isEqualTo(LocalDate.now());
        assertThat(testDrug.getStockQty()).isEqualTo(150); // 100 + 50
    }

    @Test
    @DisplayName("deliverOrder - already delivered throws BadRequestException")
    void deliverOrder_alreadyDelivered() {
        when(purchaseOrderRepository.findById(2L)).thenReturn(Optional.of(deliveredOrder));

        assertThatThrownBy(() -> purchaseOrderService.deliverOrder(2L, LocalDate.now()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already delivered");
    }

    @Test
    @DisplayName("deliverOrder - cancelled order throws BadRequestException")
    void deliverOrder_cancelledOrder() {
        when(purchaseOrderRepository.findById(3L)).thenReturn(Optional.of(cancelledOrder));

        assertThatThrownBy(() -> purchaseOrderService.deliverOrder(3L, LocalDate.now()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot deliver a cancelled order");
    }

    @Test
    @DisplayName("deliverOrder - order with no items still delivers successfully")
    void deliverOrder_noItems() {
        when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(draftOrder));
        when(purchaseOrderRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        PurchaseOrder result = purchaseOrderService.deliverOrder(1L, LocalDate.now());

        assertThat(result.getStatus()).isEqualTo(PurchaseOrder.Status.DELIVERED);
        assertThat(testDrug.getStockQty()).isEqualTo(100); // unchanged
    }

    // ─── deleteOrder ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteOrder - success deletes order")
    void deleteOrder_success() {
        when(purchaseOrderRepository.existsById(1L)).thenReturn(true);

        purchaseOrderService.deleteOrder(1L);

        verify(purchaseOrderRepository).deleteById(1L);
    }

    @Test
    @DisplayName("deleteOrder - not found throws RuntimeException")
    void deleteOrder_notFound() {
        when(purchaseOrderRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> purchaseOrderService.deleteOrder(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Purchase order not found");

        verify(purchaseOrderRepository, never()).deleteById(any());
    }
}