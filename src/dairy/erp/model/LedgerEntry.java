package dairy.erp.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A single line of a customer ledger / statement.
 */
public class LedgerEntry {

    private LocalDate date;
    private String transactionType; // MILK, PAYMENT, ADJUSTMENT
    private String description;
    private BigDecimal quantity = BigDecimal.ZERO;
    private BigDecimal amount = BigDecimal.ZERO;   // positive for milk, negative/zero for payment
    private BigDecimal payment = BigDecimal.ZERO;
    private BigDecimal balance = BigDecimal.ZERO;

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public String getTransactionType() { return transactionType; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getPayment() { return payment; }
    public void setPayment(BigDecimal payment) { this.payment = payment; }
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
}
