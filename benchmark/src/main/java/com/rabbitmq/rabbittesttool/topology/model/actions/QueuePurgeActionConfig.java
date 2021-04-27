package com.rabbitmq.rabbittesttool.topology.model.actions;

public class QueuePurgeActionConfig extends ActionConfig {
    public QueuePurgeActionConfig(ActionDelay actionDelay) {
        super(ActionType.QueuePurge, actionDelay);
    }
}
