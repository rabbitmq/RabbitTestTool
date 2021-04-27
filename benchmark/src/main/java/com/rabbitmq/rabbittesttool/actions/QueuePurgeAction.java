package com.rabbitmq.rabbittesttool.actions;

import com.rabbitmq.rabbittesttool.BenchmarkLogger;
import com.rabbitmq.rabbittesttool.clients.ConnectionSettings;
import com.rabbitmq.rabbittesttool.topology.TopologyGenerator;
import com.rabbitmq.rabbittesttool.topology.model.actions.QueuePurgeActionConfig;

public class QueuePurgeAction {
    BenchmarkLogger logger;
    ConnectionSettings connectionSettings;
    QueuePurgeActionConfig config;
    TopologyGenerator topologyGenerator;

    public QueuePurgeAction(ConnectionSettings connectionSettings,
                            QueuePurgeActionConfig config,
                            TopologyGenerator topologyGenerator) {
        this.logger = new BenchmarkLogger("DRAINACTION");
        this.connectionSettings = connectionSettings;
        this.config = config;
        this.topologyGenerator = topologyGenerator;
    }

    public void run(String queueName) {
        topologyGenerator.purgeQueue(connectionSettings.getVhost(),
                queueName,
                connectionSettings.isDownstream());
        logger.info("Purge complete.");
    }
}
