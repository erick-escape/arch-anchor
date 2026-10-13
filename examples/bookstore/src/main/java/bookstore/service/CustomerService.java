package bookstore.service;

import bookstore.model.Customer;
import bookstore.persistence.CustomerRepository;

import java.util.Optional;

public class CustomerService {

    private final CustomerRepository customers;

    public CustomerService(CustomerRepository customers) {
        this.customers = customers;
    }

    public Optional<Customer> findCustomer(String email) {
        return customers.findByEmail(email);
    }

    public boolean register(Customer customer) {
        if (!customer.email().contains("@")) {
            return false;
        }
        if (customers.findByEmail(customer.email()).isPresent()) {
            return false;
        }
        return customers.save(customer);
    }

}
