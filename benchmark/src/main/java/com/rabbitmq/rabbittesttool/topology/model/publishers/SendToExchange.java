package com.rabbitmq.rabbittesttool.topology.model.publishers;

import com.rabbitmq.rabbittesttool.topology.model.VirtualHost;

public class SendToExchange {
    private String exchange;
    private RoutingKeyMode routingKeyMode;
    private String routingKey;
    private String[] routingKeys;

    public SendToExchange(String exchange, RoutingKeyMode routingKeyMode) {
        this.exchange = exchange;
        this.routingKeyMode = routingKeyMode;
    }

    public SendToExchange clone(int scaleNumber) {
        SendToExchange s2e = new SendToExchange(this.exchange + VirtualHost.getScaleSuffix(scaleNumber), this.routingKeyMode);
        s2e.setRoutingKeys(routingKeys);
        s2e.setRoutingKey(routingKey);
        return s2e;
    }

    public static SendToExchange withNoRoutingKey(String exchange) {
        return new SendToExchange(exchange, RoutingKeyMode.None);
    }

    public static SendToExchange withRandomRoutingKey(String exchange) {
        return new SendToExchange(exchange, RoutingKeyMode.Random);
    }

    public static SendToExchange withRoutingKeyIndex(String exchange, String[] routingKeys) {
        SendToExchange ste = new SendToExchange(exchange, RoutingKeyMode.RoutingKeyIndex);
        ste.setRoutingKeys(routingKeys);
        return ste;
    }

    public static SendToExchange withStreamRoutingKey(String exchange) {
        return new SendToExchange(exchange, RoutingKeyMode.SequenceKey);
    }

    public static SendToExchange withRoutingKey(String exchange, String routingKey) {
        SendToExchange ste = new SendToExchange(exchange, RoutingKeyMode.FixedValue);
        ste.setRoutingKey(routingKey);
        return ste;
    }

    public static SendToExchange withRoutingKeys(String exchange, String[] routingKeys) {
        SendToExchange ste = new SendToExchange(exchange, RoutingKeyMode.MultiValue);
        ste.setRoutingKeys(routingKeys);
        return ste;
    }

    public String getExchange() {
        return exchange;
    }

    public void setExchange(String exchange) {
        this.exchange = exchange;
    }

    public RoutingKeyMode getRoutingKeyMode() {
        return routingKeyMode;
    }

    public void setRoutingKeyMode(RoutingKeyMode routingKeyMode) {
        this.routingKeyMode = routingKeyMode;
    }

    public String getRoutingKey() {
        return routingKey;
    }

    public void setRoutingKey(String routingKey) {
        this.routingKey = routingKey;
    }

    public String[] getRoutingKeys() {
        return routingKeys;
    }

    public void setRoutingKeys(String[] routingKeys) {
        this.routingKeys = routingKeys;
    }
}
