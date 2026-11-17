<?php

declare(strict_types=1);

namespace Bookstore\Web;

use Bookstore\Model\Book;
use PDO;
use PDOStatement;

final class SalesReport
{
    private const BESTSELLER = 'SELECT b.isbn, b.title, b.price_in_cents, b.published_on '
        . 'FROM books b JOIN orders o ON o.isbn = b.isbn '
        . 'GROUP BY b.isbn, b.title, b.price_in_cents, b.published_on '
        . 'ORDER BY SUM(o.quantity) DESC LIMIT 1';

    public function __construct(private readonly string $dsn)
    {
    }

    /** GET /reports/bestseller */
    public function bestseller(): ?Book
    {
        $row = $this->bestsellerQuery()->fetch(PDO::FETCH_ASSOC);
        if ($row === false) {
            return null;
        }

        return Book::withPublicationDate(
            $row['isbn'],
            $row['title'],
            (int) $row['price_in_cents'],
            $row['published_on'],
        );
    }

    private function bestsellerQuery(): PDOStatement
    {
        return (new PDO($this->dsn))->query(self::BESTSELLER);
    }
}
