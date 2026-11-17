<?php

declare(strict_types=1);

namespace Bookstore\Model;

use DateTimeImmutable;

final class Customer
{
    public function __construct(
        private readonly string $email,
        private readonly string $name,
        private readonly DateTimeImmutable $memberSince,
    ) {
    }

    public static function joiningToday(string $email, string $name): self
    {
        return new self($email, $name, new DateTimeImmutable('today'));
    }

    /** A customer whose first day as a member is given as text, such as "2024-03-15". */
    public static function withMembershipDate(string $email, string $name, string $memberSince): self
    {
        return new self($email, $name, new DateTimeImmutable($memberSince));
    }

    public function email(): string
    {
        return $this->email;
    }

    public function name(): string
    {
        return $this->name;
    }

    public function memberSince(): DateTimeImmutable
    {
        return $this->memberSince;
    }

    public function hasBeenMemberForAYear(): bool
    {
        return $this->memberSince < new DateTimeImmutable('-1 year');
    }
}
