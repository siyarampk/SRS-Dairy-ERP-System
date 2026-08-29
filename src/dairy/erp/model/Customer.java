package dairy.erp.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * POJO for a customer / member.
 */
public class Customer {

    private int id;
    private String customerCode;
    private String customerName;
    private String fatherName;
    private String mobile;
    private String address;
    private String village;
    private String milkType;
    private String customerType;
    private String status;
    private BigDecimal openingBalance = BigDecimal.ZERO;
    private LocalDate registrationDate;
    private String remarks;
    private String createdAt;
    private String updatedAt;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getCustomerCode() { return customerCode; }
    public void setCustomerCode(String customerCode) { this.customerCode = customerCode; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getFatherName() { return fatherName; }
    public void setFatherName(String fatherName) { this.fatherName = fatherName; }
    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getVillage() { return village; }
    public void setVillage(String village) { this.village = village; }
    public String getMilkType() { return milkType; }
    public void setMilkType(String milkType) { this.milkType = milkType; }
    public String getCustomerType() { return customerType; }
    public void setCustomerType(String customerType) { this.customerType = customerType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public BigDecimal getOpeningBalance() { return openingBalance; }
    public void setOpeningBalance(BigDecimal openingBalance) { this.openingBalance = openingBalance; }
    public LocalDate getRegistrationDate() { return registrationDate; }
    public void setRegistrationDate(LocalDate registrationDate) { this.registrationDate = registrationDate; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
