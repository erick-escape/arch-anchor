<?php

declare(strict_types=1);

namespace Bookstore\Service;

use Bookstore\Model\Order;
use Bookstore\Persistence\OrderRepository;

final class OrderService
{
    private const LARGEST_ORDER = 50;

    public function __construct(private readonly OrderRepository $orders)
    {
    }

    public function findOrder(string $number): ?Order
    {
        return $this->orders->findByNumber($number);
    }

    public function place(Order $order): bool
    {
        if ($order->quantity() < 1 || $order->quantity() > self::LARGEST_ORDER) {
            return false;
        }
        if ($this->orders->findByNumber($order->number()) !== null) {
            return false;
        }

        return $this->orders->save($order);
    }
}
