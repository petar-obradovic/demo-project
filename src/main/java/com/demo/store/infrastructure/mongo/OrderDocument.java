package com.demo.store.infrastructure.mongo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document("orders")
public class OrderDocument {

    @Id
    private String id;

    @Indexed
    private String customerId;

    private List<Line> lines = new ArrayList<>();

    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal totalAmount;

    private String totalCurrency;
    private String status;
    private Instant placedAt;

    public static class Line {
        private String productId;
        private String name;

        @Field(targetType = FieldType.DECIMAL128)
        private BigDecimal unitPriceAmount;

        private String unitPriceCurrency;
        private int quantity;

        @Field(targetType = FieldType.DECIMAL128)
        private BigDecimal lineTotalAmount;

        private String lineTotalCurrency;

        public String getProductId() { return productId; }
        public void setProductId(String productId) { this.productId = productId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public BigDecimal getUnitPriceAmount() { return unitPriceAmount; }
        public void setUnitPriceAmount(BigDecimal unitPriceAmount) { this.unitPriceAmount = unitPriceAmount; }
        public String getUnitPriceCurrency() { return unitPriceCurrency; }
        public void setUnitPriceCurrency(String unitPriceCurrency) { this.unitPriceCurrency = unitPriceCurrency; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
        public BigDecimal getLineTotalAmount() { return lineTotalAmount; }
        public void setLineTotalAmount(BigDecimal lineTotalAmount) { this.lineTotalAmount = lineTotalAmount; }
        public String getLineTotalCurrency() { return lineTotalCurrency; }
        public void setLineTotalCurrency(String lineTotalCurrency) { this.lineTotalCurrency = lineTotalCurrency; }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public List<Line> getLines() { return lines; }
    public void setLines(List<Line> lines) { this.lines = lines; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public String getTotalCurrency() { return totalCurrency; }
    public void setTotalCurrency(String totalCurrency) { this.totalCurrency = totalCurrency; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getPlacedAt() { return placedAt; }
    public void setPlacedAt(Instant placedAt) { this.placedAt = placedAt; }
}
