package dairy.erp.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Aggregated summary for a single day shown on the dashboard.
 */
public class DashboardSummary {

    private LocalDate date;
    private long totalCustomers;
    private BigDecimal totalQuantity = BigDecimal.ZERO;
    private BigDecimal totalAmount = BigDecimal.ZERO;
    private BigDecimal morningQty = BigDecimal.ZERO;
    private BigDecimal eveningQty = BigDecimal.ZERO;
    private BigDecimal cowQty = BigDecimal.ZERO;
    private BigDecimal buffaloQty = BigDecimal.ZERO;
    private BigDecimal mixQty = BigDecimal.ZERO;
    private BigDecimal avgFat = BigDecimal.ZERO;
    private BigDecimal avgSnf = BigDecimal.ZERO;

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public long getTotalCustomers() { return totalCustomers; }
    public void setTotalCustomers(long totalCustomers) { this.totalCustomers = totalCustomers; }
    public BigDecimal getTotalQuantity() { return totalQuantity; }
    public void setTotalQuantity(BigDecimal totalQuantity) { this.totalQuantity = totalQuantity; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public BigDecimal getMorningQty() { return morningQty; }
    public void setMorningQty(BigDecimal morningQty) { this.morningQty = morningQty; }
    public BigDecimal getEveningQty() { return eveningQty; }
    public void setEveningQty(BigDecimal eveningQty) { this.eveningQty = eveningQty; }
    public BigDecimal getCowQty() { return cowQty; }
    public void setCowQty(BigDecimal cowQty) { this.cowQty = cowQty; }
    public BigDecimal getBuffaloQty() { return buffaloQty; }
    public void setBuffaloQty(BigDecimal buffaloQty) { this.buffaloQty = buffaloQty; }
    public BigDecimal getMixQty() { return mixQty; }
    public void setMixQty(BigDecimal mixQty) { this.mixQty = mixQty; }
    public BigDecimal getAvgFat() { return avgFat; }
    public void setAvgFat(BigDecimal avgFat) { this.avgFat = avgFat; }
    public BigDecimal getAvgSnf() { return avgSnf; }
    public void setAvgSnf(BigDecimal avgSnf) { this.avgSnf = avgSnf; }
}
