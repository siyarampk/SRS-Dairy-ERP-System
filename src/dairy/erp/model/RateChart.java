package dairy.erp.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * POJO for a rate chart rule (a FAT/SNF band with a rate).
 */
public class RateChart {

    private int id;
    private String milkType;
    private BigDecimal fatMin = BigDecimal.ZERO;
    private BigDecimal fatMax = BigDecimal.ZERO;
    private BigDecimal snfMin = BigDecimal.ZERO;
    private BigDecimal snfMax = BigDecimal.ZERO;
    private BigDecimal ratePerLitre = BigDecimal.ZERO;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private boolean active;
    private String createdAt;
    private String updatedAt;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getMilkType() { return milkType; }
    public void setMilkType(String milkType) { this.milkType = milkType; }
    public BigDecimal getFatMin() { return fatMin; }
    public void setFatMin(BigDecimal fatMin) { this.fatMin = fatMin; }
    public BigDecimal getFatMax() { return fatMax; }
    public void setFatMax(BigDecimal fatMax) { this.fatMax = fatMax; }
    public BigDecimal getSnfMin() { return snfMin; }
    public void setSnfMin(BigDecimal snfMin) { this.snfMin = snfMin; }
    public BigDecimal getSnfMax() { return snfMax; }
    public void setSnfMax(BigDecimal snfMax) { this.snfMax = snfMax; }
    public BigDecimal getRatePerLitre() { return ratePerLitre; }
    public void setRatePerLitre(BigDecimal ratePerLitre) { this.ratePerLitre = ratePerLitre; }
    public LocalDate getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(LocalDate effectiveFrom) { this.effectiveFrom = effectiveFrom; }
    public LocalDate getEffectiveTo() { return effectiveTo; }
    public void setEffectiveTo(LocalDate effectiveTo) { this.effectiveTo = effectiveTo; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
