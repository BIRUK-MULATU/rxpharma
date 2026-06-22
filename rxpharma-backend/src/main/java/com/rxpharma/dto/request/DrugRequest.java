package com.rxpharma.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class DrugRequest {

    @NotBlank(message = "Name is required")
    private String name;

    private String sku;

    @NotBlank(message = "Category is required")
    private String category;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    private BigDecimal price;

    @Min(value = 0, message = "Stock quantity cannot be negative")
    private int stockQty;

    @NotNull(message = "Expiry date is required")
    private LocalDate expiryDate;

    private Long supplierId;
}