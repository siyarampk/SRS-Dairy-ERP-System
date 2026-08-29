package dairy.erp.service;

import dairy.erp.dao.CustomerDAO;
import dairy.erp.model.Customer;
import dairy.erp.util.LogUtil;

import java.sql.SQLException;
import java.util.List;
import java.util.logging.Logger;

/**
 * Business logic for customer master data, including safe deletion and
 * automatic customer code generation.
 */
public class CustomerService {

    private static final Logger LOG = LogUtil.getLogger(CustomerService.class);
    private static final String CODE_PREFIX = "CUST";

    private final CustomerDAO customerDAO = new CustomerDAO();

    public List<Customer> listAll() {
        try {
            return customerDAO.findAll();
        } catch (SQLException e) {
            LOG.severe("Could not list customers: " + e.getMessage());
            throw new RuntimeException("Could not load customers.", e);
        }
    }

    public List<Customer> search(String code, String name, String mobile, String status) {
        try {
            return customerDAO.search(code, name, mobile, status);
        } catch (SQLException e) {
            LOG.severe("Customer search failed: " + e.getMessage());
            throw new RuntimeException("Could not search customers.", e);
        }
    }

    public Customer findByCode(String code) {
        try {
            return customerDAO.findByCode(code);
        } catch (SQLException e) {
            LOG.severe("Customer lookup failed: " + e.getMessage());
            throw new RuntimeException("Could not find customer.", e);
        }
    }

    public Customer findById(int id) {
        try {
            return customerDAO.findById(id);
        } catch (SQLException e) {
            LOG.severe("Customer lookup failed: " + e.getMessage());
            throw new RuntimeException("Could not find customer.", e);
        }
    }

    public int add(Customer c) {
        try {
            return customerDAO.addCustomer(c);
        } catch (SQLException e) {
            LOG.severe("Add customer failed: " + e.getMessage());
            throw new RuntimeException("Could not add customer: " + e.getMessage(), e);
        }
    }

    public void update(Customer c) {
        try {
            customerDAO.updateCustomer(c);
        } catch (SQLException e) {
            LOG.severe("Update customer failed: " + e.getMessage());
            throw new RuntimeException("Could not update customer: " + e.getMessage(), e);
        }
    }

    /**
     * Deletes a customer safely if no transaction history exists, otherwise
     * falls back to marking the customer inactive so history stays intact.
     */
    public String deleteOrDeactivate(int id) {
        try {
            boolean deleted = customerDAO.deleteCustomer(id);
            if (deleted) {
                return "DELETED";
            }
            customerDAO.markInactive(id, true);
            return "DEACTIVATED";
        } catch (SQLException e) {
            LOG.severe("Delete customer failed: " + e.getMessage());
            throw new RuntimeException("Could not delete customer: " + e.getMessage(), e);
        }
    }

    public boolean isCodeTaken(String code, int excludeId) {
        Customer existing = findByCode(code);
        return existing != null && existing.getId() != excludeId;
    }

    /**
     * Generates the next customer code (CUST001, CUST002, ...) based on the
     * highest existing numeric suffix, so codes are never reused.
     */
    public String nextCode() {
        try {
            String last = customerDAO.lastCustomerCode();
            int next = 1;
            if (last != null && last.startsWith(CODE_PREFIX)) {
                try {
                    next = Integer.parseInt(last.substring(CODE_PREFIX.length())) + 1;
                } catch (NumberFormatException ignored) {
                    next = countAll() + 1;
                }
            }
            return String.format("%s%03d", CODE_PREFIX, next);
        } catch (SQLException e) {
            LOG.severe("Next code generation failed: " + e.getMessage());
            throw new RuntimeException("Could not generate customer code.", e);
        }
    }

    private int countAll() {
        return listAll().size();
    }
}
