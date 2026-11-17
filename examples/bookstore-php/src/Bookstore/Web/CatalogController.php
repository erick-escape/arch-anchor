<?php

declare(strict_types=1);

namespace Bookstore\Web;

use Bookstore\Model\Book;
use Bookstore\Service\CatalogService;

final class CatalogController
{
    public function __construct(private readonly CatalogService $catalog)
    {
    }

    /** GET /books/{isbn} */
    public function showBook(string $isbn): string
    {
        $book = $this->catalog->findBook($isbn);
        if ($book === null) {
            return "404 No book with ISBN {$isbn}";
        }

        return '200 ' . $this->describe($book);
    }

    private function describe(Book $book): string
    {
        $badge = $book->isNewRelease() ? ' (new)' : '';

        return "{$book->title()}{$badge}, {$book->priceInCents()} cents";
    }
}
