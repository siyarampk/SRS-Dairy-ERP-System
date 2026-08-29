package dairy.erp.model;

import java.math.BigDecimal;

/**
 * A per-customer row in weekly/monthly grouped reports.
 */
public class CustomerSummaryRow {

    private String customerCode;
    private String customerName;
    private String milkType;
    private BigDecimal totalQuantity = BigDecimal.ZERO;
    private BigDecimal totalAmount = BigDecimal.ZERO;
    private BigDecimal avgFat = BigDecimal.ZERO;
    private BigDecimal avgSnf = BigDecimal.ZERO;
    private BigDecimal avgRate = BigDecimal.ZERO;

    public String getCustomerCode() { return customerCode; }
    public void setCustomerCode(String customerCode) { this.customerCode = customerCode; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getMilkType() { return milkType; }
    public void setMilkType(String milkType) { this.milkType = milkType; }
    public BigDecimal getTotalQuantity() { return totalQuantity; }
    public void setTotalQuantity(BigDecimal totalQuantity) { this.totalQuantity = totalQuantity; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public BigDecimal getAvgFat() { return avgFat; }
    public void setAvgFat(BigDecimal avgFat) { this.avgFat = avgFat; }
    public BigDecimal getAvgSnf() { return avgSnf; }
    public void setAvgSnf(BigDecimal avgSnf) { this.avgSnf = avgSnf; }
    public BigDecimal getAvgRate() { return avgRate; }
    public void setAvgRate(BigDecimal avgRate) { this.avgRate = avgRate; }
}
