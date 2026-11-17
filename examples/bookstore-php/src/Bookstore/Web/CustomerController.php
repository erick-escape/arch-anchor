<?php

declare(strict_types=1);

namespace Bookstore\Web;

use Bookstore\Model\Customer;
use Bookstore\Service\CustomerService;

final class CustomerController
{
    public function __construct(private readonly CustomerService $customers)
    {
    }

    /** POST /customers */
    public function register(string $email, string $name): string
    {
        $customer = Customer::joiningToday($email, $name);
        if (!$this->customers->register($customer)) {
            return "409 {$email} is already registered or not an e-mail address";
        }

        return "201 Welcome, {$customer->name()}";
    }
}
