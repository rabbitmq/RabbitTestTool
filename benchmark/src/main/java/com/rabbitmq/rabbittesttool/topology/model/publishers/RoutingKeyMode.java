package com.rabbitmq.rabbittesttool.topology.model.publishers;

public enum RoutingKeyMode {
    None,
    FixedValue,
    MultiValue,
    Random,
    SequenceKey,
    RoutingKeyIndex
}
