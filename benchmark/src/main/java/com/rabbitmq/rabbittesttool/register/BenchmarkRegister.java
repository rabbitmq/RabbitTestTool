package com.rabbitmq.rabbittesttool.register;

import com.rabbitmq.rabbittesttool.InstanceConfiguration;
import com.rabbitmq.rabbittesttool.model.ConsumeInterval;
import com.rabbitmq.rabbittesttool.model.DisconnectedInterval;
import com.rabbitmq.rabbittesttool.model.Summary;
import com.rabbitmq.rabbittesttool.model.Violation;
import com.rabbitmq.rabbittesttool.topology.model.Topology;

import java.util.List;

public interface BenchmarkRegister {
    void logBenchmarkStart(String benchmarkId,
                           int runOrdinal,
                           String technology,
                           String version,
                           InstanceConfiguration instanceConfig,
                           Topology topology,
                           String arguments,
                           String benchmarkTag);

    void logBenchmarkEnd(String benchmarkId);
    void logException(String benchmarkId, Exception e);
    void logStepStart(String benchmarkId, int step, int durationSeconds, String stepValue);
    void logLiveStatistics(String benchmarkId, int step, StepStatistics stepStatistics);
    void logStepEnd(String benchmarkId, int step, StepStatistics stepStatistics);
    List<StepStatistics> getStepStatistics(String runId,
                                           String technology,
                                           String version,
                                           String configTag);
    InstanceConfiguration getInstanceConfiguration(String runId,
                                                   String technology,
                                                   String version,
                                                   String configTag);
    List<BenchmarkMetaData> getBenchmarkMetaData(String runId,
                                                 String technology,
                                                 String version,
                                                 String configTag);

    void logModelSummary(Summary sumary);
    void logViolations(String benchmarkId, List<Violation> violations);
    void logConsumeIntervals(String benchmarkId,
                             List<ConsumeInterval> consumeIntervals,
                             int unavailabilityThresholdSeconds,
                             double availability);

    void logDisconnectedIntervals(String benchmarkId,
                             List<DisconnectedInterval> disconnectedIntervals,
                             int unavailabilityThresholdSeconds,
                             double availability);
}
