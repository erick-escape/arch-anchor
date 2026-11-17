<?php

declare(strict_types=1);

namespace Bookstore\Model;

use DateTimeImmutable;

final class Book
{
    public function __construct(
        private readonly string $isbn,
        private readonly string $title,
        private readonly int $priceInCents,
        private readonly DateTimeImmutable $publishedOn,
    ) {
    }

    /** A book whose day of publication is given as text, such as "2024-03-15". */
    public static function withPublicationDate(
        string $isbn,
        string $title,
        int $priceInCents,
        string $publishedOn,
    ): self {
        return new self($isbn, $title, $priceInCents, new DateTimeImmutable($publishedOn));
    }

    public function isbn(): string
    {
        return $this->isbn;
    }

    public function title(): string
    {
        return $this->title;
    }

    public function priceInCents(): int
    {
        return $this->priceInCents;
    }

    public function publishedOn(): DateTimeImmutable
    {
        return $this->publishedOn;
    }

    public function isNewRelease(): bool
    {
        return $this->publishedOn > new DateTimeImmutable('-3 months');
    }
}
