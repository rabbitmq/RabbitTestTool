package com.rabbitmq.rabbittesttool.clients.publishers;

import com.rabbitmq.rabbittesttool.clients.MessagePayload;

import java.util.concurrent.ConcurrentMap;

public class PublishTracker {
    ConcurrentMap<Long, MessagePayload> payloads;


}
