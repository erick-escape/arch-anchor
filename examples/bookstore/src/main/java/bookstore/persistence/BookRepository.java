package bookstore.persistence;

import bookstore.model.Book;

import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class BookRepository {

    private static final String FIND_BY_ISBN = "SELECT isbn, title, price_in_cents, published_on FROM books WHERE isbn = ?";

    private static final String INSERT = "INSERT INTO books (isbn, title, price_in_cents, published_on) VALUES (?, ?, ?, ?)";

    private final String jdbcUrl;

    public BookRepository(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }

    public Optional<Book> findByIsbn(String isbn) {
        try (Connection connection = DriverManager.getConnection(jdbcUrl);
                PreparedStatement query = connection.prepareStatement(FIND_BY_ISBN)) {
            query.setString(1, isbn);
            ResultSet row = query.executeQuery();
            return row.next() ? Optional.of(toBook(row)) : Optional.empty();
        }
        catch (SQLException databaseUnavailable) {
            return Optional.empty();
        }
    }

    public boolean save(Book book) {
        try (Connection connection = DriverManager.getConnection(jdbcUrl);
                PreparedStatement insert = connection.prepareStatement(INSERT)) {
            insert.setString(1, book.isbn());
            insert.setString(2, book.title());
            insert.setLong(3, book.priceInCents());
            insert.setDate(4, Date.valueOf(book.publishedOn()));
            return insert.executeUpdate() == 1;
        }
        catch (SQLException databaseUnavailable) {
            return false;
        }
    }

    private Book toBook(ResultSet row) throws SQLException {
        return new Book(row.getString("isbn"), row.getString("title"), row.getLong("price_in_cents"),
                row.getDate("published_on").toLocalDate());
    }

}
