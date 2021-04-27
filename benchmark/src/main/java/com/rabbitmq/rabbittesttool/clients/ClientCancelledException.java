package com.rabbitmq.rabbittesttool.clients;

public class ClientCancelledException extends RuntimeException {
    public ClientCancelledException(String s) {
        super(s);
    }
}
