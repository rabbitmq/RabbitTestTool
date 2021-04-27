package com.rabbitmq.rabbittesttool.clients.consumers;

import com.rabbitmq.rabbittesttool.statistics.RecordedCounter;

public class ConsumerStats {
    private RecordedCounter recordedReceived;

    public ConsumerStats() {
        recordedReceived = new RecordedCounter();
    }

    public void incrementReceivedCount() {
        recordedReceived.increment();
    }

    public long getAndResetRecordedReceived() {
        return recordedReceived.getRecordedValueAndReset();
    }

    public long getAndResetRealReceived() {
        return recordedReceived.getRealValueAndReset();
    }
}
