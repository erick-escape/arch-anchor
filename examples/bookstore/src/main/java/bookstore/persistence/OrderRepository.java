package bookstore.persistence;

import bookstore.model.Book;
import bookstore.model.Customer;
import bookstore.model.Order;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Optional;

public class OrderRepository {

    private static final String FIND_BY_NUMBER = "SELECT o.number, o.quantity, o.placed_at, "
            + "c.email, c.name, c.member_since, b.isbn, b.title, b.price_in_cents, b.published_on "
            + "FROM orders o JOIN customers c ON c.email = o.customer_email JOIN books b ON b.isbn = o.isbn "
            + "WHERE o.number = ?";

    private static final String INSERT = "INSERT INTO orders (number, customer_email, isbn, quantity, placed_at) VALUES (?, ?, ?, ?, ?)";

    private final String jdbcUrl;

    public OrderRepository(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }

    public Optional<Order> findByNumber(String number) {
        try (Connection connection = DriverManager.getConnection(jdbcUrl);
                PreparedStatement query = connection.prepareStatement(FIND_BY_NUMBER)) {
            query.setString(1, number);
            ResultSet row = query.executeQuery();
            return row.next() ? Optional.of(toOrder(row)) : Optional.empty();
        }
        catch (SQLException databaseUnavailable) {
            return Optional.empty();
        }
    }

    public boolean save(Order order) {
        try (Connection connection = DriverManager.getConnection(jdbcUrl);
                PreparedStatement insert = connection.prepareStatement(INSERT)) {
            insert.setString(1, order.number());
            insert.setString(2, order.customer().email());
            insert.setString(3, order.book().isbn());
            insert.setInt(4, order.quantity());
            insert.setTimestamp(5, Timestamp.valueOf(order.placedAt()));
            return insert.executeUpdate() == 1;
        }
        catch (SQLException databaseUnavailable) {
            return false;
        }
    }

    private Order toOrder(ResultSet row) throws SQLException {
        Customer customer = new Customer(row.getString("email"), row.getString("name"),
                row.getDate("member_since").toLocalDate());
        Book book = new Book(row.getString("isbn"), row.getString("title"), row.getLong("price_in_cents"),
                row.getDate("published_on").toLocalDate());
        return new Order(row.getString("number"), customer, book, row.getInt("quantity"),
                row.getTimestamp("placed_at").toLocalDateTime());
    }

}
