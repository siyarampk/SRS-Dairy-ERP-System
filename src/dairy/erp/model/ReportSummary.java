package dairy.erp.model;

import java.math.BigDecimal;

/**
 * Aggregated totals for report summaries.
 */
public class ReportSummary {

    private BigDecimal totalQuantity = BigDecimal.ZERO;
    private BigDecimal totalAmount = BigDecimal.ZERO;
    private BigDecimal totalPayments = BigDecimal.ZERO;
    private long totalRecords;
    private long totalCustomers;
    private BigDecimal avgFat = BigDecimal.ZERO;
    private BigDecimal avgSnf = BigDecimal.ZERO;
    private BigDecimal avgRate = BigDecimal.ZERO;
    private BigDecimal openingBalance = BigDecimal.ZERO;
    private BigDecimal closingBalance = BigDecimal.ZERO;

    public BigDecimal getTotalQuantity() { return totalQuantity; }
    public void setTotalQuantity(BigDecimal totalQuantity) { this.totalQuantity = totalQuantity; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public BigDecimal getTotalPayments() { return totalPayments; }
    public void setTotalPayments(BigDecimal totalPayments) { this.totalPayments = totalPayments; }
    public long getTotalRecords() { return totalRecords; }
    public void setTotalRecords(long totalRecords) { this.totalRecords = totalRecords; }
    public long getTotalCustomers() { return totalCustomers; }
    public void setTotalCustomers(long totalCustomers) { this.totalCustomers = totalCustomers; }
    public BigDecimal getAvgFat() { return avgFat; }
    public void setAvgFat(BigDecimal avgFat) { this.avgFat = avgFat; }
    public BigDecimal getAvgSnf() { return avgSnf; }
    public void setAvgSnf(BigDecimal avgSnf) { this.avgSnf = avgSnf; }
    public BigDecimal getAvgRate() { return avgRate; }
    public void setAvgRate(BigDecimal avgRate) { this.avgRate = avgRate; }
    public BigDecimal getOpeningBalance() { return openingBalance; }
    public void setOpeningBalance(BigDecimal openingBalance) { this.openingBalance = openingBalance; }
    public BigDecimal getClosingBalance() { return closingBalance; }
    public void setClosingBalance(BigDecimal closingBalance) { this.closingBalance = closingBalance; }
}
