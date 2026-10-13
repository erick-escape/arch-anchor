package bookstore.web;

import bookstore.model.Customer;
import bookstore.service.CustomerService;

public class CustomerController {

    private final CustomerService customers;

    public CustomerController(CustomerService customers) {
        this.customers = customers;
    }

    /** POST /customers */
    public String register(String email, String name) {
        Customer customer = Customer.joiningToday(email, name);
        if (!customers.register(customer)) {
            return "409 " + email + " is already registered or not an e-mail address";
        }
        return "201 Welcome, " + customer.name();
    }

}
