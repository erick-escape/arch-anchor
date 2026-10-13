package bookstore.model;

import java.time.LocalDate;

public class Book {

    private final String isbn;

    private final String title;

    private final long priceInCents;

    private final LocalDate publishedOn;

    public Book(String isbn, String title, long priceInCents, LocalDate publishedOn) {
        this.isbn = isbn;
        this.title = title;
        this.priceInCents = priceInCents;
        this.publishedOn = publishedOn;
    }

    public String isbn() {
        return isbn;
    }

    public String title() {
        return title;
    }

    public long priceInCents() {
        return priceInCents;
    }

    public LocalDate publishedOn() {
        return publishedOn;
    }

    public boolean isNewRelease() {
        return publishedOn.isAfter(LocalDate.now().minusMonths(3));
    }

}
