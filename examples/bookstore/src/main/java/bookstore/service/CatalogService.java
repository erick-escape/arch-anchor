package bookstore.service;

import bookstore.model.Book;
import bookstore.persistence.BookRepository;

import java.util.Optional;

public class CatalogService {

    private final BookRepository books;

    public CatalogService(BookRepository books) {
        this.books = books;
    }

    public Optional<Book> findBook(String isbn) {
        return books.findByIsbn(isbn);
    }

    public boolean addBook(Book book) {
        if (book.priceInCents() <= 0) {
            return false;
        }
        if (books.findByIsbn(book.isbn()).isPresent()) {
            return false;
        }
        return books.save(book);
    }

}
