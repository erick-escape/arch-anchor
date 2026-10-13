package bookstore.web;

import bookstore.model.Book;
import bookstore.service.CatalogService;

import java.util.Optional;

public class CatalogController {

    private final CatalogService catalog;

    public CatalogController(CatalogService catalog) {
        this.catalog = catalog;
    }

    /** GET /books/{isbn} */
    public String showBook(String isbn) {
        Optional<Book> book = catalog.findBook(isbn);
        if (book.isEmpty()) {
            return "404 No book with ISBN " + isbn;
        }
        String badge = book.get().isNewRelease() ? " (new)" : "";
        return "200 " + book.get().title() + badge + ", " + book.get().priceInCents() + " cents";
    }

}
