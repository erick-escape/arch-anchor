package bookstore.model;

import java.time.LocalDate;

public class Customer {

    private final String email;

    private final String name;

    private final LocalDate memberSince;

    public Customer(String email, String name, LocalDate memberSince) {
        this.email = email;
        this.name = name;
        this.memberSince = memberSince;
    }

    public static Customer joiningToday(String email, String name) {
        return new Customer(email, name, LocalDate.now());
    }

    public String email() {
        return email;
    }

    public String name() {
        return name;
    }

    public LocalDate memberSince() {
        return memberSince;
    }

    public boolean hasBeenMemberForAYear() {
        return memberSince.isBefore(LocalDate.now().minusYears(1));
    }

}
