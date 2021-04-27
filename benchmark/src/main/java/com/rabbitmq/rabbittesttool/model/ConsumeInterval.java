package com.rabbitmq.rabbittesttool.model;

public class ConsumeInterval {
    private ReceivedMessage startMessage;
    private ReceivedMessage endMessage;

    public ConsumeInterval(ReceivedMessage startMessage, ReceivedMessage endMessage) {
        this.startMessage = startMessage;
        this.endMessage = endMessage;
    }

    public ReceivedMessage getStartMessage() {
        return startMessage;
    }

    public void setStartMessage(ReceivedMessage startMessage) {
        this.startMessage = startMessage;
    }

    public ReceivedMessage getEndMessage() {
        return endMessage;
    }

    public void setEndMessage(ReceivedMessage endMessage) {
        this.endMessage = endMessage;
    }
}
