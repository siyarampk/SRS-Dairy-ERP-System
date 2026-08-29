package dairy.erp.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * POJO for a milk collection record.
 */
public class MilkCollection {

    private int id;
    private LocalDate collectionDate;
    private int customerId;
    private String customerCode;
    private String customerName;
    private String milkType;
    private String shift;
    private BigDecimal quantity = BigDecimal.ZERO;
    private BigDecimal fat = BigDecimal.ZERO;
    private BigDecimal snf = BigDecimal.ZERO;
    private BigDecimal ratePerLitre = BigDecimal.ZERO;
    private BigDecimal amount = BigDecimal.ZERO;
    private String remarks;
    private String createdAt;
    private String updatedAt;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public LocalDate getCollectionDate() { return collectionDate; }
    public void setCollectionDate(LocalDate collectionDate) { this.collectionDate = collectionDate; }
    public int getCustomerId() { return customerId; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }
    public String getCustomerCode() { return customerCode; }
    public void setCustomerCode(String customerCode) { this.customerCode = customerCode; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getMilkType() { return milkType; }
    public void setMilkType(String milkType) { this.milkType = milkType; }
    public String getShift() { return shift; }
    public void setShift(String shift) { this.shift = shift; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public BigDecimal getFat() { return fat; }
    public void setFat(BigDecimal fat) { this.fat = fat; }
    public BigDecimal getSnf() { return snf; }
    public void setSnf(BigDecimal snf) { this.snf = snf; }
    public BigDecimal getRatePerLitre() { return ratePerLitre; }
    public void setRatePerLitre(BigDecimal ratePerLitre) { this.ratePerLitre = ratePerLitre; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
