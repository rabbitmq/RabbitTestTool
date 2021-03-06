package com.rabbitmq.rabbittesttool.topology.model;

public enum VariableDimension {
    MessageSize,
    MessageHeaders,
    Publishers,
    Consumers,
    PublisherInFlightLimit,
    PublishRatePerPublisher,
    ConsumerPrefetch,
    ConsumerAckInterval,
    ConsumerAckIntervalMs,
    Queues,
    RoutingKeyIndex,
    ProcessingMs
}
