<?php

declare(strict_types=1);

namespace Bookstore\Service;

use Bookstore\Model\Customer;
use Bookstore\Persistence\CustomerRepository;

final class CustomerService
{
    public function __construct(private readonly CustomerRepository $customers)
    {
    }

    public function findCustomer(string $email): ?Customer
    {
        return $this->customers->findByEmail($email);
    }

    public function register(Customer $customer): bool
    {
        if (!str_contains($customer->email(), '@')) {
            return false;
        }
        if ($this->customers->findByEmail($customer->email()) !== null) {
            return false;
        }

        return $this->customers->save($customer);
    }
}
