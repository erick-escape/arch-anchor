package bookstore.service;

import bookstore.model.Order;
import bookstore.persistence.OrderRepository;

import java.util.Optional;

public class OrderService {

    private static final int LARGEST_ORDER = 50;

    private final OrderRepository orders;

    public OrderService(OrderRepository orders) {
        this.orders = orders;
    }

    public Optional<Order> findOrder(String number) {
        return orders.findByNumber(number);
    }

    public boolean place(Order order) {
        if (order.quantity() < 1 || order.quantity() > LARGEST_ORDER) {
            return false;
        }
        if (orders.findByNumber(order.number()).isPresent()) {
            return false;
        }
        return orders.save(order);
    }

}
