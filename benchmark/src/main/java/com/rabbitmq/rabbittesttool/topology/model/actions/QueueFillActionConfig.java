package com.rabbitmq.rabbittesttool.topology.model.actions;

public class QueueFillActionConfig extends ActionConfig {
    int messageSize;
    int messageCount;
    int publishRate;

    public QueueFillActionConfig(ActionDelay actionDelay,
                                 int messageSize,
                                 int messageCount,
                                 int publishRate) {
        super(ActionType.QueueFill, actionDelay);
        this.messageSize = messageSize;
        this.messageCount = messageCount;
        this.publishRate = publishRate;
    }

    public int getMessageSize() {
        return messageSize;
    }

    public int getMessageCount() {
        return messageCount;
    }

    public int getPublishRate() {
        return publishRate;
    }
}
