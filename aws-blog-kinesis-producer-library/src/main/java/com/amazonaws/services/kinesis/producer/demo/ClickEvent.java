package com.amazonaws.services.kinesis.producer.demo;

public class ClickEvent {
    private String sessionId;
    private String payload;

    public ClickEvent(String sessionId, String payload) {
        this.sessionId = sessionId;
        this.payload = payload;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getPayload() {
        return payload;
    }
}