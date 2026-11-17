<?php

declare(strict_types=1);

namespace Bookstore\Persistence;

use Bookstore\Model\Book;
use Bookstore\Model\Customer;
use Bookstore\Model\Order;
use PDO;
use PDOException;
use PDOStatement;

final class OrderRepository
{
    private const FIND_BY_NUMBER = 'SELECT o.number, o.quantity, o.placed_at, '
        . 'c.email, c.name, c.member_since, b.isbn, b.title, b.price_in_cents, b.published_on '
        . 'FROM orders o JOIN customers c ON c.email = o.customer_email JOIN books b ON b.isbn = o.isbn '
        . 'WHERE o.number = ?';

    private const INSERT = 'INSERT INTO orders (number, customer_email, isbn, quantity, placed_at) '
        . 'VALUES (?, ?, ?, ?, ?)';

    public function __construct(private readonly string $dsn)
    {
    }

    public function findByNumber(string $number): ?Order
    {
        try {
            $row = $this->executed(self::FIND_BY_NUMBER, [$number])->fetch(PDO::FETCH_ASSOC);

            return $row === false ? null : $this->toOrder($row);
        } catch (PDOException) {
            return null;
        }
    }

    public function save(Order $order): bool
    {
        try {
            $insert = $this->executed(self::INSERT, [
                $order->number(),
                $order->customer()->email(),
                $order->book()->isbn(),
                $order->quantity(),
                $order->placedAt()->format('Y-m-d H:i:s'),
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

    private function toOrder(array $row): Order
    {
        $customer = Customer::withMembershipDate($row['email'], $row['name'], $row['member_since']);
        $book = Book::withPublicationDate(
            $row['isbn'],
            $row['title'],
            (int) $row['price_in_cents'],
            $row['published_on'],
        );

        return Order::withPlacementTime($row['number'], $customer, $book, (int) $row['quantity'], $row['placed_at']);
    }
}
