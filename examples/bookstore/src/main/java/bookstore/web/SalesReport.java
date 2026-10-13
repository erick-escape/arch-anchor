package bookstore.web;

import bookstore.model.Book;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

public class SalesReport {

    private static final String BESTSELLER = "SELECT b.isbn, b.title, b.price_in_cents, b.published_on "
            + "FROM books b JOIN orders o ON o.isbn = b.isbn "
            + "GROUP BY b.isbn, b.title, b.price_in_cents, b.published_on "
            + "ORDER BY SUM(o.quantity) DESC LIMIT 1";

    private final String jdbcUrl;

    public SalesReport(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }

    /** GET /reports/bestseller */
    public Optional<Book> bestseller() throws SQLException {
        try (Connection connection = DriverManager.getConnection(jdbcUrl);
                Statement query = connection.createStatement();
                ResultSet row = query.executeQuery(BESTSELLER)) {
            if (!row.next()) {
                return Optional.empty();
            }
            return Optional.of(new Book(row.getString("isbn"), row.getString("title"),
                    row.getLong("price_in_cents"), row.getDate("published_on").toLocalDate()));
        }
    }

}
