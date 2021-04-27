package com.rabbitmq.rabbittesttool.actions;

import com.rabbitmq.rabbittesttool.BenchmarkLogger;
import com.rabbitmq.rabbittesttool.clients.ConnectionSettings;
import com.rabbitmq.rabbittesttool.clients.consumers.DrainConsumer;
import com.rabbitmq.rabbittesttool.topology.QueueHosts;
import com.rabbitmq.rabbittesttool.topology.model.actions.QueueDrainActionConfig;

import java.util.concurrent.atomic.AtomicBoolean;

public class QueueDrainAction {
    BenchmarkLogger logger;
    ConnectionSettings connectionSettings;
    QueueDrainActionConfig config;
    QueueHosts queueHosts;
    AtomicBoolean isCancelled;

    public QueueDrainAction(ConnectionSettings connectionSettings,
                            QueueDrainActionConfig config,
                            QueueHosts queueHosts,
                            AtomicBoolean isCancelled) {
        this.logger = new BenchmarkLogger("DRAINACTION");
        this.connectionSettings = connectionSettings;
        this.config = config;
        this.queueHosts = queueHosts;
        this.isCancelled = isCancelled;
    }

    public void run(String queueName) {
        DrainConsumer consumer = new DrainConsumer(queueName + "-drain",
                connectionSettings,
                queueHosts,
                isCancelled);
        consumer.drain(queueName, config.getThresholdSeconds());
        logger.info("Drain complete.");
    }
}
