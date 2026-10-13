package bookstore.persistence;

import bookstore.model.Customer;

import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class CustomerRepository {

    private static final String FIND_BY_EMAIL = "SELECT email, name, member_since FROM customers WHERE email = ?";

    private static final String INSERT = "INSERT INTO customers (email, name, member_since) VALUES (?, ?, ?)";

    private final String jdbcUrl;

    public CustomerRepository(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }

    public Optional<Customer> findByEmail(String email) {
        try (Connection connection = DriverManager.getConnection(jdbcUrl);
                PreparedStatement query = connection.prepareStatement(FIND_BY_EMAIL)) {
            query.setString(1, email);
            ResultSet row = query.executeQuery();
            return row.next() ? Optional.of(toCustomer(row)) : Optional.empty();
        }
        catch (SQLException databaseUnavailable) {
            return Optional.empty();
        }
    }

    public boolean save(Customer customer) {
        try (Connection connection = DriverManager.getConnection(jdbcUrl);
                PreparedStatement insert = connection.prepareStatement(INSERT)) {
            insert.setString(1, customer.email());
            insert.setString(2, customer.name());
            insert.setDate(3, Date.valueOf(customer.memberSince()));
            return insert.executeUpdate() == 1;
        }
        catch (SQLException databaseUnavailable) {
            return false;
        }
    }

    private Customer toCustomer(ResultSet row) throws SQLException {
        return new Customer(row.getString("email"), row.getString("name"),
                row.getDate("member_since").toLocalDate());
    }

}
