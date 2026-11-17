<?php

declare(strict_types=1);

namespace Bookstore\Persistence;

use Bookstore\Model\Book;
use PDO;
use PDOException;
use PDOStatement;

final class BookRepository
{
    private const FIND_BY_ISBN = 'SELECT isbn, title, price_in_cents, published_on FROM books WHERE isbn = ?';

    private const INSERT = 'INSERT INTO books (isbn, title, price_in_cents, published_on) VALUES (?, ?, ?, ?)';

    public function __construct(private readonly string $dsn)
    {
    }

    public function findByIsbn(string $isbn): ?Book
    {
        try {
            $row = $this->executed(self::FIND_BY_ISBN, [$isbn])->fetch(PDO::FETCH_ASSOC);

            return $row === false ? null : $this->toBook($row);
        } catch (PDOException) {
            return null;
        }
    }

    public function save(Book $book): bool
    {
        try {
            $insert = $this->executed(self::INSERT, [
                $book->isbn(),
                $book->title(),
                $book->priceInCents(),
                $book->publishedOn()->format('Y-m-d'),
            ]);

            return $insert->rowCount() === 1;
        } catch (PDOException) {
            return false;
        }
    }

    private function executed(string $sql, array $parameters): PDOStatement
    {
        $statement = (new PDO($this->dsn))->prepare($sql);
        $statement->execute($parameters);

        return $statement;
    }

    private function toBook(array $row): Book
    {
        return Book::withPublicationDate(
            $row['isbn'],
            $row['title'],
            (int) $row['price_in_cents'],
            $row['published_on'],
        );
    }
}
