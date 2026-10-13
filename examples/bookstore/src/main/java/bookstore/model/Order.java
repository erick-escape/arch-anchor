package bookstore.model;

import java.time.LocalDateTime;

public class Order {

    private final String number;

    private final Customer customer;

    private final Book book;

    private final int quantity;

    private final LocalDateTime placedAt;

    public Order(String number, Customer customer, Book book, int quantity, LocalDateTime placedAt) {
        this.number = number;
        this.customer = customer;
        this.book = book;
        this.quantity = quantity;
        this.placedAt = placedAt;
    }

    public static Order placedNow(String number, Customer customer, Book book, int quantity) {
        return new Order(number, customer, book, quantity, LocalDateTime.now());
    }

    public String number() {
        return number;
    }

    public Customer customer() {
        return customer;
    }

    public Book book() {
        return book;
    }

    public int quantity() {
        return quantity;
    }

    public LocalDateTime placedAt() {
        return placedAt;
    }

    public long totalInCents() {
        return book.priceInCents() * quantity;
    }

}
