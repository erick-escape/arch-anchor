<?php

declare(strict_types=1);

namespace Bookstore\Persistence;

use Bookstore\Model\Customer;
use PDO;
use PDOException;
use PDOStatement;

final class CustomerRepository
{
    private const FIND_BY_EMAIL = 'SELECT email, name, member_since FROM customers WHERE email = ?';

    private const INSERT = 'INSERT INTO customers (email, name, member_since) VALUES (?, ?, ?)';

    public function __construct(private readonly string $dsn)
    {
    }

    public function findByEmail(string $email): ?Customer
    {
        try {
            $row = $this->executed(self::FIND_BY_EMAIL, [$email])->fetch(PDO::FETCH_ASSOC);

            return $row === false ? null : $this->toCustomer($row);
        } catch (PDOException) {
            return null;
        }
    }

    public function save(Customer $customer): bool
    {
        try {
            $insert = $this->executed(self::INSERT, [
                $customer->email(),
                $customer->name(),
                $customer->memberSince()->format('Y-m-d'),
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

    private function toCustomer(array $row): Customer
    {
        return Customer::withMembershipDate($row['email'], $row['name'], $row['member_since']);
    }
}
