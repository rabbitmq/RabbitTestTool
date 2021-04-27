package com.rabbitmq.rabbittesttool.model;

public enum ViolationType {
    Ordering,
    RedeliveredOrdering,
    Missing,
    NonRedeliveredDuplicate,
    RedeliveredDuplicate
}
