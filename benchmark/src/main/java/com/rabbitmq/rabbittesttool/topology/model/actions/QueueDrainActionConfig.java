package com.rabbitmq.rabbittesttool.topology.model.actions;

public class QueueDrainActionConfig extends ActionConfig {
    int thresholdSeconds;

    public QueueDrainActionConfig(ActionDelay actionDelay, int thresholdSeconds) {
        super(ActionType.QueueDrain, actionDelay);
        this.thresholdSeconds = thresholdSeconds;
    }

    public int getThresholdSeconds() {
        return thresholdSeconds;
    }
}
