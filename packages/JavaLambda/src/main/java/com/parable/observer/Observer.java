package com.parable.observer;

public interface Observer {
    enum MessageTemplate {
        HELP, MESSAGE_SENT, ERROR
    }
    void update(MessageTemplate message);
    void update(String message);
}
