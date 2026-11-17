<?php

declare(strict_types=1);

namespace Bookstore\Web;

use Bookstore\Model\Book;
use Bookstore\Model\Customer;
use Bookstore\Model\Order;
use Bookstore\Service\CatalogService;
use Bookstore\Service\CustomerService;
use Bookstore\Service\OrderService;

final class OrderController
{
    public function __construct(
        private readonly OrderService $orders,
        private readonly CatalogService $catalog,
        private readonly CustomerService $customers,
    ) {
    }

    /** POST /orders */
    public function placeOrder(string $number, string $customerEmail, string $isbn, int $quantity): string
    {
        $customer = $this->customers->findCustomer($customerEmail);
        $book = $this->catalog->findBook($isbn);
        if (!$customer instanceof Customer || !$book instanceof Book) {
            return '404 Unknown customer or book';
        }
        $order = Order::placedNow($number, $customer, $book, $quantity);
        if (!$this->orders->place($order)) {
            return "422 Order {$number} was not accepted";
        }

        return "201 Order {$number} placed, {$order->totalInCents()} cents";
    }

    /** GET /orders/{number} */
    public function showOrder(string $number): string
    {
        $order = $this->orders->findOrder($number);
        if ($order === null) {
            return "404 No order {$number}";
        }

        return "200 {$order->quantity()} x {$order->book()->title()}";
    }
}
