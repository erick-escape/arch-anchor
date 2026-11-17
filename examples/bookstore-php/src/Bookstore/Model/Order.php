<?php

declare(strict_types=1);

namespace Bookstore\Model;

use DateTimeImmutable;

final class Order
{
    public function __construct(
        private readonly string $number,
        private readonly Customer $customer,
        private readonly Book $book,
        private readonly int $quantity,
        private readonly DateTimeImmutable $placedAt,
    ) {
    }

    public static function placedNow(string $number, Customer $customer, Book $book, int $quantity): self
    {
        return new self($number, $customer, $book, $quantity, new DateTimeImmutable());
    }

    /** An order whose moment of placement is given as text, such as "2024-03-15 10:30:00". */
    public static function withPlacementTime(
        string $number,
        Customer $customer,
        Book $book,
        int $quantity,
        string $placedAt,
    ): self {
        return new self($number, $customer, $book, $quantity, new DateTimeImmutable($placedAt));
    }

    public function number(): string
    {
        return $this->number;
    }

    public function customer(): Customer
    {
        return $this->customer;
    }

    public function book(): Book
    {
        return $this->book;
    }

    public function quantity(): int
    {
        return $this->quantity;
    }

    public function placedAt(): DateTimeImmutable
    {
        return $this->placedAt;
    }

    public function totalInCents(): int
    {
        return $this->book->priceInCents() * $this->quantity;
    }
}
