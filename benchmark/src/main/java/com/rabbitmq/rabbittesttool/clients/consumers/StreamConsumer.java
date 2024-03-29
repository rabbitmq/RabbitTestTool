package com.rabbitmq.rabbittesttool.clients.consumers;

import com.rabbitmq.rabbittesttool.BenchmarkLogger;
import com.rabbitmq.rabbittesttool.clients.ClientUtils;
import com.rabbitmq.rabbittesttool.clients.ConnectToNode;
import com.rabbitmq.rabbittesttool.clients.ConnectionSettings;
import com.rabbitmq.rabbittesttool.model.MessageModel;
import com.rabbitmq.rabbittesttool.statistics.MetricGroup;
import com.rabbitmq.rabbittesttool.statistics.MetricType;
import com.rabbitmq.rabbittesttool.topology.Broker;
import com.rabbitmq.rabbittesttool.topology.QueueHosts;
import com.rabbitmq.rabbittesttool.topology.TopologyException;
import com.rabbitmq.stream.ChunkChecksum;
import com.rabbitmq.stream.OffsetSpecification;
import com.rabbitmq.stream.codec.SimpleCodec;
import com.rabbitmq.stream.impl.Client;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class StreamConsumer implements Runnable  {
    BenchmarkLogger logger;
    String consumerId;
    ConnectionSettings connectionSettings;
    QueueHosts queueHosts;
    AtomicBoolean isCancelled;
    Integer step;
    MetricGroup metricGroup;
    StreamConsumerListener consumerListener;
    MessageModel messageModel;
    ConsumerSettings consumerSettings;
    Broker currentHost;
    AtomicLong lastOffset;
    AtomicLong chunkTimestamp;

    public StreamConsumer(String consumerId,
                          ConnectionSettings connectionSettings,
                          QueueHosts queueHosts,
                          ConsumerSettings consumerSettings,
                          MetricGroup metricGroup,
                          MessageModel messageModel) {
        this.logger = new BenchmarkLogger("STREAM CONSUMER");
        this.isCancelled = new AtomicBoolean();
        this.consumerId = consumerId;
        this.connectionSettings = connectionSettings;
        this.queueHosts = queueHosts;
        this.messageModel = messageModel;
        this.consumerSettings = consumerSettings;
        this.metricGroup = metricGroup;
        this.step = 0;
        this.lastOffset = new AtomicLong();
        this.chunkTimestamp = new AtomicLong();
    }

    public void signalStop() {
        isCancelled.set(true);
    }

    public void setAckInterval(int ackInterval) {
        this.consumerSettings.getAckMode().setAckInterval(ackInterval);
        if(this.consumerListener != null)
            this.consumerListener.setAckInterval(ackInterval);
    }

    public void setAckIntervalMs(int ackIntervalMs) {
        this.consumerSettings.getAckMode().setAckIntervalMs(ackIntervalMs);
        if(this.consumerListener != null)
            this.consumerListener.setAckIntervalMs(ackIntervalMs);
    }

    public void setPrefetch(short prefetch) {
        this.consumerSettings.getAckMode().setConsumerPrefetch(prefetch);
        // we do not update the consumer as a new channel is required
    }

    public void setProcessingMs(int processingMs) {
        this.consumerSettings.setProcessingMs(processingMs);
        if(this.consumerListener != null)
            this.consumerListener.setProcessingMs(processingMs);
    }

    public void triggerNewChannel() {
        step++;
    }

    public long getRecordedReceiveCount() {
        return metricGroup.getRecordedDeltaScalarValueForStepStats(MetricType.ConsumerReceivedMessage);
    }

    public long getRealReceiveCount() {
        return metricGroup.getRealDeltaScalarValueForStepStats(MetricType.ConsumerReceivedMessage);
    }

    public MetricGroup getMetricGroup() {
        return metricGroup;
    }

    @Override
    public void run() {
        while(!isCancelled.get()) {
            try {
                Client client = null;
                try {
                    consumerListener = new StreamConsumerListener(consumerId,
                            connectionSettings.getVhost(),
                            consumerSettings.getQueue(),
                            metricGroup,
                            messageModel,
                            lastOffset,
                            chunkTimestamp,
                            consumerSettings.getAckMode().getConsumerPrefetch(),
                            consumerSettings.getAckMode().getAckInterval(),
                            consumerSettings.getAckMode().getAckIntervalMs(),
                            consumerSettings.getProcessingMs());

                    client = getClient(consumerListener);
                    messageModel.clientConnected(consumerId);

                    ConsumerExitReason exitReason = ConsumerExitReason.None;
                    while (!isCancelled.get() && exitReason != ConsumerExitReason.ConnectionFailed) {
                        int currentStep = step;
                        exitReason = startConsumption(client, currentStep);
                    }
                } finally {
                    tryClose(client);
                }
            } catch (TimeoutException | IOException e) {
                if(!isCancelled.get())
                    metricGroup.increment(MetricType.ConsumerConnectionErrors);
                logger.error("Consumer " + consumerId + " connection failed in step " + step);
                messageModel.clientDisconnected(consumerId, isCancelled.get());
            } catch (Exception e) {
                if(!isCancelled.get())
                    metricGroup.increment(MetricType.ConsumerConnectionErrors);
                logger.error("Consumer " + consumerId + " has failed unexpectedly in step " + step, e);
                messageModel.clientDisconnected(consumerId, isCancelled.get());
            }

            if(!isCancelled.get()) {
                recreateWait();
            }
        }
    }

    private void tryClose(Client client) {
        try {
            messageModel.clientDisconnected(consumerId, isCancelled.get());
            client.close();
        }
        catch(Exception e){}
    }

    private void recreateWait() {
        logger.info("Consumer " + consumerId + " will restart in 5 seconds");
        ClientUtils.waitFor(5000, isCancelled);
    }

    private ConsumerExitReason startConsumption(Client client, Integer currentStep) throws IOException, TimeoutException {
        ConsumerExitReason exitReason = ConsumerExitReason.None;
        logger.info("Consumer " + consumerId + " subscribed");
        try {
            consumerListener = new StreamConsumerListener(consumerId,
                    connectionSettings.getVhost(),
                    consumerSettings.getQueue(),
                    metricGroup,
                    messageModel,
                    this.lastOffset,
                    this.chunkTimestamp,
                    consumerSettings.getAckMode().getConsumerPrefetch(),
                    consumerSettings.getAckMode().getAckInterval(),
                    consumerSettings.getAckMode().getAckIntervalMs(),
                    consumerSettings.getProcessingMs());

            while (!isCancelled.get() && currentStep.equals(step)) {
                ClientUtils.waitFor(1000, this.isCancelled);

                if(!client.isOpen()) {
                    ClientUtils.waitFor(5000, this.isCancelled);
                    if(!client.isOpen()) {
                        logger.error("Client not connected, recreating...");
                        exitReason = ConsumerExitReason.ConnectionFailed;
                        break;
                    }
                }

//                if(consumerSettings.getAckMode().isManualAcks())
//                    consumerListener.ensureAckTimeLimitEnforced();
            }

            if(exitReason == ConsumerExitReason.None) {
                if (isCancelled.get())
                    exitReason = ConsumerExitReason.Cancelled;
                else if (!currentStep.equals(step))
                    exitReason = ConsumerExitReason.NextStep;
                else
                    exitReason = ConsumerExitReason.ConnectionFailed;
            }
        }
        catch(Exception e) {
            logger.error("Failed setting up a consumer", e);
            throw e;
        }
        finally {
            if(client.isOpen()) {
                try {
                    client.close();
                    logger.info("Consumer " + consumerId + " closed channel to " + currentHost.getNodeName() + " with exit reason " + exitReason);
                }
                catch(Exception e) {
                    logger.error("Consumer " + consumerId + " could not close channel to " + currentHost.getNodeName() + " with exit reason " + exitReason, e);
                }
            }
            else {
                exitReason = ConsumerExitReason.ConnectionFailed;
            }

            ClientUtils.waitFor(1000, isCancelled);

            return exitReason;
        }
    }

    private Client getClient(StreamConsumerListener listener) {
        currentHost = getBrokerToConnectTo();
        Client consumer = new Client(new Client.ClientParameters()
                .host(currentHost.getIp())
                .port(currentHost.getStreamPort())
                .username(connectionSettings.getUser())
                .password(connectionSettings.getPassword())
                .virtualHost(connectionSettings.getVhost())
                .codec(new SimpleCodec())
                .chunkChecksum(ChunkChecksum.NO_OP)
                .requestedHeartbeat(Duration.ofSeconds(3600))
                .chunkListener(listener)
                .messageListener(listener));

        OffsetSpecification offsetSpecification = OffsetSpecification.offset(this.lastOffset.get());

        Client.Response response = consumer.subscribe(
                (byte)1,
                consumerSettings.getQueue(),
                offsetSpecification,
                Math.max(10, consumerSettings.getAckMode().getConsumerPrefetch())
        );

        if (!response.isOk()) {
            logger.error("Stream consumer failed to subscribe" + response.getResponseCode());
            throw new RuntimeException("Consumer unable to subscribe");
        }
        else {
            logger.info("Consumer " + consumerId + " opened connection to " + currentHost.getNodeName());
        }

        return consumer;
    }

    private Broker getBrokerToConnectTo() {
        while(!isCancelled.get()) {
            Broker host = null;
            if (connectionSettings.getConsumerConnectToNode().equals(ConnectToNode.RoundRobin))
                host = queueHosts.getHostRoundRobin();
            else if (connectionSettings.getConsumerConnectToNode().equals(ConnectToNode.Random))
                host = queueHosts.getRandomHost();
            else if (connectionSettings.getConsumerConnectToNode().equals(ConnectToNode.Local))
                host = queueHosts.getHost(consumerSettings.getQueue());
            else if (connectionSettings.getConsumerConnectToNode().equals(ConnectToNode.NonLocal))
                host = queueHosts.getRandomOtherHost(consumerSettings.getQueue());
            else
                throw new TopologyException("ConnectToNode value not supported: " + connectionSettings.getConsumerConnectToNode());

            if(host != null)
                return host;
            else
                ClientUtils.waitFor(1000, isCancelled);
        }

        throw new TopologyException("Could not identify a broker to connect to");
    }
}
