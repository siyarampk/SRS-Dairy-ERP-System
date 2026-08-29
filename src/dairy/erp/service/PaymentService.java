package dairy.erp.service;

import dairy.erp.dao.CustomerDAO;
import dairy.erp.dao.PaymentDAO;
import dairy.erp.model.Customer;
import dairy.erp.model.Payment;
import dairy.erp.util.LogUtil;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.logging.Logger;

/**
 * Business logic for payments and the simple customer ledger (milk credit vs
 * payments producing a running balance).
 */
public class PaymentService {

    private static final Logger LOG = LogUtil.getLogger(PaymentService.class);
    private final PaymentDAO paymentDAO = new PaymentDAO();
    private final CustomerDAO customerDAO = new CustomerDAO();

    public int add(Payment p) {
        try {
            return paymentDAO.add(p);
        } catch (SQLException e) {
            LOG.severe("Add payment failed: " + e.getMessage());
            throw new RuntimeException("Could not save payment: " + e.getMessage(), e);
        }
    }

    public void update(Payment p) {
        try {
            paymentDAO.update(p);
        } catch (SQLException e) {
            LOG.severe("Update payment failed: " + e.getMessage());
            throw new RuntimeException("Could not update payment: " + e.getMessage(), e);
        }
    }

    public void delete(int id) {
        try {
            paymentDAO.delete(id);
        } catch (SQLException e) {
            LOG.severe("Delete payment failed: " + e.getMessage());
            throw new RuntimeException("Could not delete payment.", e);
        }
    }

    public List<Payment> search(int customerId, LocalDate from, LocalDate to) {
        try {
            return paymentDAO.findByCustomerDateRange(customerId, from, to);
        } catch (SQLException e) {
            LOG.severe("Payment search failed: " + e.getMessage());
            throw new RuntimeException("Could not load payments.", e);
        }
    }

    public Customer findCustomerByCode(String code) {
        try {
            return customerDAO.findByCode(code);
        } catch (SQLException e) {
            LOG.severe("Customer lookup failed: " + e.getMessage());
            throw new RuntimeException("Could not find customer.", e);
        }
    }
}
