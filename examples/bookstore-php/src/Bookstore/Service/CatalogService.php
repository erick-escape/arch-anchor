<?php

declare(strict_types=1);

namespace Bookstore\Service;

use Bookstore\Model\Book;
use Bookstore\Persistence\BookRepository;

final class CatalogService
{
    public function __construct(private readonly BookRepository $books)
    {
    }

    public function findBook(string $isbn): ?Book
    {
        return $this->books->findByIsbn($isbn);
    }

    public function addBook(Book $book): bool
    {
        if ($book->priceInCents() <= 0) {
            return false;
        }
        if ($this->books->findByIsbn($book->isbn()) !== null) {
            return false;
        }

        return $this->books->save($book);
    }
}
