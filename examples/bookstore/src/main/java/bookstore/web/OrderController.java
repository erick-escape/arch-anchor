package bookstore.web;

import bookstore.model.Book;
import bookstore.model.Customer;
import bookstore.model.Order;
import bookstore.service.CatalogService;
import bookstore.service.CustomerService;
import bookstore.service.OrderService;

import java.util.Optional;

public class OrderController {

    private final OrderService orders;

    private final CatalogService catalog;

    private final CustomerService customers;

    public OrderController(OrderService orders, CatalogService catalog, CustomerService customers) {
        this.orders = orders;
        this.catalog = catalog;
        this.customers = customers;
    }

    /** POST /orders */
    public String placeOrder(String number, String customerEmail, String isbn, int quantity) {
        Optional<Customer> customer = customers.findCustomer(customerEmail);
        Optional<Book> book = catalog.findBook(isbn);
        if (customer.isEmpty() || book.isEmpty()) {
            return "404 Unknown customer or book";
        }
        Order order = Order.placedNow(number, customer.get(), book.get(), quantity);
        if (!orders.place(order)) {
            return "422 Order " + number + " was not accepted";
        }
        return "201 Order " + number + " placed, " + order.totalInCents() + " cents";
    }

    /** GET /orders/{number} */
    public String showOrder(String number) {
        Optional<Order> order = orders.findOrder(number);
        if (order.isEmpty()) {
            return "404 No order " + number;
        }
        return "200 " + order.get().quantity() + " x " + order.get().book().title();
    }

}
